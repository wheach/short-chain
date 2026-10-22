package com.shortchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短链映射实体，对应表 t_short_url
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_short_url")
public class ShortUrl {

    /** 主键：雪花算法生成的全局唯一 ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 短码（短链后缀），如 Ab3xY9 */
    private String shortCode;

    /** 原始长链 URL */
    private String originalUrl;

    /** 长链 sm3 值（生成幂等去重业务唯一键） */
    private String sm3Url;

    /** 状态：0 正常 / 1 禁用 */
    private Integer status;

    /** 过期时间，NULL 表示永不过期 */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
