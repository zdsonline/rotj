package com.zds.loadBalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Author zhangds
 * @Date 2019/1/29 22:33
 * @Description 一致性hash算法
 * 为了简化，不考虑参数。
 **/
public class ConsistentHash {

    private final int replicaNumber;

    //圆环
    private final TreeMap<Long, Server> circle = new TreeMap<>();


    public ConsistentHash(int numberOfReplicas, List<Server> servers) {
        this.replicaNumber = numberOfReplicas;///固定传160
        for (Server server : servers) {
            String ip = server.getIp();
            //这里的目的是为每个服务器创建160个虚拟节点，并且尽可能的让这160个虚拟节点分散在圆环上，
            for (int i = 0; i < replicaNumber / 4; i++) {
                // 对 ip + i 进行 md5 运算，得到一个长度为16的字节数组
                byte[] digest = md5(ip + i);
                // 对 digest 部分字节进行4次 hash 运算，得到四个不同的 long 型正整数
                for (int h = 0; h < 4; h++) {
                    // h = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
                    // h = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
                    // h = 2, h = 3 时过程同上
                    long m = hash(digest, h);
                    // 将 hash 到 server 的映射关系存储到 circle 中，
                    // circle 需要提供高效的查询操作，因此选用 TreeMap 作为存储结构
                    circle.put(m, server);
                }
            }
        }
    }


    public Server select(String method) {
        // 对参数 key 进行 md5 运算
        String key = method;
        byte[] digest = md5(key);
        // 取 digest 数组的前四个字节进行 hash 运算，再将 hash 值传给 selectForKey 方法，
        // 寻找合适的 Invoker
        return selectForKey(hash(digest, 0));
    }

    private Server selectForKey(long hash) {
        // 到 TreeMap 中查找第一个节点值大于或等于当前 hash 的 Invoker
        Map.Entry<Long, Server> entry = circle.tailMap(hash, true).firstEntry();
        // 如果 hash 大于 Invoker 在圆环上最大的位置，此时 entry = null，
        // 需要将 TreeMap 的头结点赋值给 entry
        if (entry == null) {
            entry = circle.firstEntry();
        }
        // 返回 server
        return entry.getValue();
    }


    private long hash(byte[] digest, int number) {
        return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                | (digest[number * 4] & 0xFF))
                & 0xFFFFFFFFL;
    }

    private byte[] md5(String value) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        md5.reset();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        md5.update(bytes);
        return md5.digest();
    }
}
