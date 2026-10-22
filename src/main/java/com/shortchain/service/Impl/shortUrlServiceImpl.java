package com.shortchain.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SmUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.shortchain.common.ResultCode;
import com.shortchain.common.exception.BusinessException;
import com.shortchain.entity.ShortUrl;
import com.shortchain.mapper.ShortUrlMapper;
import com.shortchain.service.ShortUrlService;
import com.shortchain.util.Base62Util;
import com.shortchain.util.RedissonBloomFilterUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import static com.shortchain.common.Constant.*;

/**
 * 短链服务实现类
 * 核心业务：URL 合法性校验 + 短码生成 + 跳转
 */
@Service
@Slf4j
public class ShortUrlServiceImpl implements ShortUrlService {
    @Resource
    private RedissonBloomFilterUtil bloomFilter;

    @Resource
    private ShortUrlMapper shortUrlMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 雪花算法生成器（Hutool 提供）
     * 后期优化：替换为自定义 SnowflakeIdGenerator，处理时钟回拨等
     */
    private final Snowflake snowflake = IdUtil.getSnowflake();

    @Override
    public String generateShortCode(String originalUrl) {
        try(Entry entry = SphU.entry("createShortUrl" , EntryType.OUT,1,originalUrl)) {
            // 1. 长度超限单独提示（40003），单独判断才能与"格式非法"区分
            if (originalUrl != null && originalUrl.length() > MAX_URL_LENGTH) {
                log.warn("[URL超长] length={}", originalUrl.length());
                throw new BusinessException(ResultCode.URL_TOO_LONG);
            }
            // 2. 合法性校验（40002）
            if (!isValidUrl(originalUrl)) {
                log.warn("[URL校验失败] originalUrl={}", originalUrl);
                throw new BusinessException(ResultCode.INVALID_URL);
            }
            // 2. 计算 originalUrl 的 SM3 哈希值，作为幂等唯一键
            String sm3Url = SmUtil.sm3(originalUrl);

            // 3. SETNX 抢占 注意：此时 shortCode 还没生成，先用占位符 "1" 抢占
            Boolean isSuccess = stringRedisTemplate.opsForValue()
                    .setIfAbsent(sm3Url, "1", SM3_SETNX_TTL, TimeUnit.SECONDS);

            String shortCode;
            if (!Boolean.TRUE.equals(isSuccess)) {
                // 3a. 抢占失败：说明该长链可能已有短链，从 Redis 查出已存在的 shortCode
                shortCode = stringRedisTemplate.opsForValue().get(sm3Url);
                // 注意：占位符是 "1"，只有拿到有效的短码才直接返回，否则回源 DB
                if (shortCode != null && !shortCode.isEmpty() && !"1".equals(shortCode)) {
                    return shortCode;
                }
                // Redis 里是占位符或已过期：回源 DB 查询已存在的短码
                ShortUrl existEntity = shortUrlMapper.selectBySm3(sm3Url);
                if (existEntity != null) {
                    shortCode = existEntity.getShortCode();
                    stringRedisTemplate.opsForValue().set(sm3Url, shortCode, SM3_CODE_TTL, TimeUnit.SECONDS);
                    return shortCode;
                }
                // 极端情况：抢占失败但 DB 也没有，降级为重新生成
                return doGenerate(originalUrl, sm3Url);
            } else {
                // 3b. 抢占成功：生成新的短码
                return doGenerate(originalUrl, sm3Url);
            }
        }catch (BlockException e){
            throw new BusinessException(ResultCode.RATE_LIMITED);  // 改这一行
        }
    }

