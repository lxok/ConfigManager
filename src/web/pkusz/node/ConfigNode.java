package web.pkusz.node;

import web.pkusz.data.DatabaseZK;
import web.pkusz.node.log.OpCommitLog;
import web.pkusz.protocal.*;
import web.pkusz.serialize.Entry;
import web.pkusz.serialize.SerializeUtil;

import java.io.*;
import java.time.Instant;
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

    public static final int WAIT_PERS_NODE_TIME = 5000; //ms
    public static final long DEFAULT_NODE_SLEEPING_INTERVAL = 60; //s

    private static final String PROP = "configmanagernode.properties";
    public static final String COMMIT_LOG = "commitlog.log";

    private Properties prop;
    private int type = -1;
    private String id;
    private DatabaseZK db;
    private String address;
    private String localIP;
    private Instant changedTime;
    private OpCommitLog opCommitLog;
    private int currentState = 0;
    private long lastCommitNum;

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
        String typeStr = prop.getProperty("type");
        if (typeStr == null) {
            //log
            System.out.println("System config file doesn't contain the client type(type).");
            return false;
        }
        try {
            type = Integer.parseInt(typeStr);
        } catch (Exception e) {
            //log
            System.out.println("System config file parse client type(type) error.");
            return false;
        }
        if (type != NodeType.MOUNT_SERVER && type != NodeType.STORE_SERVER && type != NodeType.MANAGE_SERVER) {
            //log
            System.out.println("System config file parse client type(type) error.");
            return false;
        }
        db = new DatabaseZK(prop);
        if (!db.connect()) {
            return false;
        }
        opCommitLog = new OpCommitLog();
        OpCommitLog.Entry lastEntry = opCommitLog.readLatestEntry();
        if (lastEntry == null) {
            lastCommitNum = 0;
            currentState = 0;
        } else {
            lastCommitNum = lastEntry.getCommitOpNum();
            currentState = lastEntry.state();
        }
        return true;
    }

    public void start() throws Exception {
        if (!init()) {
            return;
        }
        String nodePathE = NODE_EPHE + "/" + id;
        byte[] nodePathEValue = SerializeUtil.getBytes(new NodeEpheConfig(type, lastCommitNum, currentState));
        if (!db.createEphemeral(nodePathE, nodePathEValue)) {
            return;
        }
        dbInit = true;

        String nodePathP = NODE_PERS + "/" + id;
        NodeDataChangedWatcher watcher = new NodeDataChangedWatcher();
        while (running) {
            byte[] v = db.getData(nodePathP, watcher, null);
            if (v == null) {
                Thread.sleep(WAIT_PERS_NODE_TIME);
                continue;
            } else {
                NodePersConfig nodePathPValue = new NodePersConfig();
                nodePathPValue.updateFromEntryList(SerializeUtil.getEntries(v));
                if (nodePathPValue.getOpNum() > lastCommitNum) {
                    if (nodePathPValue.getState() != currentState) {
                        transition(nodePathPValue);
                    }
                    //transition complete, change node pathE
                    completeTransition(nodePathPValue);
                }
            }
            watcher.sem.acquire();
        }
    }

    private void transition(NodePersConfig nodePersConfig) {
        switch (nodePersConfig.getState()) {
            case NodeState.READY: //ready
                break;
            case NodeState.ACTION: //action
                handleActionState();
                break;
            case NodeState.SLEEPING: //sleeping
                handleSleepingState(nodePersConfig);
                break;
            case NodeState.SHUTDOWN: //shutdown
                handleShutdownState();
                break;
            case NodeState.WAITING: //waiting
                handleWaitingState(nodePersConfig);
                break;
            default:
        }
    }

    private void handleActionState() {}

    //cut network in a Specified time, then reconnect to the network.
    private void handleSleepingState(NodePersConfig nodePersConfig) {
        String intervalStr = nodePersConfig.getAttribute("interval");
        Long interval;
        if (intervalStr == null) { // TO DO, set default sleep time.
            interval = DEFAULT_NODE_SLEEPING_INTERVAL;
        } else {
            try {
                interval = Long.parseLong(intervalStr);
            } catch (Exception e) {
                return;
            }
        }
        boolean hasClosed = execLocalNetworkClose();
        while (running) {
            try {
                Thread.sleep(interval * 1000);
                break;
            } catch (InterruptedException e) {
                continue;
            }
        }
        if (hasClosed) {
            execLocalNetworkOpen();
        }
    }

    //exec local script.
    //retry a fix num, if not success in a long time, return false.
    private boolean execLocalNetworkClose() {
        String device = getEthDeviceName();
        if (device == null || device.length() == 0) {
            System.out.println("can not get device.");
            return false;
        }
        String command = "ifdown " + device;
        try {
            BufferedReader br = runLocal(command);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.contains("successfully") || line.contains("成功") || line.length() == 0) {
                    br.close();
                    return true;
                }
            }
        } catch (Exception e) {
            //log
            System.out.println("exec <ifdown + DEVICE> in local failed.");
            System.out.println(e);
        }
        return false;
    }

    //exec local script.
    //must wait until network open
    private void execLocalNetworkOpen() {
        while (running) {
            String device = getEthDeviceName();
            if (device == null || device.length() == 0) {
                //log
                System.out.println("exec network open failed. Can not get DEVICE");
                try {
                    Thread.sleep(WAIT_PERS_NODE_TIME);
                    continue;
                } catch (InterruptedException e) {
                    continue;
                }
            }
            String command = "ifup " + device;
            try {
                BufferedReader br = runLocal(command);
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("successfully") || line.length() == 0) {
                        br.close();
                        return;
                    }
                }
                if (line == null) {
                    return;
                }
            } catch (Exception e) {
                //log
                System.out.println("exec <ifdown + DEVICE> in local failed.");
                System.out.println(e);
            }
        }
    }

    public String getEthDeviceName() {
        String command = "ip link";
        try {
            String line;
            BufferedReader br = runLocal(command);
            while ((line = br.readLine()) != null) {
                String pre = line.substring(0, 20);
                String[] fragments = pre.split(":");
                if (fragments.length >= 2) {
                    String mayName = fragments[1].trim();
                    if (mayName.startsWith("eth") || mayName.startsWith("ens")) {
                        br.close();
                        return mayName;
                    }
                }
            }
        } catch (Exception e) {
            //log
            System.out.println("exec <ip link + DEVICE> in local failed.");
            System.out.println(e);
        }
        return "";
    }

    private BufferedReader runLocal(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return br;
    }

    private void handleShutdownState() {
        //to do
    }

    private void handleWaitingState(NodePersConfig nodePersConfig) {
        //to do
    }

    //to do modify, replace zk`s computeIfAbsent
    private void completeTransition(NodePersConfig nodePathPValue) {
        opCommitLog.write(nodePathPValue.getOpNum(), nodePathPValue.getState());
        lastCommitNum = nodePathPValue.getOpNum();
        currentState = nodePathPValue.getState();
        System.out.println("complete state change. seq:" + lastCommitNum + " state:" + currentState);

        if (nodePathPValue.getState() == NodeState.SLEEPING) {
            reconnectDB();
        }
        String nodePathE = NODE_EPHE + "/" + id;
        byte[] nodePathEValue = SerializeUtil.getBytes(new NodeEpheConfig(type, lastCommitNum, currentState));
        while (running) {
            if (db.exists(nodePathE) == null) {
                if (!db.create(nodePathE, nodePathEValue)) {
                    try {
                        Thread.sleep(WAIT_PERS_NODE_TIME);
                        continue;
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                return;
            } else {
                if (db.setData(nodePathE, nodePathEValue, -1) != null) {
                    return;
                }
                try {
                    Thread.sleep(WAIT_PERS_NODE_TIME);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    public void reconnectDB() {
        if (db.getData("/", null) == null) {
            while (running && !db.connect()) {
                try {
                    Thread.sleep(WAIT_PERS_NODE_TIME);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    private void changeLocalState(NodeConfig config) {
        //use executor do local machine state operation
    }

    private void changeLocalIP(String ip) {
        System.out.println("change ip:" + ip);
        //TO DO
        //exec the script of changing local ip.
        //if true, set this.localIP = ip;
        //if false, return;
    }
}
