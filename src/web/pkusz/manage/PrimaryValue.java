package web.pkusz.manage;

/**
 * Created by nick on 2017/8/13.
 */
public class PrimaryValue implements Value{

    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] val) {}

    public String address() {
        return "1";
    }
}
