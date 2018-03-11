package web.pkusz.node;

import web.pkusz.data.DatabaseZK;
import web.pkusz.node.log.OpCommitLog;
import web.pkusz.protocal.*;
import web.pkusz.serialize.SerializeUtil;

import java.io.*;
import java.time.Instant;
import java.util.Properties;

/**
 * Created by nick on 2017/9/6.
 */
/**
 ConfigNode类是CM节点的启动类，CM节点在本地启动后，将接收来自CM的调度指令，并在本地执行相应的操作，执行完毕后，将本次执行操作
 结果返回给CM。

 CM的调度功能主要由与外部配置数据库zk的交互完成。对zk的基础数据操作由web.pkusz.data.DatabaseZK类完成。
 调度功能会在zk的/mimic/node/路径下创建每个调度节点的唯一路径，该路径的值就是当前的调度信息，CM通过该调度信息与被调度节点之间进行
 通信和调度。/mimic/node路径被分成两个部分，/mimic/node/persistent和/mimic/node/ephemeral两个节点创建路径，其中前者路径下创
 建的节点路径为永久路径，而后者路径下创建的路径为临时路径，前者中的节点路径的值记录某个节点的永久调度信息，后者的值记录某个节点
 的在线信息和当前状态信息。
 CM节点程序由ConfigNode类完成，当一个CM节点启动后，首先会创建/mimic/node/ephemeral/x临时路径，x为该节点的id值，CM在周期性监控
 中发现该节点为新创建节点后，则会相应地创建/mimic/node/persistent/x永久节点。当CM发送调度指令给x节点时，则会在/mimic/node/persistent/x
 路径的值中写入当次调度内容，CM节点获取到该信息后，会在本地进行相应的执行操作，操作完成后，将执行结果更新到/mimic/node/ephemeral/x
 的值中，CM在获取到该执行结果后，会进行下一步对应操作。

 CM的状态在web.pkusz.protocal.NodeState类中规定，包括如下几个状态：
 READY = 0;
 ACTION = 1;
 SLEEPING = 2;
 SHUTDOWN = 3;
 WAITING = 4;
 这些状态位用于表示CM节点的当前状态或将要变更状态。

 CM节点在执行过程中，可能会产生宕机情况，这样CM节点程序会中止，在这种情况下，CM只能获知该节点产生宕机，但无法获取该节点此次调度
 具体的执行情况。当CM节点被重新启动后，节点自身也无法判断上一次调度执行状况。为了解决这个问题，引入了本地调度日志功能，当节点在
 每次调度本地执行完毕后，都会将本次执行结果写入本地日志，日志被成功写入后，称本次调度被成功提交。只有提交完成的调度任务才会向CM
 反馈执行成功。当一个CM节点被重新启动后，会首先读取本地日志，获取上次本地调度情况，以使得节点能够顺利进行接下来的调度任务接收。
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

    public ConfigNode() {}

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

    /**
     * CM节点程序入口
     */
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

    /**
     * 将CM节点状态转换为请求状态。
     */
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

    /**
     * Sleep状态执行逻辑，首先将本地网卡关闭，断开网络；一定时间后，重新开启网卡，重启网络。
     */
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

    /**
     * 将命令在本地操作系统执行调用。
     */
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

    /**
     * CM节点状态转换完成处理，先将本次操作结果写入本地log，之后在zk上更新状态转换完成信息。
     */
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

    private void changeLocalIP(String ip) {
        System.out.println("change ip:" + ip);
        //TO DO
        //exec the script of changing local ip.
        //if true, set this.localIP = ip;
        //if false, return;
    }
}
