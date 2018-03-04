package web.pkusz.manage;

import org.apache.zookeeper.data.Stat;
import web.pkusz.data.DatabaseZK;
import web.pkusz.protocal.NodeEpheConfig;
import web.pkusz.protocal.NodePersConfig;
import web.pkusz.random.IPTargetSerial;
import web.pkusz.random.RandomGenerator;
import web.pkusz.random.ScheduleTimeTargetSerial;
import web.pkusz.serialize.SerializeUtil;
import web.pkusz.server.ServerManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by nick on 2017/7/23.
 */
/**
 ConfigManager类是配置管理器(CM)的主要功能类，CM的所有对外功能和执行逻辑都在这个类及其子类中。
 CM的主要功能分为启动和运行两个步骤。
 CM在启动过程中，使用了高可用架构。能够满足多个CM同时处于启动状态，其中只有一个可以进入实际运行和调度状态，而其它则继续在启动
 状态，我们称前者为主节点，后者为备节点。当主节点由于服务停止或网络中断而无法继续对外提供调度服务时，备节点中的其中一个将会升级
 成为新的主节点，其它备节点继续在启动状态等待。在系统启动或运行过程中，新的CM节点可以在任意时刻被启动，系统将根据当前执行状态自
 动将该节点置为主节点或备节点。
 当一个CM节点启动并成为主节点后，将负责执行CM的系统调度功能。
 ScheduleService类是ConfigManager的内部类，包含对其它管理节点的状态调度功能逻辑。当一个CM节点成为主节点后，将创建一个ScheduleService
 实例用于执行CM的调度服务。

 CM的高可用和调度功能主要由与外部配置数据库zk的交互完成。对zk的基础数据操作由web.pkusz.data.DatabaseZK类完成。
 高可用功能是由在zk的/mimic/server/路径下创建临时路径primary完成，该主节点路径的创建者即是CM的当主节点。一个CM节点在启动后，
 首先会判断该主节点临时路径是否被创建，若不存在，则说明系统主节点不存在，则该节点会创建主节点路径，若创建成功，则成为新的主节点，
 若未创建成功，则继续判断主节点路径是否存在，若不存在，则再次尝试创建；若存在，则说明其它备节点已经成为主节点。当主节点存在时，
 所有备节点会监控zk的主节点路径，当该路径再次不存在时，说明此时的主节点与zk的通信被中断，此时，所有备节点会再次发起之前的竞选过
 程，直到新的主节点被选出。
 调度功能会在zk的/mimic/node/路径下创建每个调度节点的唯一路径，该路径的值就是当前的调度信息，CM通过该调度信息与被调度节点之间进行
 通信和调度。/mimic/node路径被分成两个部分，/mimic/node/persistent和/mimic/node/ephemeral两个节点创建路径，其中前者路径下创
 建的节点路径为永久路径，而后者路径下创建的路径为临时路径，前者中的节点路径的值记录某个节点的永久调度信息，后者的值记录某个节点
 的在线信息和当前状态信息。
 CM节点程序由ConfigNode类完成，当一个CM节点启动后，首先会创建/mimic/node/ephemeral/x临时路径，x为该节点的id值，CM在周期性监控
 中发现该节点为新创建节点后，则会相应地创建/mimic/node/persistent/x永久节点。当CM发送调度指令给x节点时，则会在/mimic/node/persistent/x
 路径的值中写入当次调度内容，CM节点获取到该信息后，会在本地进行相应的执行操作，操作完成后，将执行结果更新到/mimic/node/ephemeral/x
 的值中，CM在获取到该执行结果后，会进行下一步对应操作。

 CM在运行阶段，一共有4个线程被执行。
 1.CM的启动线程同样就是系统的主线程，当某个节点成为主节点后，调度服务会运行在主线程中，即ScheduleService类中的调度服务在主线程
 中执行，在调度服务中，会周期性获取/mimic/node/ephemeral/路径下的所有子路径，来获取当前所有的CM节点信息。
 2.在CM开始运行阶段，会开启主节点心跳监测线程，执行逻辑在方法primaryHeartbeat()中。在CM开发过程中，考虑到了如下情况：一个主节点
 由于网络中断而断开了与zk的通信，当这个中断时间大于zk的在线监测周期后，zk就会认为这个节点已经下线，在这种情况下，CM中的其它备
 节点将发起竞选过程，最终将有一个备节点成为新的主节点，开始对全局提供调度服务；与此同时，旧的主节点网络中断情况被修复，旧的主节点
 在并没有感知到上述整个过程，因此将执行它的调度逻辑。在这种情况下，整个CM出现了两个调度主节点，它们同时对外提供调度服务，产生了
 脑裂问题。为了避免这种情况，一个节点在成为主节点后，首先会启动主节点心跳检测服务，它执行在一个独立的线程中，在这个线程中，主
 节点周期性监测/mimic/server/primary这个主节点路径，该路径的关联值就是当前全局唯一主节点的id值。若监测到这个路径不存在或者返回
 的id值与节点自身的id值不相同，则说明当前节点已经不再是CM系统主节点，在这时会停止并关闭本节点的运行服务。
 3.主节点会提供对使用者的命令行接口，在节点成为主节点后，将启动一个独立线程接收用户命令行指令，提供CM对外的管理功能。在这个功能
 的实现上，这里使用了简易做法。事实上，这样的实现方式会使给用户返回的指令反馈和系统运行打印日志显示在同一个命令行中，这并不是我们
 想要的效果。在后期实现或优化中，可以在命令行客户端和CM服务器之间采用RPC通信方式。命令行请求实现类为web.pkusz.manage.CmdService。
 4.服务器线程，当CM服务器启动后，会同时启动服务器接收来自其他客户端的网络请求，这些请求包括对证书系统的操作，对全局数据的存取等。
 服务器实现类为web.pkusz.server.serverManager。
 */
