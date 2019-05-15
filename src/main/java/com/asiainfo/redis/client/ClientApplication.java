package com.asiainfo.redis.client;

import com.asiainfo.redis.util.IOUtils;
import com.asiainfo.redis.util.JedisClusterFactory;

import redis.clients.jedis.JedisCluster;

/**   
 * JedisCluster 不使用springredis支持
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午5:22:57
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class ClientApplication {

    public static final String CLUSTER_NODES = "192.168.0.102:6010, 192.168.0.102:6020, 192.168.0.102:6030, 192.168.0.102:6040, 192.168.0.102:6050, 192.168.0.102:6060";
    
    public static void main(String[] args) {
        
        JedisCluster jc = null;
        try {
            jc = JedisClusterFactory.getInstance().getJedisCluster(CLUSTER_NODES);
            System.out.println(jc.get("10001"));
        } finally {
            IOUtils.closeQuietly(jc);
        }
    }
}
