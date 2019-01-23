package com.zds.thread;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author zhangds
 * @Date 2019/1/23 15:21
 * @Description TODO
 **/
public class ThreadCooperationCondition {

    private Lock lock = new ReentrantLock();
    private Condition noodles = lock.newCondition();

    public static void main(String[] args) throws InterruptedException {
        ThreadCooperationCondition threadCooperationCondition = new ThreadCooperationCondition();
        MakeNoodles make = threadCooperationCondition.new MakeNoodles();
        EatNoodles eat = threadCooperationCondition.new EatNoodles();

        new Thread(eat).start();
        Thread.sleep(200);
        new Thread(make).start();

    }

    class EatNoodles implements Runnable {

        @Override
        public void run() {
            eat();
        }

        private void eat() {
            lock.lock();
            try {
                System.out.println("等待吃面！");
                noodles.await();
                System.out.println("面做好了，送过来了！");
                System.out.println("开始吃面");
                Thread.sleep(1000);
                System.out.println("吃完回家");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

        }
    }

    class MakeNoodles implements Runnable {

        @Override
        public void run() {
            make();
        }

        private void make() {
            lock.lock();
            try {

                System.out.println("开始做面！");
                Thread.sleep(2000);
                noodles.signal();
                System.out.println("面做好了，端出去吧！");
            } catch (InterruptedException ie) {
            } finally {
                lock.unlock();
            }
        }
    }
}