package web.pkusz.manage;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * Created by nick on 2017/9/12.
 */
public class NodeEpheWatcher implements Watcher{

    CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void process(WatchedEvent event) {
        latch.countDown();
    }
}
