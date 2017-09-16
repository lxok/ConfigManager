package web.pkusz.random;

/**
 * Created by nick on 2017/9/14.
 */
public class ScheduleTimeTargetSerial extends RandomTargetSerialHandler{
    //schedule service nodes changed time gap within 12 hour ~ 2 day

    public ScheduleTimeTargetSerial() {
        min = 43200000;  // 12 hour = 12 * 60 * 60 * 1000 ms
        max = 172800000;  //2 day = 2 *24 * 60 * 60 * 1000 ms

        //test: changed time gap within 10s ~ 30s
        /*
        min = 10000;
        max = 30000;
        */
    }

    @Override
    public String getNext(long random) {
        return String.valueOf(random);
    }
}
