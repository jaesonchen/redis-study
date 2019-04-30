package com.asiainfo.redis.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis缓存操作接口
 * 
 * @author       zq
 * @date         2017年4月1日  下午5:16:06
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public interface IRedisDao {
	
	/**
	 * 保存指定key和String类型值到redis缓存
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的String类型值
	 */
	public void setString(String key, String value);
	
	/**
	 * 保存指定key和String类型值到redis缓存，指定过期时间和单位
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的String类型值
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 */
	public void setString(String key, String value, long timeout, TimeUnit unit);
	
	/**
	 * 保存指定key和String类型值到redis缓存，指定过期时间，单位秒
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的String类型值
	 * @param expire	过期时间
	 */
	public void setString(String key, String value, long expire);
	
	/**
	 * 保存指定key和String类型值到redis缓存，指定过期日期
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的String类型值
	 * @param date		过期日期
	 */
	public void setString(String key, String value, Date date);
	
	/**
	 * 保存指定key到redis缓存
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的值
	 */
	public void setObject(String key, Object value);
	
	/**
	 * 保存指定key到redis缓存，指定过期时间和单位
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的值
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 */
	public void setObject(String key, Object value, long timeout, TimeUnit unit);
	
	/**
	 * 保存指定key到redis缓存，指定过期时间，单位秒
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的值
	 * @param expire	过期时间
	 */
	public void setObject(String key, Object value, long expire);
	
	/**
	 * 保存指定key到redis缓存，指定过期日期
	 * 
	 * @param key		要保存的key
	 * @param value		要保存的值
	 * @param date		过期日期
	 */
	public void setObject(String key, Object value, Date date);
	
	/**
	 * cas锁实现，不设置过期时间（永久锁，必须主动释放）
	 * 
	 * @param key		要获取锁的名称
	 * @param value		锁的值，通常使用某个id以在主动释放时判断是否自己的锁
	 * @return			获取成功=true、失败=false
	 */
	public boolean setIfAbsent(String key, Object value);

	/**
	 * cas锁实现，指定过期时间和单位（临时锁，如果不主动释放，到指定时间后自动释放）
	 * 
	 * @param key		要获取锁的名称
	 * @param value		锁的值，通常使用某个id以在主动释放时判断是否自己的锁
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 * @return			获取成功=true、失败=false
	 */
	public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);
	
	/**
	 * cas锁实现，指定过期时间，单位秒（临时锁，如果不主动释放，到指定时间后自动释放）
	 * 
	 * @param key		要获取锁的名称
	 * @param value		锁的值，通常使用某个id以在主动释放时判断是否自己的锁
	 * @param expire	过期时间
	 * @return			获取成功=true、失败=false
	 */
	public boolean setIfAbsent(String key, Object value, long expire);
	
	/**
	 * cas锁实现，指定过期日期（临时锁，如果不主动释放，到指定时间后自动释放）
	 * 
	 * @param key		要获取锁的名称
	 * @param value		锁的值，通常使用某个id以在主动释放时判断是否自己的锁
	 * @param date		过期日期
	 * @return			获取成功=true、失败=false
	 */
	public boolean setIfAbsent(String key, Object value, Date date);
	
	/**
	 * 获取指定key的String值
	 * 
	 * @param key	要获取的key
	 * @return
	 */
	public String getString(String key);
	
	/**
	 * 获取指定key的值
	 * 
	 * @param key	要获取的key
	 * @return
	 */
	public Object getObject(String key);
	
	/**
	 * 是否包含指定的key
	 * 
	 * @param key	要查询的key
	 * @return
	 */
	public boolean containsKey(String key);
	
	/**
	 * 修改key的名称
	 * 
	 * @param key		要修改的key
	 * @param newKey	修改后的key
	 */
	public void rename(String key, String newKey);
	
	/**
	 * 删除指定key
	 * 
	 * @param key	要删除的key
	 */
	public void remove(String key);
	
	/**
	 * 获取指定pattern的所有key
	 * 
	 * @param pattern	要获取的pattern
	 * @return
	 */
	public Set<String> keys(String pattern);
	
	/**
	 * 设置key的过期时间，单位秒
	 * 
	 * @param key		要设置的key
	 * @param expire	过期时间
	 * @return
	 */
	public boolean expire(String key, long expire);
	
	/**
	 * 设置key的过期时间，指定单位
	 * 
	 * @param key		要设置的key
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 * @return
	 */
	public boolean expire(String key, long timeout, TimeUnit unit);
	
	/**
	 * 设置key的过期时间，指定到date
	 * 
	 * @param key	要设置的key
	 * @param date	过期的日期
	 * @return
	 */
	public boolean expire(String key, Date date);
	
	/**
	 * 获取key的过期时间，单位秒
	 * 
	 * @param key	要获取的key
	 * @return
	 */
	public Long getExpire(String key);
	
	/**
	 * 获取key的过期时间，指定单位
	 * 
	 * @param key	要获取的key
	 * @param unit	过期时间的单位
	 * @return
	 */
	public Long getExpire(String key, TimeUnit unit);
	
	/**
	 * 添加String类型set集合元素
	 * 
	 * @param key		集合的key
	 * @param values	要添加的String类型元素数组
	 * @return
	 */
	public boolean sAdd(String key, String... values);
	
	/**
	 * 删除String类型set集合的元素
	 * 
	 * @param key		集合的key
	 * @param values	要删除的String类型元素数组
	 * @return
	 */
	public boolean sRem(String key, String... values);
	
	/**
	 * 获取String类型set集合的内容
	 * 
	 * @param key	集合的key
	 * @return
	 */
	public Set<String> sMembers(String key);
	
	/**
	 * 添加String类型map集合的一个字段
	 * 
	 * @param key
	 * @param field
	 * @param value
	 * @return
	 */
	public boolean hSetString(String key, String field, String value);
	
	/**
	 * 添加String类型map集合
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public void hMSetString(String key, Map<String, String> map);
	
	/**
	 * 添加Object类型map集合的一个字段
	 * 
	 * @param key
	 * @param field
	 * @param value
	 * @return
	 */
	public boolean hSet(String key, Object field, Object value);
	
	/**
	 * 添加Object类型map集合
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public void hMSet(String key, Map<Object, Object> map);	
	
	/**
	 * 获取String类型map集合的一个字段内容
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public String hGetString(String key, String field);
	
	/**
	 * TODO
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public List<String> hMGetString(String key, String... fields);
	
	/**
	 * 获取String类型map集合的所有字段内容
	 * 
	 * @param key
	 * @return
	 */
	public Map<String, String> hGetAllString(String key);
	
	/**
	 * 获取Object类型map集合的一个字段内容
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public Object hGet(String key, Object field);
	
	/**
	 * 获取Object类型map集合的多个字段内容
	 * 
	 * @param key
	 * @param fields
	 * @return
	 */
	public List<Object> hMGet(String key, Object... fields);
	
	/**
	 * 获取Object类型map集合的所有字段内容
	 * 
	 * @param key
	 * @return
	 */
	public Map<Object, Object> hGetAll(String key);
	
	/**
	 * 批量写redis缓存
	 * 
	 * @param map	批量写的key-value集合
	 */
	public void multiSet(Map<String, Object> map);
	
	/**
	 * 批量写redis缓存
	 * 
	 * @param map		批量写的key-value集合
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 */
	public void multiSet(Map<String, Object> map, long timeout, TimeUnit unit);
	
	/**
	 * 批量获取redis内容
	 * 
	 * @param list	key列表
	 * @return
	 */
	public List<Object> multiGet(List<String> list);
	
	/**
	 * spring-data cluster暂时不支持pipeline方式，自己实现
	 * 
	 * @param map	批量写的key-value集合
	 */
	public void pipelineSet(Map<String, Object> map);
	
	/**
	 * spring-data cluster暂时不支持pipeline方式，自己实现
	 * 
	 * @param map		批量写的key-value集合
	 * @param timeout	过期时间
	 * @param unit		过期时间单位
	 */
	public void pipelineSet(Map<String, Object> map, long timeout, TimeUnit unit);
	
	/**
	 *  spring-data cluster暂时不支持pipeline方式，自己实现
	 * 
	 * @param list	key列表
	 * @return
	 */
	public List<Object> pipelineGet(List<String> list);	
}
