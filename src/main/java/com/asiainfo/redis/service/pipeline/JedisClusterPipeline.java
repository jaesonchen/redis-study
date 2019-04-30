package com.asiainfo.redis.service.pipeline;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

/**   
 * JedisCluster pipeline based on JedisCluster
 * JedisCluster 在集群模式下的批量操作功能。
 * 由于集群模式存在节点的动态添加删除，且client不能实时感知（只有在执行命令时才可能知道集群发生变更）；
 * 因此，该实现不保证一定成功，建议在批量操作之前调用 renewClusterSlots() 方法重新刷新集群JedisPool缓存。
 * 应用需要保证不论成功还是失败都会调用close() 方法，否则可能会造成泄露。如果失败需要应用自己去重试，因此每个批次执行的命令数量需要控制。
 * 建议在集群环境较稳定（增减节点不会过于频繁）的情况下使用，且允许失败或有对应的重试策略。
 * 
 * @author chenzq  
 * @date 2019年4月26日 下午10:44:09
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
public class JedisClusterPipeline extends PipelineBase implements Closeable {
    
    static final Charset UTF_8 = Charset.forName("utf-8");
    // JedisCluster 持有的JedisClusterConnectionHandler
    static final Field FIELD_CONNECTION_HANDLER;
    // JedisClusterConnectionHandler持有的JedisClusterInfoCache，用于缓存slot对应的 JedisPool
    static final Field FIELD_INFO_CACHE;
    
    static {
        FIELD_CONNECTION_HANDLER = getField(BinaryJedisCluster.class, "connectionHandler");
        FIELD_INFO_CACHE = getField(JedisClusterConnectionHandler.class, "cache");
    }
    
    // 基于JedisCluster实现，需要传入该对象
    protected JedisCluster jedisCluster;
    // BinaryJedisCluster持有的JedisClusterConnectionHandler
    protected JedisSlotBasedConnectionHandler handler;
    // JedisClusterConnectionHandler持有的JedisClusterInfoCache，slot对应的JedisPool缓存
    protected JedisClusterInfoCache cache;
    
    // pipeline操作使用的Client缓存，一次pipeline操作完成后，需要情况该缓存
    private Queue<Client> clients = new LinkedList<Client>();
    // slot对应节点的Jedis连接缓存，key使用slot对应的JedisPool，保证一个节点的命令使用的是同一个Client
    private Map<JedisPool, Jedis> jedisMap = new HashMap<>(16);
    // 每次pipeline操作的命令数
    private int pipelineCommand = 0;
    
    private JedisClusterPipeline(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
        this.handler = getFieldValue(jedisCluster, FIELD_CONNECTION_HANDLER);
        this.cache = getFieldValue(this.handler, FIELD_INFO_CACHE);
    }
    
    /**
     * @Description: 返回pipeline对象，用于管道操作
     * @author chenzq
     * @date 2019年4月26日 下午11:01:52
     * @param jedisCluster
     * @return
     */
    public static JedisClusterPipeline pipelined(JedisCluster jedisCluster) {
        return new JedisClusterPipeline(jedisCluster);
    }
    
    /**
     * @Description: 在进行pipeline批量读写之前最好调用一次该方法，保证slot cache里是最新的节点JedisPool
     * @author chenzq
     * @date 2019年4月26日 下午11:50:57
     */
    public void renewClusterSlots() {
        this.pipelineCommand = 0;
        this.cache.renewClusterSlots(null);
    }
    
    /**
     * @Description: 读取pipeline批量操作的所有Response，该方法用于不需要返回值的批量操作之后，
     *               syn把操作命令flush到服务端，并读取返回响应以免污染后续对该Jedis连接的使用。
     * @author chenzq
     * @date 2019年4月27日 上午12:17:37
     */
    public void sync() {
        try {
            for (Client client : clients) {
                // 在sync()调用时是不需要解析结果数据的，但是如果不调用get方法，发生了JedisMovedDataException这样的错误应用是不知道的，
                // 因此需要在这里调用get()来触发异常。
                generateResponse(client.getOne()).get();
                this.pipelineCommand--;
            }
        } finally {
            // 进行Response和缓存清理
            this.cleanPipeline();
        }
    }
    
    /**
     * @Description: 读取pipeline批量操作的所有Response，该方法用于需要返回值的批量操作之后，
     *               syncAndReturnAll把操作命令flush到服务端，并读取返回Response，
     *               Reponse中的数据类型取决于pipeline执行的命令返回值类型，通常byte[]类型返回值需要自己再使用对应的ValueSerializer实现解码。
     * @author chenzq
     * @date 2019年4月27日 上午12:22:24
     * @return
     */
    public List<Object> syncAndReturnAll() {
        List<Object> formatted = new ArrayList<Object>();
        try {
            for (Client client : clients) {
                formatted.add(generateResponse(client.getOne()).get());
                this.pipelineCommand--;
            }
        } finally {
            // 进行Response和缓存清理
            this.cleanPipeline();
        }
        return formatted;
    }

    /**
     * @Description: pipeline关闭，做销毁操作
     * @author chenzq
     * @date 2019年4月30日 下午6:13:31
     */
    @Override
    public void close() {
        // 进行Response和缓存清理
        this.cleanPipeline();
        // help gc
        this.jedisCluster = null;
        this.cache = null;
        this.handler = null;
        this.clients = null;
        this.jedisMap = null;
    }

    @Override
    protected Client getClient(String key) {
        return getClient(SafeEncoder.encode(key));
    }

    /**
     * 获取key对应的slot所在的节点Jedis连接，返回Jedis持有的Client对象
     */
    @Override
    protected Client getClient(byte[] key) {
        // 获取key对应的slot
        int slot = JedisClusterCRC16.getSlot(key);
        // 从JedisClusterInfoCache中读取slot对应的节点JedisPool(不从handler.getConnectionFromSlot(slot)读取Jedis连接是为了保证同一节点使用一个Client)
        JedisPool pool = this.cache.getSlotPool(slot);
        // 节点发生变动时，需要刷新节点缓存
        if (null == pool) {
            this.cache.renewClusterSlots(null);
        }
        pool = this.cache.getSlotPool(slot);
        if (null != pool) {
            // 先尝试从pipeline缓存里找
            Jedis jedis = this.jedisMap.get(pool);
            if (null == jedis) {
                // pipeline里没有缓存，则直接从JedisPool中取一个
                jedis = pool.getResource();
                // 新的Jedis连接，最好清空下Response，以免之前使用该Jedis连接的用户没有读取Response，造成污染
                this.cleanClientResponse(jedis.getClient());
                // 缓存Jedis
                this.jedisMap.put(pool, jedis);
            }
            Client client = jedis.getClient();
            // 缓存Client到pipeline中，用于后续按顺序读取Response
            this.clients.add(client);
            // 每次获取Client调用代表执行一条读写操作
            pipelineCommand++;
            return client;
        }
        // 取不到Jedis连接时，直接抛出异常
        throw new JedisNoReachableClusterNodeException("key( " + new String(key, UTF_8) + ") 对应的slot(" + slot + ")找不到可达的节点！");
    }
    
    // 清楚本次pipeline操作的所有缓存
    protected void cleanPipeline() {
        // 清空 pipelinedResponses队列
        super.clean();
        // 清空本次pipeline操作缓存的clients
        this.clients.clear();
        // 清空Client里的未读取的Response以免污染该Client的下次操作，并关闭Jedis连接(通常是返还到JedisPool中)
        for (Jedis jedis : this.jedisMap.values()) {
            if (this.pipelineCommand > 0) {
                // 清空Client里的Response，以免污染后续的操作
                this.cleanClientResponse(jedis.getClient());
            }
            // 返还到JedisPool
            jedis.close();
        }
        // 清空本次pipeline操使用Jedis连接缓存
        this.jedisMap.clear();
        // 重置pipeline计数
        this.pipelineCommand = 0;
    }
    
    // 清空Client里所有未读取的Response
    protected void cleanClientResponse(Client client) {
        try {
            // 读取Client的所有Response 字节
            client.getAll();
        } catch (Exception ex) {
            // ignore
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
            throw new RuntimeException("get field value fail", e);
        }
    }
}