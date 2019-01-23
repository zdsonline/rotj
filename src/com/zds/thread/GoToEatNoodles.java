package com.zds.thread;

/**
 * @Author zhangds
 * @Date 2019/1/22 22:12
 * @Description TODO
 **/
public class GoToEatNoodles {
    static boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("我来吃面了");
        MakeNoodles mn = new MakeNoodles();
        new Thread(mn).start();
        while (true) {
            if (flag) {
                System.out.println("可以吃面了");
                Thread.sleep(1000);
                break;
            }
            System.out.println("面还没好，等一会");
            Thread.sleep(500);
        }
        System.out.println("吃完回家！");
    }
}

class MakeNoodles implements Runnable {
    @Override
    public void run() {
        System.out.println("开始做面！");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            System.out.println("可能是没面条了，去买！");
        }
        System.out.println("面做好了！");
        GoToEatNoodles.flag = true;
    }
}