public class ConfigManager {

    public static final String VERSION = "1.0";

    public static final String ROOT = "/mimic";
    public static final String GROUP = ROOT + "/group";
    public static final String USER = ROOT + "/user";
    public static final String NODE = ROOT + "/node";
    public static final String CLIENT = ROOT + "/client";
    public static final String FILE = ROOT + "/file";
    public static final String STATE = ROOT + "/state";
    public static final String CONFIG = ROOT + "/config";
    public static final String SERVER = ROOT + "/server";

    private static final String PRIMARY = SERVER + "/primary";

    public static final String NODE_EPHE = NODE + "/ephemeral";
    public static final String NODE_PERS = NODE + "/persistent";

    private static final String PROP = "configmanager.properties";

    private Properties prop;
    private DatabaseZK db;
    private String managerID;
    private String address;
    ScheduleService scheduleService;

    volatile boolean running = true;
    volatile boolean dbInit = false;
    volatile boolean isPrimary = false;

    public ConfigManager() {
    }

    public static void main(String[] args) throws Exception {
        ConfigManager cm = new ConfigManager();
        cm.start();
    }

    //cmd line util interfaces
    //1.get server status - primary / backup
    //2.get nodes status - get all nodes status info (online or offline)
    //3.propose schedule task to an online node
    /**
     * 获取当前CM服务器节点信息，若当前服务器为主节点，返回true。方法在CmdService类中调用。
     */
    public boolean getServerStatus() {
        return dbInit && isPrimary;
    }

    //list(0) - online nodes, list(1) - offline nodes
    /**
     * 获取当前所有CM节点信息，返回值list(0)为所有在线节点，list(1)为所有离线节点。方法在CmdService类中调用。
     */
    public List<List<Node>> getNodesStatus() {
        if (!getServerStatus()) {
            return null;
        }
        List<List<Node>> res = new ArrayList<>(2);

        List<Node> onlineNodes = new ArrayList<>();
        Iterator<String> iter = scheduleService.onlineNodes.iterator();
        while (iter.hasNext()) {
            onlineNodes.add(scheduleService.nodesMap.get(iter.next()));
        }

        List<Node> offlineNodes = new ArrayList<>();
        Iterator<Map.Entry<String, Node>> iter2 = scheduleService.nodesMap.entrySet().iterator();
        while (iter2.hasNext()) {
            Map.Entry<String, Node> e = iter2.next();
            if (!scheduleService.onlineNodes.contains(e.getKey())) {
                offlineNodes.add(e.getValue());
            }
        }

        res.add(0, onlineNodes);
        res.add(1, offlineNodes);
        return res;
    }

