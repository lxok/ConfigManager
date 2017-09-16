package web.pkusz.node;

import web.pkusz.data.DatabaseZK;

import java.io.*;
import java.util.Properties;

/**
 * Created by nick on 2017/9/6.
 */
public class ConfigNode {
    public static final String VERSION = "1.0";

    public static final String ROOT = "/mimic";
    public static final String NODE = ROOT + "/node";
    public static final String NODE_EPHE = NODE + "/ephemeral";
    public static final String NODE_PERS = NODE + "/persistent";

    public static final int WAIT_PERS_NODE_TIME = 5000;

    private static final String PROP = "configmanagernode.properties";

    private Properties prop;
    private String id;
    private DatabaseZK db;
    private String address;
    private String localIP;

    private volatile boolean running = true;
    private volatile boolean dbInit = false;

    public ConfigNode() {
    }

    public static void main(String[] args) throws Exception {
        ConfigNode cn = new ConfigNode();
        cn.start();
    }

    private boolean init() {
        FileInputStream is;
        try {
            is = new FileInputStream(PROP);
        } catch (FileNotFoundException e) {
            //log
            System.out.println("System config file is not found(configmanagernode.properties).");
            System.out.println(e.toString());
            return false;
        }
        prop = new Properties();
        try {
            prop.load(is);
        } catch (IOException e) {
            //log
            System.out.println("Load node config file(configmanagernode.properties) failed.");
            System.out.println(e.toString());
            return false;
        }
        id = prop.getProperty("id");
        if (id == null) {
            //log
            System.out.println("System config file doesn't contain the manager id(id).");
            return false;
        }
        address = id;
        db = new DatabaseZK(prop);
        if (!db.connect()) {
            return false;
        }
        return true;
    }

    public void start() throws Exception {
        if (!init()) {
            return;
        }
        String nodePathE = NODE_EPHE + "/" + id;
        if (!db.createEphemeral(nodePathE, "".getBytes())) {
            return;
        }
        dbInit = true;

        String nodePathP = NODE_PERS + "/" + id;
        NodeDataChangedWatcher watcher = new NodeDataChangedWatcher();
        NodeValue nv = new NodeValue();
        while (running) {
            byte[] v = db.getData(nodePathP, watcher, null);
            if (v == null) {
                Thread.sleep(WAIT_PERS_NODE_TIME);
                continue;
            } else {
                nv.deserialize(v);
                if (localIP == null || !localIP.equals(nv.ip)) {
                    changeLocalIP(nv.ip);
                }
            }
            watcher.sem.acquire();
        }
    }

    private void changeLocalIP(String ip) {
        System.out.println("change ip:" + ip);
        //TO DO
        //exec the script of changing local ip.
        //if true, set this.localIP = ip;
        //if false, return;
    }
}
