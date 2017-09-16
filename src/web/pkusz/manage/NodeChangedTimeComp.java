package web.pkusz.manage;

import java.util.Comparator;

/**
 * Created by nick on 2017/9/14.
 */
public class NodeChangedTimeComp implements Comparator<NodeChangedTime> {
    @Override
    public int compare(NodeChangedTime o1, NodeChangedTime o2) {
        long d = o1.nextChangedTime - o2.nextChangedTime;
        if (d > 0) {
            return 1;
        }
        if (d < 0) {
            return -1;
        }
        return 0;
    }
}
