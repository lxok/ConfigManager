package web.pkusz;

import web.pkusz.manage.ConfigManager;
import web.pkusz.node.ConfigNode;

/**
 * Created by nick on 2018/3/6.
 */
/**
 * CM程序启动入口，main方法是CM的程序jar包的入口方法。
 * 根据启动参数决定是CM服务器开启还是CM节点程序开启。
 *
 * 若调用命令为java -jar ConfigManager.jar node或java -jar ConfigManager.jar -node, 启动CM节点。
 * 否则启动CM服务器。
 */
public class CMBootstrap {
    public static void main(String[] args) throws Exception {
        if (args != null && args.length != 0 && (args[0].equalsIgnoreCase("node") || args[0].equalsIgnoreCase("-node"))) {
            ConfigNode cn = new ConfigNode();
            cn.start();
        } else {
            ConfigManager cm = new ConfigManager();
            cm.start();
        }
    }
}
