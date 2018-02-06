package web.pkusz.server;

import web.pkusz.serialize.Entry;

import java.util.List;

/**
 * Created by nick on 2017/12/3.
 */
public abstract class RequestProcessStrategy {

    //respond_state_code
    public static final int OK = 0;
    public static final int ILLEGAL_CONNECT_ERROR = 1;
    public static final int REQUEST_TYPE_ERROR = 2;

    abstract public String process(List<Entry> entries);
}
