package com.shortchain.controller;

import com.shortchain.common.ResultCode;
import com.shortchain.entity.Result;
import com.shortchain.service.ShortUrlService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 短链跳转控制器（映射到根路径，独立于业务接口）
 * <p>
 * 设计说明：短链跳转是给「浏览器直接访问短链」用的，短链应尽量短，
 * 因此跳转接口映射到根路径 /{shortCode}（如 http://localhost:8080/2vPLmUHXVGo），
 * 不挂 /api/v1 前缀。/api/v1 前缀保留给「生成短链」等业务接口使用。
 */
@RestController
@Slf4j
public class ShortUrlRedirectController {

    @Resource
    private ShortUrlService shortUrlService;

    /**
     * 根据短码找OriginalUrl
     * @param shortCode
     * @return
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> getOriginalUrl(@PathVariable("shortCode") String shortCode) {
        String originalUrl = shortUrlService.getOriginalUrl(shortCode);
        if (originalUrl == null) {
            // 不存在：返回 404 + 统一 JSON
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail(ResultCode.NOT_FOUND));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