    /**
     * 获取某个CM节点对象。方法在CmdService类中调用。
     */
    public Node getNodeStatus(String nodeid) {
        if (!getServerStatus()) {
            return null;
        }
        return scheduleService.nodesMap.get(nodeid);
    }

    /**
     * 在命令行打印指定node的历史状态记录。方法在CmdService类中调用。
     */
    public void getNodeHistory(Node node) {
        NodeLog log = node.getNodeLog();
        log.printAll();
    }

    /**
     * 对某个指定CM节点提交状态变更任务。方法在CmdService类中调用。
     * nodeid: 指定的CM节点id
     * status: 指定节点将要变更的状态
     * interval: 指定距当前多久时间后发起节点状态变更。
     */
    public boolean submit(String nodeid, int status, long interval) {
        return scheduleService.submit(nodeid, status, interval);
    }

    /**
     * CM服务器初始化
     */
    private boolean init() {
        FileInputStream is;
        try {
            is = new FileInputStream(PROP);
        } catch (FileNotFoundException e) {
            //log
            System.out.println("System config file is not found(configmanager.properties).");
            System.out.println(e.toString());
            return false;
        }
        prop = new Properties();
        try {
            prop.load(is);
            GlobalProperties.setGlobalProp(prop);
        } catch (IOException e) {
            //log
            System.out.println("Load system config file failed.");
            System.out.println(e.toString());
            return false;
        }
        managerID = prop.getProperty("id");
        if (managerID == null) {
            //log
            System.out.println("System config file doesn't contain the manager id(id).");
            return false;
        }
        address = managerID;
        db = new DatabaseZK(prop);
        if (!db.connect()) {
            return false;
        }
        return true;
    }

    /**
     * CM服务器启动入口。
     */
    private void start() throws Exception {
        if (!init()) {
            return;
        }

        while (running && !dbInit) {
            Stat s = db.exists(ROOT);
            if (s == null) {
                Value rootVal = new RootValue(VERSION, managerID);
                boolean res = db.create(ROOT, rootVal.serialize());
                if (!res) {
                    continue;
                }
                res = initBaseNodes();
                if (!res) {
                    //log
                    System.out.println("Initial base noses failed, please clean the database.");
                    //TO DO clean, cause it do not allow that the root node is existed but other base nodes are not existed.
                    return;
                }
            } else {
                if (checkVersion() < 0) {
                    //log
                    System.out.println("The version of config manager is too old for database, start failed.");
                    return;
                }
            }
            dbInit = true;
        }

        while (running && !isPrimary) {
            PrimaryExistsWatcher watcher = PrimaryExistsWatcherFactory.getWatcher();
            Stat s = db.exists(PRIMARY, watcher);
            if (s == null) {
                boolean c = db.createEphemeral(PRIMARY, new PrimaryValue().serialize());
                if (c) {
                    isPrimary = true;
                    int v = checkVersion();
                    if (v < 0) {
                        //log
                        System.out.println("The version of config manager is too old for database, start failed.");
                        return;
                    }
                    if (v > 0) {
                        modifyDatabaseVersion();
                    }
                    //modify database because this config manager has higher version.
                    boolean res = initBaseNodes();
                    if (!res) {
                        //log
                        System.out.println("Initial base noses failed, please clean the database.");
                        //TO DO clean, cause it do not allow that the root node is existed but other base nodes are not existed.
                        return;
                    }
                }
                continue;
            }
            //Backup wait and watch
            watcher.sem.acquire();
        }

        if (running) {
            String en = prop.getProperty("encrypt");
            if (en != null && en.equals("true")) {
                SerializeUtil.setEncrypt(db);
            }
            
            startService();
        }
    }

    public void close() {
        running = false;
    }

    private void modifyDatabaseVersion() {
        db.setData(ROOT, new RootValue().serialize(), -1);
    }

