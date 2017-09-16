package web.pkusz.manage;

/**
 * Created by nick on 2017/8/6.
 */
public interface Value {
    byte[] serialize();
    void deserialize(byte[] val);
}
