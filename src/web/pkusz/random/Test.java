package web.pkusz.random;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by nick on 2017/7/22.
 */
public class Test {
    public static void main(String[] args) {
        RandomGenerator rg = new RandomGenerator();
        rg.setRandomTarget(new TestTargetSerial());
        List<String> res = rg.getNext(50);
        for (int i = 0; i < res.size(); i++) {
            System.out.println(res.get(i));
        }
    }
}