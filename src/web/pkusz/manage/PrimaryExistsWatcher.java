package web.pkusz.manage;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by nick on 2017/8/12.
 */
public class PrimaryExistsWatcher implements Watcher {

    static final int PRIMARY_EXIST = 0;
    static final int PRIMARY_NONE = 1;

    Semaphore sem = new Semaphore(0);
    private volatile int state = PRIMARY_EXIST;

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            state = PRIMARY_NONE;
        }
        sem.release();
    }

    int getState() {
        return state;
    }

    void reset() {
        state = PRIMARY_EXIST;
    }
}
