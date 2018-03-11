package web.pkusz.manage;

import jdk.internal.util.xml.impl.ReaderUTF8;
import web.pkusz.protocal.NodeEpheConfig;
import web.pkusz.protocal.NodePersConfig;
import web.pkusz.protocal.NodeType;
import web.pkusz.serialize.Entry;
import web.pkusz.serialize.Serialize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by nick on 2017/9/14.
 */
public class Node implements Serialize {

    private static NodeEpheConfig INIT_NODE_EPHE = new NodeEpheConfig();

    String id;
    String ip;
    String port;
    long lastChangedTime;
    NodePersConfig nodePersConfig;
    NodeEpheConfig nodeEpheConfig;
    NodeLog log; //TO DO log init

    Node(String id, String ip, String port, long lastChangedTime, NodePersConfig nodePersConfig) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.lastChangedTime = lastChangedTime;
        this.nodePersConfig = nodePersConfig;
        this.nodeEpheConfig = INIT_NODE_EPHE;
        log = new NodeLog(id);
    }

    Node(String id) {
        this.id = id;
        this.ip = "";
        this.port = "";
        //this.lastChangedTime =
        log = new NodeLog(id);
    }

    public NodeLog getNodeLog() {
        return log;
    }

    public int getType() {
        if (nodeEpheConfig == null) {
            return NodeType.UNKNOWN;
        }
        return nodeEpheConfig.getType();
    }

    public void log() {
        SimpleDateFormat sdf= new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date dt = new Date(lastChangedTime);
        String sDateTime = sdf.format(dt);
        String c = nodePersConfig.toString();
        log.appendRecord(sDateTime + " " + c);
    }

    public void setLastChangedTime(long lastChangedTime) {
        this.lastChangedTime = lastChangedTime;
    }

    public void setNodePersConfig(NodePersConfig nodePersConfig) {
        this.nodePersConfig = nodePersConfig;
    }

    public void setNodeEpheConfig(NodeEpheConfig nodeEpheConfig) {
        this.nodeEpheConfig = nodeEpheConfig;
    }

    @Override
    public List<Entry> genEntryList() {
        return nodePersConfig.genEntryList();
    }

    @Override
    public void updateFromEntryList(List<Entry> entries) {}
}
