/**  
 *  Redis:
    Redis是一个开源的内存中的数据结构存储系统，它可以用作：数据库、缓存和消息中间件。
    Redis 内置了复制（Replication），LUA脚本（Lua scripting）， LRU驱动事件（LRU eviction），事务（Transactions） 和不同级别的磁盘持久化（Persistence），
    并通过 Redis哨兵（Sentinel）和自动分区（Cluster）提供高可用性（High Availability）。
    Redis本质上是一个Key-Value类型的内存数据库，整个数据库统统加载在内存当中进行操作，定期通过异步操作把数据库数据flush到硬盘上进行保存。
    因为是纯内存操作，Redis的性能非常出色，每秒可以处理超过 10万次读写操作，是已知性能最快的Key-Value DB。
    Redis支持保存多种数据结构（String、List、Set、Sorted Set、Hash），此外单个value的最大限制是1GB，可以对存入的Key-Value设置expire时间。
    Redis的主要缺点是数据库容量受到物理内存的限制，不能用作海量数据的高性能读写，因此Redis适合的场景主要局限在较小数据量的高性能操作和运算上。
    
    Redis为什么这么快：
    1、完全基于内存
    2、数据结构简单
    3、采用单线程，避免了不必要的上下文切换和竞争条件
    4、使用多路I/O复用模型，非阻塞IO；
    5、单线程的方式是无法发挥多核CPU 性能，不过我们可以通过在单机开多个Redis 实例来完善！
    
    Redis的优点：
    1、速度快，因为数据存在内存中，类似于HashMap，HashMap的优势就是查找和操作的时间复杂度都是O(1)
    2、支持丰富数据类型，支持string，list，set，sorted set，hash
    3、支持事务，操作都是原子性，所谓的原子性就是对数据的更改要么全部执行，要么全部不执行
    4、丰富的特性：可用于缓存，消息，按key设置过期时间，过期后将会自动删除
    5、Redis支持数据的备份，即master-slave模式的数据备份。
    6、Redis支持数据的持久化，可以将内存中的数据保持在磁盘中，重启的时候可以再次加载进行使用。
    
    适用场景：
    会话缓存（Session Cache）：最常用的一种使用Redis的情景是会话缓存（session cache）。
      用Redis缓存会话比其他存储（如Memcached）的优势在于：Redis提供持久化。Redis并不能保证数据的强一致性，适用于维护一个不是严格要求一致性的缓存。
    队列：Reids在内存存储引擎领域的一大优点是提供 list 和 set 操作，这使得Redis能作为一个很好的消息队列平台来使用。
    排行榜/计数器：Redis在内存中对数字进行递增或递减的操作实现的非常好。集合（Set）和有序集合（Sorted Set）也使得我们在执行这些操作的时候变的非常简单。
    发布/订阅: 使用较少
    
    Redis分布式锁：
    先拿setnx来争抢锁，抢到之后，再用expire给锁加一个过期时间防止锁忘记了释放。
    如果在setnx之后执行expire之前进程意外crash或者要重启维护了，那会怎么样？
    set指令有非常复杂的参数，这个应该是可以同时把setnx和expire合成一条指令来用的！
    
    Redis秒杀：
    incrby key -1 返回结果大于等于0算拍到。
    
    Redis持久化：
    持久化就是把内存的数据写到磁盘中去，防止服务宕机了内存数据丢失。
    Redis 提供了两种持久化方式:RDB（默认） 和AOF 。
    RDB：Redis DataBase，功能核心函数rdbSave(生成RDB文件)和rdbLoad（从文件加载内存）。
    AOF：redis 写命令日志。
    1、aof文件比rdb更新频率高，优先使用aof还原数据。
    2、aof比rdb更安全也更大
    3、rdb性能比aof好
    4、如果两个都配了优先加载AOF
    
    Redis哈希槽：
    Redis集群没有使用一致性hash,而是引入了哈希槽的概念，Redis集群有16384个哈希槽，每个key通过CRC16校验后对16384取模来决定放置哪个槽，集群的每个节点负责一部分hash槽。
    
    Redis事务：
    事务是一个单独的隔离操作：事务中的所有命令都会序列化、按顺序地执行。事务在执行的过程中，不会被其他客户端发送来的命令请求所打断。
    事务是一个原子操作，事务中的命令要么全部被执行，要么全部都不执行。


    Redis cluster
    1. 所有的redis节点彼此互联(PING-PONG机制), 内部使用二进制协议优化传输速度和带宽.
    2. 节点的fail是通过集群中超过半数的master节点检测失效时才生效.
    3. 客户端与redis节点直连, 不需要中间proxy层; 客户端不需要连接集群所有节点, 连接集群中任何一个可用节点即可
    4. redis-cluster把所有的物理节点映射到[0-16383]slot上, cluster 负责维护node<->slot<->key
    
    Redis cluster 选举
    1. 选举过程是集群中所有master参与, 如果半数以上master节点与故障节点通信超过(cluster-node-timeout), 认为该节点故障, 自动触发故障转移操作.
    2. 什么时候整个集群不可用(cluster_state:fail)? 
       a. 如果集群任意master挂掉, 且当前master没有slave. 集群进入fail状态, 也可以理解成集群的slot映射[0-16383]不完整时进入fail状态.
       b. 如果集群超过半数以上master挂掉，无论是否有slave集群进入fail状态.
    ps: 当集群不可用时, 所有对集群的操作做都不可用，收到((error) CLUSTERDOWN The cluster is down)错误.

************************************************************************************************************************
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
