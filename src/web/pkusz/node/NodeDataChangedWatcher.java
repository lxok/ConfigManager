package web.pkusz.node;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.Semaphore;

/**
 * Created by nick on 2017/9/6.
 */
public class NodeDataChangedWatcher implements Watcher{
    Semaphore sem = new Semaphore(0);

    @Override
    public void process(WatchedEvent event) {
        sem.release();
    }
}
