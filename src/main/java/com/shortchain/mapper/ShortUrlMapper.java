package com.shortchain.mapper;

import com.shortchain.entity.ShortUrl;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShortUrlMapper {

    @Insert("INSERT INTO t_short_url (id, short_code, original_url, sm3_url, status, expire_at, created_at, updated_at) " +
            "VALUES (#{id}, #{shortCode}, #{originalUrl}, #{sm3Url}, #{status}, #{expireAt}, #{createdAt}, #{updatedAt})")
    void insert(ShortUrl entity);

    @Select("SELECT * FROM t_short_url WHERE short_code = #{shortCode}")
    ShortUrl selectByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT * FROM t_short_url WHERE sm3_url = #{sm3Url}")
    ShortUrl selectBySm3(@Param("sm3Url") String sm3Url);
}

