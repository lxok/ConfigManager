package web.pkusz.random;

/**
 * Created by nick on 2017/7/16.
 */
public abstract class RandomTargetSerialHandler {
    protected long min;
    protected long max;

    public void setRangeMin(int min) {
        this.min = min;
    }

    public void setRangeMax(int max) {
        this.max = max;
    }

    public long getRangeMin() {
        return min;
    }

    public long getRangeMax() {
        return max;
    }

    public abstract String getNext(long random);
}
