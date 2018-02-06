package web.pkusz.random;

/**
 * Created by nick on 2017/7/22.
 */
public class DefaultTargetSerial extends RandomTargetSerialHandler {

    public DefaultTargetSerial() {
        min = 0;
        max = 65535;
        //max = (long)Math.pow(2, 20) - 1;
    }

    @Override
    public String getNext(long random) {
        return String.valueOf(random);
    }
}
