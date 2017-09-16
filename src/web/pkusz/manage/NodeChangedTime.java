package web.pkusz.manage;

/**
 * Created by nick on 2017/9/14.
 */
public class NodeChangedTime {
    String id;
    long nextChangedTime;

    NodeChangedTime(String id, long nextChangedTime) {
        this.id = id;
        this.nextChangedTime = nextChangedTime;
    }
}
