package com.asiainfo.redis.config.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * springboot redis cluster 自动配置
 * 
 * @author       zq
 * @date         2017年11月1日  下午5:37:36
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
@Configuration
public class RedisConfig {

	final Logger logger = LoggerFactory.getLogger(getClass());
	
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
