package web.pkusz.manage;

/**
 * Created by nick on 2017/8/13.
 */
public class PrimaryExistsWatcherFactory {
    private static PrimaryExistsWatcher watcher = null;

    public static PrimaryExistsWatcher getWatcher() {
        if (watcher == null) {
           watcher = new PrimaryExistsWatcher();
        }
        return watcher;
    }
}
