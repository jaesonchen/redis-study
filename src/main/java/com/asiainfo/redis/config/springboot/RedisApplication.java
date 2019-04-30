package com.asiainfo.redis.config.springboot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.asiainfo.redis.service.IRedisService;

/**
 * springboot redis支持
 * 
 * @author       zq
 * @date         2017年11月2日  上午10:15:29
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
@SpringBootApplication
@ComponentScan({ "com.asiainfo.redis.service", "com.asiainfo.redis.config.springboot" })
@RestController
public class RedisApplication {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private IRedisService service;
    
    @RequestMapping("/set/{key}/{value}")
    Object set(@PathVariable("key") String key, @PathVariable("value") String value) {
        this.redisTemplate.opsForValue().set(key, value);
        return "success";
    }
    
    @RequestMapping("/get/{key}")
    Object get(@PathVariable("key") String key) {
        return this.redisTemplate.opsForValue().get(key);
    }

    @RequestMapping("/set")
    Object setAll() {
        Map<String, Object> map = new HashMap<>();
        for (int i = 10000; i < 20000; i++) {
            map.put(String.valueOf(i), i - 10000);
        }
        return this.service.pipelineWrite(map, 300) ? "success" : "failuer";
    }
    
    @RequestMapping("/get")
    List<Object> getAll() {
        List<String> list = new ArrayList<>();
        for (int i = 10000; i < 20000; i++) {
            list.add(String.valueOf(i));
        }
        return this.service.pipelineRead(list);
    }
	
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(new Object[] { RedisApplication.class });
		app.setAdditionalProfiles(new String[] { "redis" });
		app.run(args);
	}
}
