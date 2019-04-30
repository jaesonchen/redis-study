package com.asiainfo.redis.config.annotation;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConverters;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.asiainfo.redis.config.springboot.RedisSerializerProperties;

import redis.clients.jedis.JedisPoolConfig;

/**   
 * annotation 配置，不依赖springboot 自动配置
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午3:41:08
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
@Configuration
@PropertySource(value = { "classpath:redis.properties" })
public class RedisConfig {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Environment env;
    
    //spring.redis.cluster.nodes
    //spring.redis.cluster.max-redirects
    @Bean
    public RedisClusterConfiguration clusterConfiguration() {
        ResourcePropertySource proptertySource = null;
        try {
            proptertySource = new ResourcePropertySource("redis.properties", "classpath:redis.properties");
        } catch (IOException ex) {
            throw JedisConverters.toDataAccessException(ex);
        }
        return new RedisClusterConfiguration(proptertySource);
    }
    
    @Bean
    public JedisPoolConfig poolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(Integer.parseInt(env.getProperty("redis.maxIdle", "20")));
        poolConfig.setMinIdle(Integer.parseInt(env.getProperty("redis.minIdle", "10")));
        poolConfig.setMaxTotal(Integer.parseInt(env.getProperty("redis.maxTotal", "100")));
        poolConfig.setMaxWaitMillis(Integer.parseInt(env.getProperty("redis.maxWait", "1000")));
        poolConfig.setTestOnBorrow(Boolean.valueOf(env.getProperty("redis.testOnBorrow", "true")));
        return poolConfig;
    }

    @Bean
    public JedisConnectionFactory connectionFactory(RedisClusterConfiguration clusterConfig, JedisPoolConfig poolConfig) {
        JedisConnectionFactory factory = new JedisConnectionFactory(clusterConfig, poolConfig);
        factory.setPassword(env.getProperty("redis.password", ""));
        factory.setTimeout(Integer.parseInt(env.getProperty("redis.timeout", "60000")));
        return factory;
    }
    
    @Bean
    @ConfigurationProperties(prefix="spring.redis.serializer")
    public RedisSerializerProperties serializerProperteis() {
        return new RedisSerializerProperties();
    }
    
    @Bean
    @Qualifier("keySerializer")
    public RedisSerializer<?> keySerializer(RedisSerializerProperties properties) {
        
        logger.info("spring-redis key serializer init, key-class={}", properties.getKey());
        try {
            Class<?> clazz = Class.forName(properties.getKey());
            return (RedisSerializer<?>) clazz.newInstance();
        } catch (Exception ex) {
            return new StringRedisSerializer();
        }
    }
    
    @Bean
    @Qualifier("valueSerializer")
    public RedisSerializer<?> valueSerializer(RedisSerializerProperties properties) {
        
        logger.info("spring-redis key serializer init, value-class={}", properties.getValue());
        try {
            Class<?> clazz = Class.forName(properties.getValue());
            return (RedisSerializer<?>) clazz.newInstance();
        } catch (Exception ex) {
            return new StringRedisSerializer();
        }
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory factory, 
            @Qualifier("keySerializer") RedisSerializer<?> keySerializer, 
            @Qualifier("valueSerializer") RedisSerializer<?> valueSerializer) {
        
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);
            template.setKeySerializer(keySerializer);
            template.setValueSerializer(valueSerializer);
            return template;
        } catch (Exception ex) {
            throw new RuntimeException("error on builder redistemplate!", ex);
        }
    }
}
