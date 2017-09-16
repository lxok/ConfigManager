package web.pkusz.manage;

import java.util.Date;

/**
 * Created by nick on 2017/8/6.
 */
public class RootValue implements Value {
    Date date;
    String version;
    String managerID;

    public RootValue() {}

    public RootValue(String version, String managerID) {
        date = new Date();
        this.version = version;
        this.managerID = managerID;
    }

    @Override
    public byte[] serialize() {
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] val) {
    }
}
