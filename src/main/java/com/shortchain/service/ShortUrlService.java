package com.shortchain.service;

import org.springframework.stereotype.Service;

@Service
public interface ShortUrlService {
    String generateShortCode(String originalUrl);

    String getOriginalUrl(String shortCode);

}
