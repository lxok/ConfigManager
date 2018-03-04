package web.pkusz.protocal;

/**
 * Created by nick on 2018/1/6.
 */
/**
 NodeState类内部使用静态常量的方式定义全局节点状态，节点状态用于CM向CM节点发送状态切换指令和CM节点当前状态表示。
*/
public class NodeState {
    public static final int READY = 0;
    public static final int ACTION = 1;
    public static final int SLEEPING = 2;
    public static final int SHUTDOWN = 3;
    public static final int WAITING = 4;
}
