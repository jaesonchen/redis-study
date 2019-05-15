package com.asiainfo.redis.usage;

import java.util.UUID;

import com.asiainfo.redis.util.IOUtils;
import com.asiainfo.redis.util.JedisClusterFactory;

import redis.clients.jedis.JedisCluster;

/**   
 * DistributeLock redis分布式锁实现jc.setnx(key)、 jc.expire(key, second)、jc.del(key)
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午5:55:11
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class DistributeLock {

    public static final String CLUSTER_NODES = "192.168.0.102:6010, 192.168.0.102:6020, 192.168.0.102:6030, 192.168.0.102:6040, 192.168.0.102:6050, 192.168.0.102:6060";
    public static final String LOCK_KEY = "lock_key";
    public static final int DEFAULT_EXPIRE = 60;
    public static final long SUCCESS = 1;
    public static final long FAILURE = 1;
    
    public static void main(String[] args) {
        
        DistributeLock dl = new DistributeLock();
        JedisCluster jc = null;
        try {
            jc = JedisClusterFactory.getInstance().getJedisCluster(CLUSTER_NODES);
            // acquireLock
            String lock = dl.acquireLock(jc, LOCK_KEY);
            dl.acquireLock(jc, LOCK_KEY);
            
            // releaseLock
            System.out.println(dl.releaseLock(jc, LOCK_KEY, null));
            System.out.println(dl.releaseLock(jc, LOCK_KEY, UUID.randomUUID().toString()));
            System.out.println(dl.releaseLock(jc, LOCK_KEY, lock));
        } finally {
            IOUtils.closeQuietly(jc);
        }
    }
    
    // 获取锁
    public String acquireLock(final JedisCluster jc, final String key) {
        final String lock = UUID.randomUUID().toString();
        try {
            if (SUCCESS == jc.setnx(key, lock)) {
                System.out.printf("成功获取到锁key=%s，lockid=%s ......", key, lock);
                System.out.println();
                jc.expire(key, DEFAULT_EXPIRE);
                return lock;
            }
            System.out.printf("未能获取到锁key=%s ......", key);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.printf("获取锁key=%s时出现异常", key);
        }
        System.out.println();
        return null;
    }
    
    // 释放锁
    public boolean releaseLock(final JedisCluster jc, final String key, final String lock) {
        try {
            String currentLock = jc.get(key);
            if (null == currentLock || !currentLock.equals(lock)) {
                System.out.printf("释放锁key=%s(%s)已过期，当前锁(%s)", key, lock, currentLock);
                System.out.println();
                return true;
            }
            jc.del(key);
            System.out.printf("释放锁key=%s(%s)成功", key, lock);
            System.out.println();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.printf("释放锁key=%s(%s)时出现异常", key, lock);
            System.out.println();
        }
        return false;
    }
}
