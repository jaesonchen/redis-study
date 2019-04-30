package com.asiainfo.redis.usage;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.asiainfo.redis.util.IOUtils;
import com.asiainfo.redis.util.JedisClusterFactory;

import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.Transaction;
import redis.clients.util.JedisClusterCRC16;

/**   
 * redis集群对象JedisCluster不支持事务，但是，集群里面的每个节点支持事务，可以像pipeline那样分节点进行redis Transaction。
 * redis Transaction 应用，watch、multi、discard、exec，watch的key如果被其他Client修改，exec返回null或者空集合。
 * 
 * @author chenzq  
 * @date 2019年4月27日 下午7:42:27
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class JedisTransaction {

    public static final String CLUSTER_NODES = "192.168.0.102:6010, 192.168.0.102:6020, 192.168.0.102:6030, 192.168.0.102:6040, 192.168.0.102:6050, 192.168.0.102:6060";
    public static final String WATCH_KEY = "watch_key";
    public static final int DEFAULT_EXPIRE = 60;
    
    // JedisCluster 持有的JedisClusterConnectionHandler
    static final Field FIELD_CONNECTION_HANDLER;
    // JedisClusterConnectionHandler持有的JedisClusterInfoCache，用于缓存slot对应的 JedisPool
    static final Field FIELD_INFO_CACHE;
    
    static {
        FIELD_CONNECTION_HANDLER = getField(BinaryJedisCluster.class, "connectionHandler");
        FIELD_INFO_CACHE = getField(JedisClusterConnectionHandler.class, "cache");
    }

    // BinaryJedisCluster持有的JedisClusterConnectionHandler
    protected JedisSlotBasedConnectionHandler handler;
    // JedisClusterConnectionHandler持有的JedisClusterInfoCache，slot对应的JedisPool缓存
    protected JedisClusterInfoCache cache;
    
    public static void main(String[] args) throws Exception {

        JedisCluster jc = null;
        try {
            jc = JedisClusterFactory.instance().getJedisCluster(CLUSTER_NODES);
            JedisSlotBasedConnectionHandler handler = getFieldValue(jc, FIELD_CONNECTION_HANDLER);
            JedisClusterInfoCache cache = getFieldValue(handler, FIELD_INFO_CACHE);
            Jedis jedis = cache.getSlotPool(JedisClusterCRC16.getSlot(WATCH_KEY)).getResource();
            Jedis anotherJedis = cache.getSlotPool(JedisClusterCRC16.getSlot(WATCH_KEY)).getResource();
            new Thread(new TransactionTask(jedis, WATCH_KEY)).start();;
            new Thread(new AnotherTask(anotherJedis, WATCH_KEY)).start();;
            System.in.read();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(jc);
        }
    }
    
    static class TransactionTask implements Runnable {

        Jedis jedis;
        String key;
        public TransactionTask(Jedis jedis, String key) {
            this.jedis = jedis;
            this.key = key;
        }

        @Override
        public void run() {
            
            // watch key
            String watchReply = this.jedis.watch(this.key);
            System.out.println("TransactionTask watch reply status=" + watchReply);
            try {
                // begin tx
                Transaction tx = jedis.multi();
                tx.set(this.key, UUID.randomUUID().toString());
                // waiting another thread to modify key
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // ignore
                }
                tx.expire(this.key, DEFAULT_EXPIRE);
                
                // exec 如果watch期间key被修改，则exec返回null 或者空集合
                List<Object> list = tx.exec();
                if (null != list && !list.isEmpty()) {
                    list.stream().forEach(System.out::println);
                } else {
                    System.out.println("exec return null, tx is dicard!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // unwatch
                this.jedis.unwatch();
            }
        }
    }
    
    static class AnotherTask implements Runnable {

        Jedis jedis;
        String key;
        public AnotherTask(Jedis jedis, String key) {
            this.jedis = jedis;
            this.key = key;
        }
        
        @Override
        public void run() {
            String newValue = UUID.randomUUID().toString();
            this.jedis.set(this.key, newValue);
            System.out.println(this.key + " is set to new value: " + newValue);
        }
    }
    
    // 获取clazz 中的field
    static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("cannot find or access field '" + fieldName + "' from " + clazz.getName(), e);
        }
    }
    
    // 读取Field的值
    @SuppressWarnings({"unchecked" })
    static <T> T getFieldValue(Object obj, Field field) {
        try {
            return (T) field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
