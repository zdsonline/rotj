package com.zds.loadBalance;

/**
 * @Author zhangds
 * @Date 2019/1/25 10:24
 * @Description TODO
 **/
public class Server {

    private String ip;

    private int weight;

    private int active;


    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
