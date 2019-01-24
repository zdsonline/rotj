package com.zds.thread;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * @Author zhangds
 * @Date 2019/1/24 11:09
 * @Description TODO
 **/
public class ThreadCyclicBarrier {

    public static void main(String[] args) {

        startGameWhenAllReady();
    }

    private static void startGameWhenAllReady() {
        int runner = 5;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(runner);
        for (char c = 'A'; c <= 'E'; c++) {
            GamePlayer gp = new GamePlayer(cyclicBarrier, String.valueOf(c));
            new Thread(gp).start();
        }
    }
}

class GamePlayer implements Runnable {

    CyclicBarrier cyclicBarrier;

    String gamePlayerName;

    public GamePlayer(CyclicBarrier cyclicBarrier, String gamePlayerName) {
        this.cyclicBarrier = cyclicBarrier;
        this.gamePlayerName = gamePlayerName;
    }

    @Override
    public void run() {
        Random random = new Random();
        long prepareTime = random.nextInt(5000) + 1000;
        System.out.println(gamePlayerName + "开始准备！准备时间为："+prepareTime);
        try {
            Thread.sleep(prepareTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println(gamePlayerName + "准备完毕，等待其他人！");
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println(gamePlayerName + "开局");
    }
}
