package com.yowyob.erp.config.redis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

        @Value("${spring.data.redis.host}")
        private String redis_host;

        @Value("${spring.data.redis.port}")
        private int redis_port;

        @Value("${spring.data.redis.password:}")
        private String redis_password;

        @Value("${spring.data.redis.timeout:5000}")
        private long redis_timeout;

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis_host, redis_port);
                if (redis_password != null && !redis_password.isBlank()) {
                        config.setPassword(redis_password);
                }

                LettuceClientConfiguration client_config = LettuceClientConfiguration.builder()
                                .commandTimeout(Duration.ofMillis(redis_timeout))
                                .shutdownTimeout(Duration.ofSeconds(2))
                                .build();

                log.info("🧩 Redis Connection initialized → host={} port={} timeout={}ms", redis_host, redis_port,
                                redis_timeout);
                return new LettuceConnectionFactory(config, client_config);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connection_factory) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());

                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connection_factory);
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
                template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
                template.afterPropertiesSet();

                log.info("✅ RedisTemplate configured with JSON serialization and timeout={}ms", redis_timeout);
                return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .serializeKeysWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                .disableCachingNullValues();

                RedisCacheManager manager = RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration("ecrituresAll", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                                .withCacheConfiguration("compteAll", defaultConfig.entryTtl(Duration.ofHours(1)))
                                .withCacheConfiguration("compteSolde", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                                .withCacheConfiguration("sessionCache", defaultConfig.entryTtl(Duration.ofHours(2)))
                                .build();

                log.info(
                                "🧠 Redis CacheManager initialized (TTL: compteAll=1h, compteSolde=5m, ecrituresAll=10m, sessionCache=2h)");
                return manager;
        }
}
