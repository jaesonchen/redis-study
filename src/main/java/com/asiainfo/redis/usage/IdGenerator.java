package com.asiainfo.redis.usage;

import com.asiainfo.redis.util.IOUtils;
import com.asiainfo.redis.util.JedisClusterFactory;

import redis.clients.jedis.JedisCluster;

/**   
 * IdGenerator 分布式id生成器，jedis.incr(key)
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午5:47:34
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class IdGenerator {
    
    public static final String CLUSTER_NODES = "192.168.0.102:6010, 192.168.0.102:6020, 192.168.0.102:6030, 192.168.0.102:6040, 192.168.0.102:6050, 192.168.0.102:6060";
    public static final String KEY = "sequence_id";

    public static void main(String[] args) {
        JedisCluster jc = null;
        try {
            jc = JedisClusterFactory.instance().getJedisCluster(CLUSTER_NODES);
            // incr
            for (int i = 0; i < 100; i++) {
                jc.incr(KEY);
            }
            System.out.println(jc.get(KEY));
            
            // incrBy
            for (int i = 0; i < 100; i++) {
                jc.incrBy(KEY, 2);
            }
            System.out.println(jc.get(KEY));
        } finally {
            IOUtils.closeQuietly(jc);
        }
    }
}
