package com.oshacker.discusscommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);

        //设置key的序列化和反序列化
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化和反序列化
        template.setValueSerializer(RedisSerializer.json());
        //设置hash类型key的序列化和反序列化
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash类型value的序列化和反序列化
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }
}
