package web.pkusz.server;

import web.pkusz.serialize.Entry;

import java.util.List;

/**
 * Created by nick on 2017/12/25.
 */
public class DataPutStrategy extends RequestProcessStrategy {

    //request former: type:6, class:, key:, value:

    @Override
    public String process(List<Entry> entries) {
        return null;
    }
}
