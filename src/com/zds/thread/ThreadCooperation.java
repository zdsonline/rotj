package com.zds.thread;

/**
 * @Author zhangds
 * @Date 2019/1/22 21:35
 * @Description 线程之间的协作
 */
public class ThreadCooperation {
    static Object lock = new Object();

    public static void main(String[] args) throws InterruptedException{
        MakeNoodless mn = new MakeNoodless();
        EatNoodles en = new EatNoodles();
        new Thread(en).start();
        Thread.sleep(200);//这里是为了确保先执行wait
        new Thread(mn).start();
//        Condition
    }
}

class MakeNoodless implements Runnable {

    @Override
    public void run() {
        synchronized (ThreadCooperation.lock) {
            try {
                System.out.println("开始做面！");
                Thread.sleep(2000);
                ThreadCooperation.lock.notify();
                System.out.println("面做好了，端出去吧！");
            } catch (InterruptedException ie) {
                System.out.println("可能是没面条了，去买！");
            }
        }

    }
}

class EatNoodles implements Runnable {
    @Override
    public void run() {
        synchronized (ThreadCooperation.lock) {
            try {
                System.out.println("等待吃面！");
                ThreadCooperation.lock.wait();
                System.out.println("面做好了，送过来了！");
                System.out.println("开始吃面");
                Thread.sleep(1000);
                System.out.println("吃完回家");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
