package web.pkusz.server;

import web.pkusz.serialize.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nick on 2017/12/3.
 */
public class RequestProcessor {

    //singleton
    private static RequestProcessor proc = new RequestProcessor();

    public static String process(List<Entry> entries) {
        return proc.processRequest(entries);
    }

    //default_error_processors
    public static final int ERROR_ILLEGAL_CONNECT = -1;
    public static final int ERROR_REQUEST_TYPE = -2;

    //request_type_code
    public static final int CONNECT_TEST = 1;
    public static final int X509_VERIFIED = 2;
    public static final int X509_ISSUE = 3;
    public static final int X509_RENEW = 4;
    public static final int X509_REVOKE = 5;
    public static final int DATA_PUT = 6;
    public static final int DATA_GET = 7;

    private Map<Integer, RequestProcessStrategy> processors = new HashMap<>();
    private Map<Integer, RequestProcessStrategy> errorProc = new HashMap<>();

    private RequestProcessor() {
        errorProc.put(ERROR_ILLEGAL_CONNECT, new ErrorIllegalConnect());
        errorProc.put(ERROR_REQUEST_TYPE, new ErrorRequestType());

        processors.put(CONNECT_TEST, new ConnectTestStrategy());
        processors.put(X509_VERIFIED, new X509VerifiedStrategy());
    }

    private String processRequest(List<Entry> entries) {
        if (entries == null || entries.size() == 0) {
            return errorProc.get(ERROR_ILLEGAL_CONNECT).process(entries);
        }

        int type;
        try {
            type = Integer.parseInt(entries.get(0).key);
        } catch (Exception e) {
            return errorProc.get(ERROR_ILLEGAL_CONNECT).process(entries);
        }

        if (!processors.containsKey(type)) {
            errorProc.get(ERROR_REQUEST_TYPE).process(entries);
        }

        return processors.get(type).process(entries);
    }
}
