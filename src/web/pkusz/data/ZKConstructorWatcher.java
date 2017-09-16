package web.pkusz.data;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * Created by nick on 2017/7/29.
 */
public class ZKConstructorWatcher implements Watcher {

    static final int CONNECT_FAIL = 0;
    static final int CONNECT_SUCC = 1;

    CountDownLatch connectedSemaphore = new CountDownLatch(1);
    private volatile int connectedState = CONNECT_FAIL;

    @Override
    public void process(WatchedEvent event) {
        if (event.getState() == Event.KeeperState.SyncConnected) {
            connectedState = CONNECT_SUCC;
        } else {
            connectedState = CONNECT_FAIL;
        }
        connectedSemaphore.countDown();
    }

    int getState() {
        return connectedState;
    }
}
