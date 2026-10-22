package com.shortchain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.shortchain.mapper")
public class ShortchainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortchainApplication.class, args);
    }

}
