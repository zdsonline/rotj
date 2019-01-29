package com.zds.loadBalance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author zhangds
 * @Date 2019/1/24 22:44
 * @Description TODO负载均衡算法
 **/
public class LoadBalanceImpl {


    static Map<String, Integer> serverMap = new HashMap<String, Integer>();
    //服务器列表
    static List<Server> servers = new ArrayList<>();

    static List<String> requestUrl = Arrays.asList("/index", "/order/list", "/user/info", "/order/info", "/user/list");

    static {
        for (int i = 1; i < 4; i++) {
            Server s = new Server();
            s.setIp("192.168.1." + i);
            s.setWeight(i);
            s.setActive(new Random().nextInt(10));
            servers.add(s);
            serverMap.put(s.getIp(), 0);
        }

    }

    public static void main(String[] args) {

        ConsistentHash consistentHash = new ConsistentHash(160,servers);

        for (int i = 0; i < 100; i++) {
            String method = requestUrl.get(new Random().nextInt(requestUrl.size()-1));
            Server server = consistentHash.select(method);
            System.out.println("方法："+method+"，节点："+server.getIp());
        }

//
//        for (int i = 0; i < 10; i++) {
//            Server s = randomLoadBalance();
//            serverMap.put(s.getIp(), serverMap.get(s.getIp()) + 1);
//        }
//        serverMap.forEach((s, integer) -> {
//            System.out.println(s + "--命中次数：" + integer);
//        });
    }

    /**
     * 加权随机
     *
     * @return
     */
    public static Server randomLoadBalance() {
        int totalWeights = 0;
        for (Server server : servers) {
            totalWeights += server.getWeight();
        }
        Random random = new Random();
        int result = random.nextInt(totalWeights);
        for (Server server : servers) {
            result -= server.getWeight();
            if (result < 0) {
                return server;
            }
        }
        return null;
    }

