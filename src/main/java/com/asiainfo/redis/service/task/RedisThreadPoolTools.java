package com.asiainfo.redis.service.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executors 工具 
 * 
 * @author       zq
 * @date         2017年4月5日  上午11:17:23
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public class RedisThreadPoolTools {
	
	private static final Logger logger = LoggerFactory.getLogger(RedisThreadPoolTools.class);
	
	private static final int DEFAULT_SIZE = 10;
	private final ThreadFactory defaultFactory = new CustomThreadFactory();
	private final ExecutorService service;
	
	private RedisThreadPoolTools(int size) {
		this.service = new ThreadPoolExecutor(size, size,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                this.defaultFactory);
	}
	
	static class RedisThreadPoolHolder {
		static final RedisThreadPoolTools INSTANCE = new RedisThreadPoolTools(DEFAULT_SIZE);
	}
	
    public Thread newThread(Runnable r) {
        return this.defaultFactory.newThread(r);
    }
    
    public Thread newThread(Runnable r, String threadName) {
        return new CustomThreadFactory(threadName).newThread(r);
    }
    
	public static RedisThreadPoolTools getInstance() {
		return RedisThreadPoolHolder.INSTANCE;
	}
	
	public void execute(Runnable r) {
		try {
			this.service.execute(r);
		} catch (Exception ex) {
			logger.error("线程调度发生异常，异常信息如下：\n{}", ex);
		}
	}
	
	public static void shutdown(ExecutorService service) {
        try {
            if (null != service) {
                service.shutdown();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
