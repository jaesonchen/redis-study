package com.asiainfo.redis.util;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * redis fastjson序列化实现类
 * 
 * @author       zq
 * @date         2017年11月7日  上午10:56:50
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public class FastjsonRedisSerializer implements RedisSerializer<Object> {

    private static SerializerFeature[] features = { SerializerFeature.WriteClassName };
    
    /*
     * @see org.springframework.data.redis.serializer.RedisSerializer#deserialize(byte[])
     */
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
    	
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parse(bytes);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    /*
     * @see org.springframework.data.redis.serializer.RedisSerializer#serialize(java.lang.Object)
     */
    @Override
    public byte[] serialize(Object t) throws SerializationException {
    	
        if (t == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONBytes(t, features);
        } catch (Exception ex) {
            throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }
}