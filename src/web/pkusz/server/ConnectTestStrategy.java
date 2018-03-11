package web.pkusz.server;

import web.pkusz.serialize.Entry;
import web.pkusz.serialize.SerializeUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by nick on 2017/12/3.
 */
public class ConnectTestStrategy extends RequestProcessStrategy {

    @Override
    public String process(List<Entry> entries) {

        Iterator<Entry> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry x = iter.next();
            System.out.println(x.key + " : " + x.value);
        }

        Entry e = new Entry("status", "OK");
        List<Entry> list = new ArrayList<>();
        list.add(e);
        return SerializeUtil.getStringFromEntries(list);
    }
}
