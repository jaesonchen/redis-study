package com.asiainfo.redis.service.task;

import java.util.concurrent.ThreadFactory;

/**
 * @Description: 线程Factory
 * 
 * @author       zq
 * @date         2017年10月16日  下午4:08:08
 * Copyright: 	  北京亚信智慧数据科技有限公司
 */
public class CustomThreadFactory implements ThreadFactory {

	private int counter = 0;
	private String prefix = "redis-async";
	public CustomThreadFactory() {}
	public CustomThreadFactory(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, prefix + "-" + counter++);
	}
}
