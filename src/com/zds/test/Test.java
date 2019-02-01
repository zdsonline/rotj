package com.zds.test;

/**
 * @Author zhangds
 * @Date 2019/2/1 10:04
 * @Description TODO
 **/
public class Test {

    public static void main(String[] args) {
        test1();
    }

    public static void test1() {
        int[] arr = new int[5];
        arr[0] = 1;
        arr[0] = 2;
        arr[0] = 1;
        arr[0] = 2;
        arr[0] = 5;

        int a = 0;//arr[0] ^ arr[1] ^ arr[2] ^ arr[3] ^ arr[4];
        for (int i = 1; i < arr.length; i++) {
            if (i == 1) {
                a = arr[i - 1] ^ arr[i];
            } else {
                a = a ^ arr[i];
            }
        }
        System.out.println(a);

    }

}
