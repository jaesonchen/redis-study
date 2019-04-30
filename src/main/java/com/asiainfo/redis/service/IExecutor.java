package com.asiainfo.redis.service;

/**
 * redis回调接口
 * 
 * @author       zq
 * @date         2017年4月5日  下午4:06:07
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public interface IExecutor {
    
    /**
     * 回调方法
     * 
     * @param redisDao
     */
	public void execute(IRedisDao redisDao);
}
