package web.pkusz.manage;

import org.apache.zookeeper.data.Stat;
import web.pkusz.data.DatabaseZK;
import web.pkusz.protocal.NodeEpheConfig;
import web.pkusz.protocal.NodePersConfig;
import web.pkusz.protocal.NodeState;
import web.pkusz.random.IPTargetSerial;
import web.pkusz.random.RandomGenerator;
import web.pkusz.random.ScheduleTimeTargetSerial;
import web.pkusz.serialize.Entry;
import web.pkusz.serialize.SerializeUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by nick on 2017/7/23.
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
    public boolean getServerStatus() {
        return dbInit && isPrimary;
    }

    //list(0) - online nodes, list(1) - offline nodes
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

    public Node getNodeStatus(String nodeid) {
        if (!getServerStatus()) {
            return null;
        }
        return scheduleService.nodesMap.get(nodeid);
    }

    public void getNodeHistory(Node node) {
        NodeLog log = node.getNodeLog();
        log.printAll();
    }

    public boolean submit(String nodeid, int status, long interval) {
        return scheduleService.submit(nodeid, status, interval);
    }

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
        //TO DO
        primaryHeartbeat(); //avoid brain split for a long time
        new Thread(new CmdService(this)).start();
        scheduleService = new ScheduleService();
        scheduleService.start();
    }

    //avoid brain split for a long time
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
