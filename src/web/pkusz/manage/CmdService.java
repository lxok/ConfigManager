package web.pkusz.manage;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by nick on 2018/1/20.
 */
/**
 CmdService类用于处理CM用户在本地的命令行指令。
 CM在启动后，将会在本地一个单独的线程中接收并处理用户指令，相应的指令处理逻辑在本类中完成。

 当前已完成的命令行指令包括（x表示某个节点id）：
 server 或 server status：查询CM中的所有节点状态。
 node x：查询某节点的当前状态。
 node -h x：查询某节点的所有历史状态。
 submit x 状态标志 调度距离当前间隔（毫秒）  其中，状态标志表示节点需要被调度的状态，距离当前间隔表示距当前提交任务多久后该调度任务被发起
 */
public class CmdService implements Runnable {
    static final String CMD_FORMOT_ERROR = "cmd format error";

    ConfigManager cm;
    Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {
        CmdService cmd = new CmdService(new ConfigManager());
        cmd.run();
    }

    public CmdService(ConfigManager cm) {
        this.cm = cm;
    }

    @Override
    public void run() {
        while (cm.running) {
            String line = scan.nextLine();
            parseAndExec(line);
        }
    }

    /*
    public void start() {
        while (cm.running) {
            String line = scan.nextLine();
            parseAndExec(line);
        }
    }
    */

    private void parseAndExec(String line) {
        if (line.length() == 0) {
            System.out.println(CMD_FORMOT_ERROR);
            return;
        }
        String[] cmd = line.split(" ");
        if (cmd[0].equals("server")) {
            if (cmd.length == 1 || cmd[1].equals("status")) {
                if (!cm.isPrimary) {
                    System.out.println("The server is backup, can not exec query command.");
                    return;
                }
                List<List<Node>> lists = cm.getNodesStatus();
                System.out.println("node_id|type|seq_num|state");
                System.out.println("online nodes:");
                Iterator<Node> iter = lists.get(0).iterator();
                while (iter.hasNext()) {
                    Node node = iter.next();
                    System.out.println(node.id + " " + node.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
                }

                System.out.println("offline nodes:");
                iter = lists.get(1).iterator();
                while (iter.hasNext()) {
                    Node node = iter.next();
                    System.out.println(node.id + " " + node.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
                }
            } else {
                System.out.println(CMD_FORMOT_ERROR);
            }
        } else if (cmd[0].equals("node")) {
            if (cmd.length <= 1) {
                System.out.println(CMD_FORMOT_ERROR);
                return;
            }
            if (cmd[1].equals("-h")) {
                Node node = cm.getNodeStatus(cmd[2]);
                if (node == null) {
                    System.out.println("this node is not existed in system." + " " + cmd[2]);
                } else {
                    cm.getNodeHistory(node);
                }
            } else {
                Node node = cm.getNodeStatus(cmd[1]);
                if (node == null) {
                    System.out.println("this node is not existed in system." + " " + cmd[1]);
                } else {
                    System.out.println("node_id|type|seq_num|state");
                    System.out.println(node.id + " " + node.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
                }
            }
        } else if (cmd[0].equals("submit")) {
            if (cmd.length != 4) {
                System.out.println(CMD_FORMOT_ERROR);
                return;
            }
            if (cm.getNodeStatus(cmd[1]) == null) {
                System.out.println("this node is not existed in system." + " " + cmd[1]);
                return;
            }
            if (cm.submit(cmd[1], Integer.parseInt(cmd[2]), Long.parseLong(cmd[3]))) {
                System.out.println("Submit task successful.");
            } else {
                System.out.println("Submit task failed.");
            }
        } else {
            System.out.println(CMD_FORMOT_ERROR);
        }
    }
}
