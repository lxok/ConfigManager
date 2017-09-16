package web.pkusz.data;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by nick on 2017/7/29.
 */
public class DatabaseZK implements Database {

    private static final String ADDR = "addr";

    private static final int SESSION_TIMEOUT = 5000; //ms
    private static final long CONNECT_WAIT_TIMEOUT = 10; //s

    Properties prop;
    ZooKeeper zk;

    public DatabaseZK(Properties prop) {
        this.prop = prop;
    }

    public boolean connect() {
        String addr = prop.getProperty(ADDR);
        if (addr == null) {
            //log
            System.out.println("System config file doesn't contain the server address property(addr).");
            return false;
        }
        Watcher connWatcher = new ZKConstructorWatcher();
        ZKConstructorWatcher zkWatcher = (ZKConstructorWatcher) connWatcher;
        try {
            zk = new ZooKeeper(addr, SESSION_TIMEOUT, connWatcher);
            zkWatcher.connectedSemaphore.await(CONNECT_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            processConnectFail(addr);
            System.out.println(e.toString());
            return false;
        }
        if (zkWatcher.getState() == ZKConstructorWatcher.CONNECT_FAIL) {
            processConnectFail(addr);
            return false;
        }
        return true;
    }

    public boolean create(String path, byte[] value) {
        return create(path, value, CreateMode.PERSISTENT);
    }

    public boolean createEphemeral(String path, byte[] value) {
        return create(path, value, CreateMode.EPHEMERAL);
    }

    private boolean create(String path, byte[] value, CreateMode createMode) {
        try {
            zk.create(path, value, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
        } catch (Exception e) {
            //log
            System.out.println("Create path failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    public byte[] getData(String path, Stat stat) {
        return getData(path, null, stat);
    }

    public byte[] getData(String path, Watcher watcher, Stat stat) {
        byte[] value;
        try {
            value = zk.getData(path, watcher, stat);
            if (value == null) {
                throw new IOException();
            }
        } catch (Exception e) {
            //log
            System.out.println("Get data failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return null;
        }
        return value;
    }

    public Stat setData(String path, byte[] value, int version) {
        Stat s;
        try {
            s = zk.setData(path, value, version);
            if (s == null) {
                throw new IOException();
            }
        } catch (Exception e) {
            //log
            System.out.println("Set data failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return null;
        }
        return s;
    }

    public boolean delete(String path, int version) {
        try {
            zk.delete(path, version);
        } catch (Exception e) {
            //log
            System.out.println("Delete path failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    public List<String> getChildren(String path) {
        return getChildren(path, null);
    }

    public List<String> getChildren(String path, Watcher watcher) {
        List<String> res;
        try {
            res = zk.getChildren(path, watcher);
            if (res == null) {
                throw new IOException();
            }
        } catch (Exception e) {
            //log
            System.out.println("Get children failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return null;
        }
        return res;
    }
    
    public Stat exists(String path) {
        Stat res;
        try {
            res = zk.exists(path, false);
            if (res == null) {
                System.out.println("Path is not existed. path:" + path);
            }
        } catch (Exception e) {
            //log
            System.out.println("Exists failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return null;
        }
        return res;
    }

    public Stat exists(String path, Watcher watcher) {
        Stat res;
        try {
            res = zk.exists(path, watcher);
            if (res == null) {
                System.out.println("Path is not existed. path:" + path);
            }
        } catch (Exception e) {
            //log
            System.out.println("Exists failed. (Path:" + path + ")");
            System.out.println(e.toString());
            return null;
        }
        return res;
    }

    private void processConnectFail(String addr) {
        //log
        System.out.println("Connect zk failed. Server:" + addr);
    }
}
