package web.pkusz.random;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.*;

/**
 * Created by nick on 2017/7/16.
 */
/**
 RandomGenerator类是CM系统中的随机数发生器，负责产生随机数序列。
 CM所属的存储系统对随机的要求比jdk类库中的随机数发生器要求更高，因此这里对jdk中的随机数发生器做了进一步封装和增加了其它逻辑。
 原生的随机数发生器在随机种子确定的情况下，会产生确定的数字序列。在本类中，将在一定期限内更换随机种子，能够有效避免对随机序列的
 预测。
 RandomGenerator类考虑到了通常对于随机序列的使用会进行一定的结果封装，如需要获取ip值192.168.0.x，其中x为产生的随机数，这时当产
 生了随机数x后，需要有机制将随机数扩展为用户能够直接使用的序列，如将x拓展为192.168.0.x。因此，在该类中引入了目标序列转换框架，
 用户可以根据实际需求编写转换逻辑，需要新编写的类继承RandomTargetSerialHandler序列，并将其实例注册到随机数发生器中。

 在RandomGenerator类中，采用了策略模式对随机种子源和目标序列进行抽象，它们可以被用户拓展成需要的逻辑。随机种子源的接口为SeedSrcHandler,
 目标序列转换器的抽象接口为RandomTargetSerialHandler，在使用时，将拓展出的对象注册到随机数发生中，则随机数发生器会根据拓展类的
 逻辑获取种源和生成目标序列。
 将种子源拓展的目的是在随机数发生器中，种子决定了随机数发生器产生的确定随机序列，种子的不可预测性和随机性可以带来随机序列的不确
 定性变化，随机数发生器会在一定期限内更换种子，这样能够尽可能做到产生序列的随机性。默认种源实现类为DefaultSeedSrc。
 默认目标序列转换器的实现类为DefaultTargetSerial。
 */
public class RandomGenerator {
    private static final int DEFAULT_SEED_SRC_NUM = 1;
    private static final int DEFAULT_COUNT_THRESHOLD = 100;
    private static final int DEFAULT_MAX_INTERVAL = 24 * 60 * 60; //second

    private SeedSrcHandler seedSrc;
    private RandomTargetSerialHandler randomTarget;
    private int seedSrcNum = DEFAULT_SEED_SRC_NUM;
    private long seed;
    private Random r;
    private int countThreshold = DEFAULT_COUNT_THRESHOLD;
    private long maxInterval = DEFAULT_MAX_INTERVAL;
    private int count;
    private Date modifySeedTime;
    private List<String> targetCurrent;
    private long targetRangeMin;
    private long targetRangeMax;

    public RandomGenerator() {
        setSeedSrc(new DefaultSeedSrc());
        setRandomTarget(new DefaultTargetSerial());
        r = new Random();
        modifyCurSeed();
    }

    public void setSeedSrc(SeedSrcHandler handler) {
        seedSrc = handler;
    }

    public void setSeedSrcNum(int num) {
        this.seedSrcNum = num;
    }

    public void setRandomTarget(RandomTargetSerialHandler handler) {
        randomTarget = handler;
        targetRangeMin = randomTarget.getRangeMin();
        targetRangeMax = randomTarget.getRangeMax();
    }

    public List<String> getNext(int num) {
        if (num <= 0) {
            return null;
        }
        if (!verifyCurSeed()) {
            modifyCurSeed();
        }
        List<String> res = new ArrayList<>(num);
        long random;
        for (int i = 0; i < num; i++) {
            random = getNextRandomRange();
            res.add(randomTarget.getNext(random));
        }
        targetCurrent = res;
        return res;
    }

    private boolean verifyCurSeed() {
        if (count >= countThreshold) {
            return false;
        }
        Date now = new Date();
        long diff = (now.getTime() - modifySeedTime.getTime()) / 1000;
        return diff < maxInterval;
    }

    private void modifyCurSeed() {
        List<String> src = seedSrc.getSrc(seedSrcNum);
        if (src == null || src.size() > seedSrcNum) {
            return;
        }
        long s = genSeed(src);
        r.setSeed(s);
        seed = s;
        count = 0;
        modifySeedTime = new Date();
    }

    private long getNextRandomRange() {
        long c = targetRangeMax - targetRangeMin + 1;
        if (c < 0) {
            c = targetRangeMax / 2  - targetRangeMin + 1;
        }
        long res =  r.nextInt((int)c) + targetRangeMin;
        count++;
        return res;
    }

    private long genSeed(List<String> src) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            int[] arr = new int[src.size()];
            String text;
            for (int i = 0; i < arr.length; i++) {
                text = src.get(i);
                sha.update(text.getBytes());
                byte[] hash = sha.digest();
                byte[] split = new byte[4];
                for (int j = 0; j < 4; j++) {
                    split[j] = hash[j];
                }
                arr[i] = byteArrayToInt(split);
            }
            int res = 0;
            for (int i = 0; i < arr.length; i++) {
                res ^= arr[i];
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int byteArrayToInt(byte[] arr) {
        int b0 = arr[0] & 0xff;
        int b1 = arr[1] & 0xff;
        int b2 = arr[2] & 0xff;
        int b3 = arr[3] & 0xff;
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    public static void main(String[] args) throws Exception {
        RandomGenerator r = new RandomGenerator();
        int flag = 32 * 1024;
        FileOutputStream fo = new FileOutputStream("C:\\Users\\nick\\Desktop\\32k.txt");
        PrintWriter pw = new PrintWriter(fo);
        List<String> res = r.getNext(flag);
        List<Integer> res2 = new ArrayList<>(flag);
        for (int i = 0; i < res.size(); i++) {
            res2.add(Integer.parseInt(res.get(i)));
        }
        Collections.sort(res2);
        String line = "[" + 0 + " : " + res2.get(0) + "]";
        pw.println(line);
        for (int i = 1; i < res2.size(); i++) {
            int a = res2.get(i - 1) + 1;
            line =  "[" + a + " : " + res2.get(i) + "]";
            pw.println(line);
        }
        pw.flush();
    }
}
