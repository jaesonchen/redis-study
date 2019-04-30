/**   
 - Jedis 实现与节点的连接、命令传输、返回结果读取
 - Jedis extends BinaryJedis, Jedis持有Pool对象，在close时，如果Pool不为空则，释放会缓存池中
    protected Pool<Jedis> dataSource = null;
 - Jedis 提供基本类型和String参数的方法，BinaryJedis提供byte[]类型参数方法，String类型使用utf8编码转换成byte[]
 - BinaryJedis持有Client、Pipeline、Transaction
 - 真正的操作最后都会由Client执行, Jedis/BinaryJedis 在执行操作前都会先调用checkIsInMultiOrPipeline检查当前Jedis连接是否在进行multi、pipeline操作 
  protected Client client = null;
  protected Transaction transaction = null;
  protected Pipeline pipeline;

 - Client extends BinaryClient, BinaryClient extends Connection
 - Client 提供基本类型和String参数的方法，BinaryClient提供byte[]类型参数方法
 - Connection 提供socket连接，socket.setReuseAddress(true);socket.setKeepAlive(true);socket.setTcpNoDelay(true);socket.setSoLinger(true, 0);
 - Connection 通过Protocol.sendCommand(out, cmd, args) 发送命令到redis节点对应的Client
    out.write(ASTERISK_BYTE);    //(byte)42
    out.writeIntCrLf(args.length + 1);
    out.write(DOLLAR_BYTE);      //(byte)36
    out.writeIntCrLf(command.length);
    out.write(command);
    out.writeCrLf();
    out.flush();

 - Connection 通过Protocol.processStatusCodeReply(is) 返回 byte[] 状态反馈
 - Connection 通过Protocol.processInteger(is) 返回 Long 执行结果
 - Connection 通过Protocol.processBulkReply(is) 返回 byte[] 大对象执行结果
 - Connection 通过Protocol.processMultiBulkReply(is) 返回List<Object> 多个大对象执行结果 
 
************************************************************************************************************************
 - JedisClusterCRC16 用于获取key对应的slot
 - JedisCluster 集群环境下的封装，实现集群环境下的节点slot和host:port对应的JedisPool缓存
 -              通过getConnectionFromSlot获取key对应的slot节点的Jedis连接进行读写操作，通过JedisClusterCommand回调 实现失败重试功能
 - JedisCluster extends BinaryJedisCluster
 - BinaryJedisCluster 持有 JedisClusterConnectionHandler, protected JedisClusterConnectionHandler connectionHandler; protected int maxAttempts;
 
 - JedisClusterConnectionHandler持有JedisClusterInfoCache, protected final JedisClusterInfoCache cache;
 - JedisSlotBasedConnectionHandler extends JedisClusterConnectionHandler 实现了initializeSlotsCache和getConnection方法
    // 构建
    private void initializeSlotsCache(Set<HostAndPort> startNodes, GenericObjectPoolConfig poolConfig, String password) {
        for (HostAndPort hostAndPort : startNodes) {
            Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort());
            if (password != null) {
                jedis.auth(password);
            }
            try {
                cache.discoverClusterNodesAndSlots(jedis);
                break;
            } catch (JedisConnectionException e) {
                // try next nodes
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }
    // 随机节点jedis连接
    public Jedis getConnection() {
        List<JedisPool> pools = cache.getShuffledNodesPool();
        for (JedisPool pool : pools) {
            Jedis jedis = null;
            try {
                jedis = pool.getResource();
                if (jedis == null) {
                    continue;
                }
                // 测试连接
                String result = jedis.ping();
                if (result.equalsIgnoreCase("pong")) return jedis;
                jedis.close();
            } catch (JedisException ex) {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
        throw new JedisNoReachableClusterNodeException("No reachable node in cluster");
    }
    
    // 返回slot对应node的jedis 连接
    public Jedis getConnectionFromSlot(int slot) {
        JedisPool connectionPool = cache.getSlotPool(slot);
        if (connectionPool != null) {
            // It can't guaranteed to get valid connection because of node assignment
            return connectionPool.getResource();
        } else {
            //It's abnormal situation for cluster mode, that we have just nothing for slot, try to rediscover state
            renewSlotCache();
            connectionPool = cache.getSlotPool(slot);
            if (connectionPool != null) {
                return connectionPool.getResource();
            } else {
                //no choice, fallback to new connection to random node
                return getConnection();
            }
        }
    }

 - JedisClusterInfoCache 持有Map<String, JedisPool> nodes和Map<Integer, JedisPool> slots两个jedispool缓存，key分别是节点 host:port 和 slot
    private final GenericObjectPoolConfig poolConfig;
    private final Map<String, JedisPool> nodes = new HashMap<String, JedisPool>(); // ip：port 对应的连接池缓存
    private final Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>(); // slot 对应的连接池缓存
    private static final int MASTER_NODE_INDEX = 2; // 主节点SlotInfo下标，通常前两位是节点slot的范围
    // 节点的槽位集合:[[10924, 16383, [B@4ae82894, 6386], [B@543788f3, 6387]], [5462, 10923, [B@6d3af739, 6384], [B@1da51a35, 6385]], [0, 5461, [B@16022d9d, 6382], [B@7e9a5fbe, 6383]]]

 - JedisClusterInfoCache实现了discoverClusterNodesAndSlots()初始化 和 renewSlotCache()刷新集群slot jedispool缓存
    // 初始化时，同时初始化ip:port为key的缓存(包括master、slave)，slot为key的缓存(只缓存master的slot对应的jedispool)
    public void discoverClusterNodesAndSlots(Jedis jedis) {
        w.lock();
        try {
            reset(); // destroy cache
            List<Object> slots = jedis.clusterSlots(); // 集群节点slot信息
            for (Object slotInfoObj : slots) { // 遍历节点slot信息
                List<Object> slotInfo = (List<Object>) slotInfoObj; // [slotIndexStart, slotIndexEnd, [ip byte[], port], [ip byte[], port] ...]
                // 如果此节点信息少于3 个, 说明节点出错没有ip:host信息，跳过此次循环
                if (slotInfo.size() <= MASTER_NODE_INDEX) {
                    continue;
                }
                // 节点对应的所有slot数字，用于在cache里作为key存储jedispool
                List<Integer> slotNums = getAssignedSlotArray(slotInfo);
                int size = slotInfo.size();
                // 遍历 slot对应的master、slave节点ip:host，从下表2开始
                for (int i = MASTER_NODE_INDEX; i < size; i++) {
                    List<Object> hostInfos = (List<Object>) slotInfo.get(i);
                    if (hostInfos.isEmpty()) {
                        continue;
                    }
                    // 解析主机信息 HostAndPort
                    HostAndPort targetNode = generateHostAndPort(hostInfos);
                    // 如果 以ip:host为key的nodes缓存里没有对应的jedispool，就使用poolconfig 新建一个并放入缓存，nodes缓存master/slave节点对应的jedispool
                    // new JedisPool(poolConfig, node.getHost(), node.getPort()) 放入nodes中
                    setupNodeIfNotExist(targetNode);
                    // slot缓存只缓存master节点slot对应的jedispool
                    if (i == MASTER_NODE_INDEX) {
                        assignSlotsToNode(slotNums, targetNode);
                    }
                }
            }
        } finally {
            w.unlock();
        }
    }
    // 刷新时只更新ip:port为key的缓存(只更新master)，slot为key的缓存(只更新master的slot对应的jedispool)
    public void renewClusterSlots(Jedis jedis) {
        try {
            w.lock();
            List<Object> slots = jedis.clusterSlots();
            for (Object slotInfoObj : slots) {
                List<Object> slotInfo = (List<Object>) slotInfoObj;
                if (slotInfo.size() <= MASTER_NODE_INDEX) {
                    continue;
                }
                List<Integer> slotNums = getAssignedSlotArray(slotInfo);
                // 刷新时只取主节点的ip:host
                List<Object> hostInfos = (List<Object>) slotInfo.get(MASTER_NODE_INDEX);
                if (hostInfos.isEmpty()) {
                    continue;
                }
                HostAndPort targetNode = generateHostAndPort(hostInfos);
                // 刷新master节点的缓存
                assignSlotsToNode(slotNums, targetNode);
            }
        } finally {
            w.unlock();
        }
    }

 - JedisCluster通过JedisClusterCommand 实现失败重试功能
    new JedisClusterCommand<Long>(connectionHandler, maxAttempts) {
        @Override
        public Long execute(Jedis connection) {
            return connection.pexpireAt(key, millisecondsTimestamp);
        }
    }.run(key);
    
    private ThreadLocal<Jedis> askConnection = new ThreadLocal<Jedis>();
    private T runWithRetries(byte[] key, int attempts) {
        if (attempts <= 0) {
            throw new JedisClusterMaxRedirectionsException("Too many Cluster redirections?");
        }
        Jedis connection = null;
        try {
            // 第一次 false，如果节点 A 正在迁移槽 i 至节点 B ， 那么当节点 A 没能在自己的数据库中找到命令指定的数据库键时,
            // 节点 A 会向客户端返回一个 ASK 错误， 指引客户端到节点 B 继续查找指定的数据库键
            if (asking) {
                // TODO: Pipeline asking with the original command to make it faster
                connection = askConnection.get();
                // 到目标节点打开客户端连接标识
                connection.asking();
                // if asking success, reset asking flag
                asking = false;
            } else {
                // 通过 CRC16 算法获取 slot 对应的节点的连接池中的连接
                connection = connectionHandler.getConnectionFromSlot(JedisClusterCRC16.getSlot(key));
            }
            return execute(connection);
        // 集群不存在
        } catch (JedisNoReachableClusterNodeException jnrcne) {
            throw jnrcne;
        // 连接异常
        } catch (JedisConnectionException jce) {
            releaseConnection(connection);
            connection = null;
            // 如果重试次数只有一次，那就更新连接池，并抛出异常
            if (attempts <= 1) {
                // do renewing only if there were no successful responses from this node last few seconds
                this.connectionHandler.renewSlotCache();
                //no more redirections left, throw original exception, not JedisClusterMaxRedirectionsException, because it's not MOVED situation
                throw jce;
            }
            // 递归重试，重试次数减一
            return runWithRetries(key, attempts - 1);
        // 如果是重定向异常，例如 moved ，ASK
        } catch (JedisRedirectionException jre) {
            // if MOVED redirection occurred,
            // 节点在接到一个命令请求时， 会先检查这个命令请求要处理的键所在的槽是否由自己负责， 如果不是的话， 节点将向客户端返回一个 MOVED 错误，
            // MOVED 错误携带的信息可以指引客户端转向至正在负责相关槽的节点
            if (jre instanceof JedisMovedDataException) {
                // it rebuilds cluster's slot cache
                // 如果是 moved 错误，就更新连接池， ASK 就不必更新缓存，只需要临时访问就行
                this.connectionHandler.renewSlotCache(connection);
            }
            releaseConnection(connection);
            connection = null;
            
            // 如果是 ASK
            if (jre instanceof JedisAskDataException) {
                asking = true;
                // 设置 ThreadLocal，新的连接是 ASK 指定的节点
                askConnection.set(this.connectionHandler.getConnectionFromNode(jre.getTargetNode()));
            } else if (jre instanceof JedisMovedDataException) {
            } else {
                throw new JedisClusterException(jre);
            }
            // 递归重试，重试次数减一
            return runWithRetries(key, attempts - 1);
        } finally {
            releaseConnection(connection);
        }
    }

***********************************************************************************************************
 - Jedis持有Pool对象，在close时，如果Pool不为空则，释放会缓存池中
    protected Pool<Jedis> dataSource = null;
 - JedisPoolConfig extends GenericObjectPoolConfig 用于配置JedisPool连接池
 - JedisPool extends Pool<Jedis>, Pool 持有commonpool2的GenericObjectPool, 用于缓存Jedis连接
    protected GenericObjectPool<T> internalPool;
 - JedisFactory implements PooledObjectFactory<Jedis>, 实现了makeObject用于新建Jedis连接
    public PooledObject<Jedis> makeObject() throws Exception {
        final Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort(), connectionTimeout, soTimeout);
        try {
            jedis.connect();
            if (null != this.password) {
                jedis.auth(this.password);
            }
            if (database != 0) {
                jedis.select(database);
            }
            if (clientName != null) {
                jedis.clientSetname(clientName);
            }
        } catch (JedisException je) {
            jedis.close();
            throw je;
        }
        return new DefaultPooledObject<Jedis>(jedis);
    }
    // 从pool中取出时进行初始化，重置到默认db上
    public void activateObject(PooledObject<Jedis> pooledJedis) throws Exception {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.getDB() != database) {
            jedis.select(database);
        }
    }
    // 从pool中取出时进行ping/pong校验
    public boolean validateObject(PooledObject<Jedis> pooledJedis) { pooledJedis.getObject().ping().equals("PONG") }
    // 销毁Jedis
    public void destroyObject(PooledObject<Jedis> pooledJedis) {
        final BinaryJedis jedis = pooledJedis.getObject();
        if (jedis.isConnected()) {
            try {
                try {
                    jedis.quit();
                } catch (Exception e) { }
                jedis.disconnect();
            } catch (Exception e) {
        }
    }
    
 * 
 * @author chenzq  
 * @date 2019年4月24日 下午10:48:29
 * @version V1.0
 * @Copyright: Copyright(c) 2019 jaesonchen.com Inc. All rights reserved. 
 */
package com.asiainfo.redis;