    /**
     * CM服务器启动后出在zk上创建系统所需路径和关联值。
     */
    private boolean initBaseNodes() {
        try {
            createIfAbsent(ROOT, null);
            createIfAbsent(GROUP, null);
            createIfAbsent(USER, null);
            createIfAbsent(CLIENT, null);
            createIfAbsent(FILE, null);
            createIfAbsent(STATE, null);
            createIfAbsent(CONFIG, null);
            createIfAbsent(SERVER, null);
            createIfAbsent(NODE, null);
            createIfAbsent(NODE_EPHE, null);
            createIfAbsent(NODE_PERS, null);
        } catch (Exception e) {
            //log
            System.out.println("Create base nodes failed.");
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    private int checkVersion() throws Exception{
        byte[] b = db.getData(ROOT, null);
        if (b == null) {
            throw new Exception("get data of root path in the method of check version failed.");
        }
        RootValue v = new RootValue();
        v.deserialize(b);
        return VersionComparator.compare(VERSION, v.version);
    }

    private void startService() {
        primaryHeartbeat(); //avoid brain split for a long time
        new Thread(new CmdService(this)).start(); //start cmd service thread
        new Thread(new ServerManager()).start(); //start server thread
        scheduleService = new ScheduleService(); //init and start schedule service in main thread
        scheduleService.start();
    }

    //avoid brain split for a long time
    /**
     * 避免出现脑裂问题，周期性循环检测当前节点是否仍是系统主节点。
     */
    private void primaryHeartbeat() {
        final long time = Long.valueOf(prop.getProperty("primaryheartbeat"));
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(time);
                    byte[] b = db.getData(PRIMARY, null);
                    if (b == null) {
                        isPrimary = false;
                        running = false;
                        return;
                    }
                    PrimaryValue pri = new PrimaryValue();
                    pri.deserialize(b);
                    pri.address = address;  //has not implemented serialize method
                    if (!pri.address().equals(address)) {
                        isPrimary = false;
                        running = false;
                        return;
                    }
                } catch (InterruptedException ie) {
                    //log
                    System.out.println("The thread of primary heartbeat is interrupted.");
                    System.out.println(ie.getMessage());
                }
            }
        });
        t.start();
    }

    private void createIfAbsent(String path, byte[] value) throws Exception {
        Stat s = db.exists(path);
        if (s == null) {
            db.create(path, value);
        }
    }

    /**
     * ScheduleService类包含对其它管理节点的状态调度功能逻辑。当一个CM节点成为主节点后，将创建一个ScheduleService,实例用于
     * 执行CM的调度服务。
     */
    class ScheduleService {

        static final int CYCLE_PERIOD = 10000; //10s
        static final String DEFAULT_PORT = "5000";

        ConcurrentHashMap<String, Node> nodesMap = new ConcurrentHashMap<>();
        Set<String> onlineNodes = new HashSet<>();
        PriorityQueue<NodeChangedTime> scheduleQueue = new PriorityQueue<>(new NodeChangedTimeComp());
        Set<String> inScheduleQueue = new HashSet<>();
        //Map<String, String> usedIP = new HashMap<>();  // *has removed the function of changing ip*
        RandomGenerator nextScheduleTimeRandom;
        RandomGenerator ipRandom;

        private ScheduleService() {
            nextScheduleTimeRandom = new RandomGenerator();
            nextScheduleTimeRandom.setRandomTarget(new ScheduleTimeTargetSerial());
            ipRandom = new RandomGenerator();
            ipRandom.setRandomTarget(new IPTargetSerial());
        }

        private void init() {
            List<String> nodes = db.getChildren(NODE_PERS);
            if (nodes == null || nodes.size() == 0) {
                return;
            }
            Iterator<String> i = nodes.iterator();
            String id;
            byte[] val;
            while (i.hasNext()) {
                id = i.next();
                val = db.getData(NODE_PERS + "/" + id, null);
                if (val == null) {
                    continue;
                }
                Node node = new Node(id);
                NodePersConfig nodePersConfig = new NodePersConfig();
                nodePersConfig.updateFromEntryList(SerializeUtil.getEntries(val));
                node.setNodePersConfig(nodePersConfig);
                nodesMap.put(id, node);
                //usedIP.put(node.ip, id);  // *has removed the function of changing ip*
            }
        }

        /**
         * 调度服务启动入口
         */
        public void start() {
            init();
            while (running) {
                List<String> nodes = db.getChildren(NODE_EPHE);

                if (nodes != null && nodes.size() != 0) {
                    Iterator<String> i = nodes.iterator();
                    while (i.hasNext()) {
                        String nodeid = i.next();
                        //newly added nodes in system
                        if (!nodesMap.containsKey(nodeid)) {
                            createNewNode(nodeid);
                        }
                        //newly added nodes
                        if (!onlineNodes.contains(nodeid)) {
                            onlineNodes.add(nodeid);
                            Node node = nodesMap.get(nodeid);
                            node.lastChangedTime = new Date().getTime(); //reset
                            //long ct = node.lastChangedTime + Long.valueOf(nextScheduleTimeRandom.getNext(1).get(0));
                            //scheduleQueue.offer(new NodeChangedTime(node.id, ct));
                        }
                        //update nodes config state
                        updateNodeConfig(nodeid);
                    }
                }

                //newly reduced nodes
                Iterator<String> olite = onlineNodes.iterator();
                while (olite.hasNext()) {
                    String id = olite.next();
                    if (!nodes.contains(id)) {
                        olite.remove();
                    }
                }

                //scheduleQueue
                long now = new Date().getTime();
                while (!scheduleQueue.isEmpty() && now >= scheduleQueue.peek().nextChangedTime) {
                    NodeChangedTime nct;
                    synchronized (scheduleQueue) {
                        nct = scheduleQueue.poll();
                        inScheduleQueue.remove(nct.id);
                    }
                    if (!onlineNodes.contains(nct.id)) {
                        continue;
                    }
                    Node node = nodesMap.get(nct.id);
                    node.lastChangedTime = nct.nextChangedTime;
                    if (db.setData(NODE_PERS + "/" + node.id, SerializeUtil.getBytes(nct.nextConfig), -1) != null) {
                        node.setNodePersConfig(nct.nextConfig);
                        node.log();
                    }
                }

                //blocking wait
                try {
                    Thread.sleep(CYCLE_PERIOD);
                } catch (InterruptedException e) {
                    //log
                    System.out.println("Schedule service is interrupted." + e.toString());
                }
            }
        }

        /**
         * 对某个指定cm node提交调度任务。
         */
        //scheduleTimeFromNow -- ms
        public boolean submit(String nodeid, int status, long scheduleTimeFromNowMilliSeconds) {
            if (!onlineNodes.contains(nodeid)) {
                return false;
            }
            Node node = nodesMap.get(nodeid);
            //verify the new task is not contains in scheduleQueue.
            if (inScheduleQueue.contains(nodeid) || node.nodePersConfig.getOpNum() > node.nodePersConfig.getOpNum()) {
                return false;
            }
            if (node.nodePersConfig.getState() == status) {
                return true;
            }
            NodePersConfig newNodeConfig = new NodePersConfig(node.nodePersConfig.getOpNum() + 1, status, null);
            long nextChangedTime = new Date().getTime() + scheduleTimeFromNowMilliSeconds;
            NodeChangedTime nct = new NodeChangedTime(node.id, nextChangedTime, newNodeConfig);
            synchronized (scheduleQueue) {
                scheduleQueue.offer(nct);
                inScheduleQueue.add(nct.id);
            }
            return true;
        }

        private void updateNodeConfig(String nodeid) {
            String path = NODE_EPHE + "/" + nodeid;
            byte[] val = db.getData(path, null);
            if (val != null) {
                Node node = nodesMap.get(nodeid);
                NodeEpheConfig config = new NodeEpheConfig();
                config.updateFromEntryList(SerializeUtil.getEntries(val));
                node.setNodeEpheConfig(config);
            }
        }

        private void createNewNode(String nodeid) {
            Node node = new Node(nodeid);
            byte[] val = db.getData(NODE_EPHE + "/" + nodeid, null);
            NodeEpheConfig nodeEpheConfig = new NodeEpheConfig();
            nodeEpheConfig.updateFromEntryList(SerializeUtil.getEntries(val));
            node.setNodeEpheConfig(nodeEpheConfig);
            node.setNodePersConfig(new NodePersConfig(0, 0, null));
            db.create(NODE_PERS + "/" + node.id, SerializeUtil.getBytes(node));
            nodesMap.put(nodeid, node);
        }

        /*  // *has removed the function of changing ip*
        private String allocateIP() {
            String ip = ipRandom.getNext(1).get(0);
            while (usedIP.containsKey(ip)) {
                ip = ipRandom.getNext(1).get(0);
            }
            return ip;
        }
        */
    }
}
