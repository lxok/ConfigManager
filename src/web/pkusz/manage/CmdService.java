package web.pkusz.manage;

import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by nick on 2018/1/20.
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
                System.out.println("online nodes:");
                Iterator<Node> iter = lists.get(0).iterator();
                while (iter.hasNext()) {
                    Node node = iter.next();
                    System.out.println(node.id + " " + node.nodeEpheConfig.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
                }

                System.out.println("offline nodes:");
                iter = lists.get(1).iterator();
                while (iter.hasNext()) {
                    Node node = iter.next();
                    System.out.println(node.id + " " + node.nodeEpheConfig.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
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
                    System.out.println(node.id + " " + node.nodeEpheConfig.getType() + " " + node.nodePersConfig.getOpNum() + " " + node.nodePersConfig.getState());
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
