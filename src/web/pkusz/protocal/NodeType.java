package web.pkusz.protocal;

/**
 * Created by nick on 2018/1/6.
 */
/**
 在CM中，将被调度节点分成了三种不同类型，分别为
 1.挂载服务器，表示存储系统中的用户挂载节点。
 2.存储服务器，表示存储系统中的存储节点。
 3.管理服务器，表示CM系统的中的服务节点，包括zk集群节点。
 */
public class NodeType {
    public static final int MOUNT_SERVER = 0;
    public static final int STORE_SERVER = 1;
    public static final int MANAGE_SERVER = 2;
}
