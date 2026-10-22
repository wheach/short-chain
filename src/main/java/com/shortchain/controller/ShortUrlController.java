package com.shortchain.controller;

import com.shortchain.common.ResultCode;
import com.shortchain.common.exception.BusinessException;
import com.shortchain.dto.CreateShortUrlRequest;
import com.shortchain.entity.Result;
import com.shortchain.service.ShortUrlService;
import com.shortchain.vo.ShortUrlVo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1")
public class ShortUrlController {
    @Resource
    private ShortUrlService shortUrlService;

    /** 短链域名前缀，从配置读取（如 http://localhost:8080） */
    @Value("${shortchain.domain}")
    private String domain;

    /**
     * 生成短码short—url
     * @param request
     * @return
     */
    @PostMapping("/short-urls")
    public Result generateShortCode(@Valid @RequestBody CreateShortUrlRequest request){
        String originalUrl = request.getOriginalUrl();
        String shortCode = shortUrlService.generateShortCode(originalUrl);
        if(shortCode == null){
            // 防御兜底：Service 校验失败时应抛异常，正常不会返回 null
            throw new BusinessException(ResultCode.INVALID_URL);
        }
        ShortUrlVo shortUrlVo = new ShortUrlVo();
        shortUrlVo.setShortCode(shortCode);
        shortUrlVo.setOriginalUrl(originalUrl);
        String shortUrl = domain + "/" + shortCode;
        // 拼接完整短链：域名前缀 + 短码
        shortUrlVo.setShortUrl(shortUrl);
        return Result.ok(shortUrlVo);
    }

}
