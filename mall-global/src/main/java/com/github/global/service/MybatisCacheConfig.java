package com.github.global.service;

import com.github.liuanxin.caches.MybatisRedisCache;
import com.github.liuanxin.caches.RedisContextUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnClass({ MybatisRedisCache.class, RedisTemplate.class })
@ConditionalOnBean(RedisTemplate.class)
public class MybatisCacheConfig {

    @Bean
    public RedisContextUtils redisContext() {
        return new RedisContextUtils();
    }
}
