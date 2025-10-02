package org.tuna.zoopzoop.backend.global.config.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.tuna.zoopzoop.backend.domain.auth.dto.AuthResultData;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, AuthResultData> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AuthResultData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());
        // Value Serializer (JSON 직렬화)
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(AuthResultData.class));

        return template;
    }
}