    /**
     * 生成短码并落库、写缓存（抢占成功后的公共逻辑）
     */
    private String doGenerate(String originalUrl, String sm3Url) {
        long id = snowflake.nextId();
        String shortCode = Base62Util.encode(id);

        // 1. 先落库：DB 是事实源，唯一索引兜底
        ShortUrl entity = new ShortUrl();
        entity.setId(id);
        entity.setShortCode(shortCode);
        entity.setOriginalUrl(originalUrl);
        entity.setSm3Url(sm3Url);
        entity.setStatus(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        try {
            shortUrlMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发下同一长链已被别的线程落库：回查并返回已有短码（幂等）
            ShortUrl exist = shortUrlMapper.selectBySm3(sm3Url);
            if (exist == null) {
                exist = shortUrlMapper.selectByShortCode(shortCode);
            }
            if (exist != null) {
                // 顺手补一下幂等键，避免后续再次回源 DB
                stringRedisTemplate.opsForValue()
                        .set(sm3Url, exist.getShortCode(), SM3_CODE_TTL, TimeUnit.SECONDS);
                return exist.getShortCode();
            }
            // 理论不可达：冲突却查不到记录，继续抛出
            throw e;
        }

        // 2. 落库成功，再写 Redis（幂等键 + 跳转缓存预热）
        stringRedisTemplate.opsForValue().set(sm3Url, shortCode, SM3_CODE_TTL, TimeUnit.SECONDS);
        String key = SHORT_CODE + shortCode;
        stringRedisTemplate.opsForValue().set(key, entity.getOriginalUrl(), SHORT_CODE_TTL, TimeUnit.SECONDS);

        // 3. 最后加布隆（DB 有记录才加，避免"幽灵"元素）
        bloomFilter.add(shortCode);

        return shortCode;
    }

    @Override
    public String getOriginalUrl(String shortCode) {
        Entry entry = null;
        try {
            entry = SphU.entry("redirect", EntryType.OUT, 1, shortCode);
            // 1. 先判断 shortCode 是否为空（安全性）
            if (shortCode == null || shortCode.isEmpty()) {
                return null;
            }
            if(!bloomFilter.mightContain(shortCode)){
                //如果布隆过滤器中不存在，则必然不存在
                return null;
            }
            // 2. 拼接缓存 key，查 Redis
            String key = SHORT_CODE + shortCode;
            String originalUrl = stringRedisTemplate.opsForValue().get(key);
            if (originalUrl != null && !originalUrl.isEmpty()) {
                // 2a. 命中：重置 TTL（滑动过期），并返回
                stringRedisTemplate.expire(key, SHORT_CODE_TTL, TimeUnit.SECONDS);
                return originalUrl;
            }

            // 3. Redis 未命中，回源 MySQL
            ShortUrl entity = shortUrlMapper.selectByShortCode(shortCode);
            if (entity == null) {
                // 查不到，短码不存在
                return null;
            }

            // 4. 回填缓存，并设置 TTL，防止内存膨胀
            stringRedisTemplate.opsForValue().set(key, entity.getOriginalUrl(), SHORT_CODE_TTL, TimeUnit.SECONDS);
            return entity.getOriginalUrl();
        } catch (BlockException e) {
            // 按 BlockException 的具体子类区分拦截原因
            if (e instanceof DegradeException) {
                throw new BusinessException(ResultCode.SERVICE_DEGRADED);   // 熔断
            } else if (e instanceof ParamFlowException) {
                throw new BusinessException(ResultCode.HOT_PARAM_LIMITED);  // 热点参数限流
            } else {
                throw new BusinessException(ResultCode.RATE_LIMITED);       // 整体限流（FlowException 等）
            }
        } catch (Exception t) {
            // 只上报业务异常（Exception），不放 Error（OOM 等）进来，避免污染熔断统计
            Tracer.trace(t);
            throw t;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }



    /**
     * 校验原始长链是否合法
     * 使用 JDK 内置的 java.net.URL 解析，更贴合真实浏览器行为
     *
     * 选型说明：相比 java.net.URI，URL 对 '^'、'|'、'\\' 等浏览器可正常处理的
     * 非规范字符容忍度更高（URI 会直接抛 URISyntaxException 误杀合法链接，
     * 例如 CSDN 搜索链接 utm_medium 参数中的 '^'）。因此用 URL 更贴近实际，
     * 减少"浏览器能打开、后端却判非法"的误判。
     *
     * @param url 待校验的原始长链
     * @return 合法返回 true，否则 false
     */
    private boolean isValidUrl(String url) {
        // 1. 非空校验
        if (url == null || url.isBlank()) {
            return false;
        }
        // 2. 长度校验
        if (url.length() > MAX_URL_LENGTH) {
            return false;
        }
        try {
            // 3. 用 URL 解析，格式错误会抛 MalformedURLException
            URL parsed = new URL(url);
            // 4. 协议白名单：只允许 http / https
            String protocol = parsed.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return false;
            }
            // 5. 必须有合法的 host
            return parsed.getHost() != null && !parsed.getHost().isBlank();
        } catch (MalformedURLException e) {
            // 解析失败，说明格式不合法
            return false;
        }
    }
}
