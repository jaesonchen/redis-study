package com.asiainfo.redis.config.springboot;

/**
 * TODO
 * 
 * @author       zq
 * @date         2017年11月2日  上午10:04:06
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public class RedisSerializerProperties {
	
	private String key;
	private String value;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "RedisSerializerProperties [key=" + key + ", value=" + value + "]";
	}
}
