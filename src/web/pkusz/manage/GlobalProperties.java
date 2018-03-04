package web.pkusz.manage;

import java.util.Properties;

/**
 * Created by nick on 2018/3/2.
 */
public class GlobalProperties {
    public static void setGlobalProp(Properties prop) {
        gprop.prop = prop;
    }

    public static Properties getGlobalProp() {
        return gprop.prop;
    }

    private static GlobalProperties gprop = new GlobalProperties();

    private GlobalProperties() {}

    Properties prop;
}
