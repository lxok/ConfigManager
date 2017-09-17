package web.pkusz.random;

/**
 * Created by nick on 2017/9/14.
 */
public class IPTargetSerial extends RandomTargetSerialHandler {
    //192.168.100.xxx, xxx is random number within 0 ~ 255

    static final String DEFAULT_IP_PREFIX = "192.168.100.";
    String ipPrefix;

    public IPTargetSerial() {
        min = 0;
        max = 255;
    }

    public IPTargetSerial(String ipPrefix) {
        min = 0;
        max = 255;
        this.ipPrefix = ipPrefix;
    }

    @Override
    public String getNext(long random) {
        if (ipPrefix == null) {
            return DEFAULT_IP_PREFIX + random;
        }
        return ipPrefix + random;
    }
}
