package web.pkusz.server;

import web.pkusz.serialize.Entry;
import web.pkusz.serialize.Serialize;
import web.pkusz.serialize.SerializeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nick on 2017/12/3.
 */
public class ErrorRequestType extends RequestProcessStrategy {

    public static final int STATE = RequestProcessStrategy.REQUEST_TYPE_ERROR;
    public static final Entry STATE_ENTRY = new Entry("state", String.valueOf(STATE));


    @Override
    public String process(List<Entry> entries) {
        List<Entry> response = new ArrayList<>();
        response.add(STATE_ENTRY);
        return SerializeUtil.getStringFromEntries(response);
    }
}
