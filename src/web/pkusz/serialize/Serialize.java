package web.pkusz.serialize;

import java.util.List;

/**
 * Created by nick on 2017/10/23.
 */
/**
 Serialize接口是序列化定义接口。
 在zk中路径（键）关联值的逻辑类都应该实现本接口。
 接口的功能为将需要写入关联值的对象属性输出成为属性序列，或将属性序列更新到对象中。
 SerializeUtil工具类中可以将属性序列与字节序列之间相互转换，通过这个工具类，可以将具体的Serialize对象的属性与zk中的值做相互转换。
 */
public interface Serialize {
    List<Entry> genEntryList();
    void updateFromEntryList(List<Entry> entries);
}