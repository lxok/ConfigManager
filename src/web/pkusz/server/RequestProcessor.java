package web.pkusz.server;

import web.pkusz.serialize.Entry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nick on 2017/12/3.
 */
/**
 RequestProcessor类是CM服务器的请求分发类。
 当请求到达server时，channel中的RequestHandler会将请求从utf-8解码后转发给RequestProcessor，之后Request将根据请求类型的不同来
 将请求进一步转发给对应的请求处理对象。

 在实现中，使用策略模式对请求处理逻辑进行抽象，请求处理逻辑类的基类为RequestProcessStrategy。
 不同请求类型对应的请求码在类中以静态变量的方式定义。
 除了正常的请求类型，错误请求类型，它们的请求码被定义为负值，也在类中被定义。
 为了减少请求对象的创建，类中使用了对象池管理不同的请求对象，当请求到达时，使用对象池中的对应处理对象来处理请求，而不是新建处理
 对象。
 对象池以及其中的请求处理对象都在RequestProcessor对象的构造函数中被创建。
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
        processors.put(X509_ISSUE, null);
        processors.put(X509_RENEW, null);
        processors.put(X509_REVOKE, null);
        processors.put(DATA_PUT, null);
        processors.put(DATA_GET, null);
    }

    private String processRequest(List<Entry> entries) {
        if (entries == null || entries.size() == 0) {
            return errorProc.get(ERROR_ILLEGAL_CONNECT).process(entries);
        }
        int type;
        try {
            type = Integer.parseInt(entries.get(0).value);
        } catch (Exception e) {
            return errorProc.get(ERROR_ILLEGAL_CONNECT).process(entries);
        }

        if (!processors.containsKey(type) || processors.get(type) == null) {
            errorProc.get(ERROR_REQUEST_TYPE).process(entries);
        }
        return processors.get(type).process(entries);
    }
}
