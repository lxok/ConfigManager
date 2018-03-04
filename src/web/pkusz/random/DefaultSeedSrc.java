package web.pkusz.random;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by nick on 2017/7/22.
 */
/**
 DefaultSeedSrc类是RandGenerator类随机数发生器的默认种源生成器实现。
 */
public class DefaultSeedSrc implements SeedSrcHandler{
    @Override
    public List<String> getSrc(int maxNum) {
        maxNum = 1;
        List<String> res = new ArrayList<>(maxNum);
        res.add(new Date().toString());
        return res;
    }
}