    /**
     * 最小活跃数
     * 这里并没有活跃数的维护，所以只是做算法演示,运行结果并没有什么意义
     *
     * @return
     */
    public static Server leastActiveLoadBalance() {
        int leastActive = -1;///最小活跃数
        int leastCount = 0;//与最小活跃数相同活跃数的服务数量
        int totalWeight = 0;//与最小活跃数相同活跃数的服务的总权重
        boolean sameWeight = false;///是否有权重相同的服务
        int firstWeight = -1;//
        int[] leastIndexs = new int[servers.size()];
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            int active = server.getActive();
            int weight = server.getWeight();
            if (leastActive == -1 || active < leastActive) {
                leastActive = active;
                leastCount = 1;
                leastIndexs[0] = i;
                totalWeight = weight;
                firstWeight = weight;
                sameWeight = true;
            } else if (active == leastActive) {
                leastIndexs[leastCount++] = i;
                totalWeight += weight;
                if (sameWeight && i > 0
                        && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        if (leastCount == 1) {
            return servers.get(leastIndexs[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            //这里与加权随机相同[1,2,3,4,5]
            ///////////////////2,3
            //////////////////2
            Random random = new Random();
            int offsetWeight = random.nextInt(totalWeight);
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexs[i];
                offsetWeight -= servers.get(leastIndex).getWeight();
                if (offsetWeight < 0)
                    return servers.get(leastIndex);
            }
        }
        return null;
    }

    /**
     * 一致性哈希
     *
     * @return
     */
    public static Server consistentHashLoadBalance(String url) {
        int length = servers.size();
        int hashcode = url.hashCode();
        int seq = hashcode % length;
        seq = Math.abs(seq);

        return servers.get(seq);
    }


    // 最外层为服务类名 + 方法名，第二层为 url 到 WeightedRoundRobin 的映射关系。
    // 这里我们可以将 url 看成是服务提供者的 id
    private static ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<String, ConcurrentMap<String, WeightedRoundRobin>>();

    private static AtomicBoolean updateLock = new AtomicBoolean();

    private static int RECYCLE_PERIOD = 60000;


    /**
     * 加权轮询
     *
     * @return
     */
    public static Server roundRobinLoadBalance() {

        String key = servers.get(0).getIp();
        // 获取 url 到 WeightedRoundRobin 映射表，如果为空，则创建一个新的
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map == null) {
            methodWeightMap.putIfAbsent(key, new ConcurrentHashMap<String, WeightedRoundRobin>());
            map = methodWeightMap.get(key);
        }
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;

        // 获取当前时间
        long now = System.currentTimeMillis();
        Server selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;

        // 下面这个循环主要做了这样几件事情：
        //   1. 遍历 Invoker 列表，检测当前 Invoker 是否有
        //      相应的 WeightedRoundRobin，没有则创建
        //   2. 检测 Invoker 权重是否发生了变化，若变化了，
        //      则更新 WeightedRoundRobin 的 weight 字段
        //   3. 让 current 字段加上自身权重，等价于 current += weight
        //   4. 设置 lastUpdate 字段，即 lastUpdate = now
        //   5. 寻找具有最大 current 的 Invoker，以及 Invoker 对应的 WeightedRoundRobin，
        //      暂存起来，留作后用
        //   6. 计算权重总和
        for (Server server : servers) {
            String identifyString = System.identityHashCode(server.getIp()) + "";
            WeightedRoundRobin weightedRoundRobin = map.get(identifyString);
            int weight = server.getWeight();
            if (weight < 0) {
                weight = 0;
            }

            // 检测当前 Invoker 是否有对应的 WeightedRoundRobin，没有则创建
            if (weightedRoundRobin == null) {
                weightedRoundRobin = new WeightedRoundRobin();
                // 设置 Invoker 权重
                weightedRoundRobin.setWeight(weight);
                // 存储 url 唯一标识 identifyString 到 weightedRoundRobin 的映射关系
                map.putIfAbsent(identifyString, weightedRoundRobin);
                weightedRoundRobin = map.get(identifyString);
            }
            // Invoker 权重不等于 WeightedRoundRobin 中保存的权重，说明权重变化了，此时进行更新
            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);
            }

            // 让 current 加上自身权重，等价于 current += weight
            long cur = weightedRoundRobin.increaseCurrent();
            // 设置 lastUpdate，表示近期更新过
            weightedRoundRobin.setLastUpdate(now);
            // 找出最大的 current
            if (cur > maxCurrent) {
                maxCurrent = cur;
                // 将具有最大 current 权重的 Invoker 赋值给 selectedInvoker
                selectedInvoker = server;
                // 将 Invoker 对应的 weightedRoundRobin 赋值给 selectedWRR，留作后用
                selectedWRR = weightedRoundRobin;
            }

            // 计算权重总和
            totalWeight += weight;
        }

        // 对 <identifyString, WeightedRoundRobin> 进行检查，过滤掉长时间未被更新的节点。
        // 该节点可能挂了，invokers 中不包含该节点，所以该节点的 lastUpdate 长时间无法被更新。
        // 若未更新时长超过阈值后，就会被移除掉，默认阈值为60秒。
        if (!updateLock.get() && servers.size() != map.size()) {
            if (updateLock.compareAndSet(false, true)) {
                try {
                    ConcurrentMap<String, WeightedRoundRobin> newMap = new ConcurrentHashMap<String, WeightedRoundRobin>();
                    // 拷贝
                    newMap.putAll(map);

                    // 遍历修改，即移除过期记录
                    Iterator<Map.Entry<String, WeightedRoundRobin>> it = newMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, WeightedRoundRobin> item = it.next();
                        if (now - item.getValue().getLastUpdate() > RECYCLE_PERIOD) {
                            it.remove();
                        }
                    }

                    // 更新引用
                    methodWeightMap.put(key, newMap);
                } finally {
                    updateLock.set(false);
                }
            }
        }

        if (selectedInvoker != null) {
            // 让 current 减去权重总和，等价于 current -= totalWeight
            selectedWRR.sel(totalWeight);
            // 返回具有最大 current 的 Invoker
            return selectedInvoker;
        }

        // should not happen here
        return servers.get(0);
    }

    protected static class WeightedRoundRobin {
        // 服务提供者权重
        private int weight;
        // 当前权重
        private AtomicLong current = new AtomicLong(0);
        // 最后一次更新时间
        private long lastUpdate;

        public void setWeight(int weight) {
            this.weight = weight;
            // 初始情况下，current = 0
            current.set(0);
        }

        public long increaseCurrent() {
            // current = current + weight；
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            // current = current - total;
            current.addAndGet(-1 * total);
        }

        public int getWeight() {
            return weight;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }
    }
}
