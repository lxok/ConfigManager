package web.pkusz.serialize;

import java.util.List;

/**
 * Created by nick on 2017/10/23.
 */
public interface Serialize {
    List<Entry> genEntryList();
    void updateFromEntryList(List<Entry> entries);
}