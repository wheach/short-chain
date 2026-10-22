package com.shortchain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortUrlVo {
    String shortUrl;
    String shortCode;
    String originalUrl;
}
