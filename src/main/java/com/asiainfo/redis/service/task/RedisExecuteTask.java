package com.asiainfo.redis.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asiainfo.redis.service.IExecutor;
import com.asiainfo.redis.service.IRedisDao;

/**
 * Redis 异步任务
 * 
 * @author       zq
 * @date         2017年4月5日  下午4:04:55
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public class RedisExecuteTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(RedisExecuteTask.class);
	
	private IRedisDao redisDao;
	private IExecutor executor;
	
	public RedisExecuteTask(IRedisDao redisDao, IExecutor executor) {
		this.redisDao = redisDao;
		this.executor = executor;
	}

	@Override
	public void run() {
		
		logger.debug("开始执行redis异步任务 ......");
		try {
			this.executor.execute(this.redisDao);
			logger.debug("redis异步任务执行结束 ......");
			return;
		} catch (Exception ex) {
			logger.error("执行redis异步任务时出现异常，异常信息：\n{}", ex);
		}
	}
}
