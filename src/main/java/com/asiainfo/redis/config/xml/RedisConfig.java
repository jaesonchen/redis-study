package com.asiainfo.redis.config.xml;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**   
 * xml 配置文件，不依赖springboot 自动配置
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午3:41:08
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
@Configuration
@ImportResource("classpath:application-redis-configure.xml")
public class RedisConfig {

}
