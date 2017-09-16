package web.pkusz.random;

import java.util.List;

/**
 * Created by nick on 2017/7/16.
 */
public interface SeedSrcHandler {
    List<String> getSrc(int maxNum);
}

