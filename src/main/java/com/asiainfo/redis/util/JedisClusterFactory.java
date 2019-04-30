package com.asiainfo.redis.util;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

/**
 * JedisClusterFactory 获取集群环境下的JedisCluster
 * 
 * @author chenzq  
 * @date 2019年4月24日 下午5:39:59
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class JedisClusterFactory {
    
    private static class JedisClusterFactoryHolder {
        private static JedisClusterFactory instance = new JedisClusterFactory();
    }
    
    public static JedisClusterFactory instance() {
        return JedisClusterFactoryHolder.instance;
    }
    
    // 获取JedisCluster连接
    public JedisCluster getJedisCluster(String clusterNodes) {
        
        JedisCluster jc = null;
        try {
            // Jedis连接池配置，基于common-pool2
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            // 最大连接数, 默认8个
            poolConfig.setMaxTotal(100);
            // 最大空闲连接数，默认8
            poolConfig.setMaxIdle(50);
            // 最小空闲连接数, 默认0
            poolConfig.setMinIdle(10);
            // 获取连接时的最大等待时间, 如果超时就抛异常, 默认-1
            poolConfig.setMaxWaitMillis(2000);
            // 从连接池中取connection时进行validateObject校验
            poolConfig.setTestOnBorrow(true);
            
            // 空闲时测试连接，默认true
            //poolConfig.setTestWhileIdle(true);
            // 空闲连接删除的最小时间，默认60ms
            //poolConfig.setMinEvictableIdleTimeMillis(60000);
            // 删除空闲连接线程的运行间隔，默认30s
            //poolConfig.setTimeBetweenEvictionRunsMillis(30000);
            
            // DEFAULT_TIMEOUT = 2000
            // DEFAULT_MAX_REDIRECTIONS(maxAttempts) = 5
            //jc = new JedisCluster(toHostAndPort(clusterNodes), poolConfig);
            jc = new JedisCluster(toHostAndPort(clusterNodes), 6000, 3, poolConfig);
        } catch (Exception e) {
            // ignore
        }
        return jc;
    }

    // 集群节点转换为Set<HostAndPort>
    private Set<HostAndPort> toHostAndPort(String hosts) {
        
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        try {
            for (String ipPort : hosts.split(",")) {
                String[] ipAndPort = ipPort.split(":");
                nodes.add(new HostAndPort(ipAndPort[0].trim(), Integer.parseInt(ipAndPort[1].trim())));
            }
        } catch (Exception ex) {
            // ignor
        }
        return nodes;
    }
}
