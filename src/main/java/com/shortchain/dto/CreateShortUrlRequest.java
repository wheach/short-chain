package com.shortchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 生成短链请求 DTO（接口入参）
 * 只暴露必要字段，与 Entity 解耦
 */
@Data
public class CreateShortUrlRequest {

    @NotBlank(message = "originalUrl 不能为空")
    @Size(max = 2048, message = "链接长度不能超过 2048")
    private String originalUrl;
}
