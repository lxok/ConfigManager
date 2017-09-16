package web.pkusz.random;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by nick on 2017/7/22.
 */
public class Test {
    Semaphore sem = new Semaphore(1);

    public static void main(String[] args) throws Exception {
        Test t = new Test();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(5000);
                        t.sem.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        while (true) {
            t.sem.acquire();
            System.out.println("get through wait");
        }
    }

    static class comp implements Comparator<Integer> {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    }
}