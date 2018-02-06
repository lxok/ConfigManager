package web.pkusz.manage;

import web.pkusz.protocal.NodePersConfig;

/**
 * Created by nick on 2017/9/14.
 */
public class NodeChangedTime {
    String id;
    long nextChangedTime;
    NodePersConfig nextConfig;

    NodeChangedTime(String id, long nextChangedTime, NodePersConfig nextConfig) {
        this.id = id;
        this.nextChangedTime = nextChangedTime;
        this.nextConfig = nextConfig;
    }
}
