package com.shortchain.util;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器工具类（基于 Redisson 分布式布隆过滤器）
 * <p>
 * 用途：拦截「不存在的短码」请求，防止恶意/无效请求穿透 Redis 直接打到 MySQL（缓存穿透）。
 * <p>
 * 设计说明：
 * <ul>
 *   <li>元素类型固定为 String（短码），避免泛型带来的序列化不确定性。</li>
 *   <li>注册为 Spring Bean，注入 RedissonClient，布隆 key 名称与参数从配置读取。</li>
 *   <li>{@code tryInit} 只在 Redis 中 key 不存在时生效；一旦建好，后续启动沿用旧参数，
 *       修改 expected-insertions / false-positive-rate 需先删除 Redis 中的 key。</li>
 * </ul>
 */
@Slf4j
@Component
public class RedissonBloomFilterUtil {

    /** 布隆过滤器在 Redis 中的 key 名称 */
    private static final String BLOOM_FILTER_NAME = "bloom:short:code";

    /** 元素固定为短码字符串 */
    private final RBloomFilter<String> bloomFilter;

    public RedissonBloomFilterUtil(
            RedissonClient redissonClient,
            @Value("${shortchain.bloom.expected-insertions:100000000}") long expectedInsertions,
            @Value("${shortchain.bloom.false-positive-rate:0.01}") double falseProbability) {

        this.bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);

        // tryInit：首次（key 不存在）返回 true 并完成初始化；已存在则返回 false，沿用旧参数。
        boolean created = this.bloomFilter.tryInit(expectedInsertions, falseProbability);
        log.info("布隆过滤器初始化完成, name={}, created={}, expectedInsertions={}, falseProbability={}",
                BLOOM_FILTER_NAME, created, expectedInsertions, falseProbability);
    }

    /**
     * 生成短码时：把短码加入布隆过滤器
     *
     * @param shortCode 短码
     * @return 元素成功添加返回 true（注意：可能因误判返回 false，业务上不依赖返回值）
     */
    public boolean add(String shortCode) {
        return bloomFilter.add(shortCode);
    }

    /**
     * 跳转时：判断短码是否「可能存在」
     * <p>
     * 布隆过滤器语义：
     * <ul>
     *   <li>返回 false → 短码<b>一定不存在</b>，可直接拦截（返回 404），无需查 Redis/MySQL。</li>
     *   <li>返回 true → 短码<b>可能存在</b>（存在误判），仍需继续查 Redis/MySQL 确认。</li>
     * </ul>
     *
     * @param shortCode 短码
     * @return 可能存在返回 true，一定不存在返回 false
     */
    public boolean mightContain(String shortCode) {
        return bloomFilter.contains(shortCode);
    }

    /**
     * 获取已插入元素数量（近似值）
     */
    public long count() {
        return bloomFilter.count();
    }

    /**
     * 获取位数组长度（bit 数 m），用于观测布隆过滤器占用情况
     * <p>
     * 注意：这是位数组的 bit 长度，不是「能容纳的元素个数」，后者由初始化参数 expectedInsertions 决定。
     */
    public long bitSetSize() {
        return bloomFilter.getSize();
    }

    /**
     * 获取当前配置的误判率
     */
    public double falseProbability() {
        return bloomFilter.getFalseProbability();
    }
}
