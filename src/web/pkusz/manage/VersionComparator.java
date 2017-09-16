package web.pkusz.manage;

/**
 * Created by nick on 2017/8/6.
 */

/**
 * 为了便于运行过程中根节点数据更替不频繁，又要满足代码版本号可以体现更新，
 * 在本系统版本号中，我们规定“.”之前的为主版本号，之后的为副版本号。
 * 我们规定，当代码的数据库结构更新时，主版本号必须提高。
 * 而程序本身的功能更新，则可以按照实际情况提高主版本号或副版本号。
 *
 * 此类用于服务器在初始化后判断自身版本号与数据库版本号是否匹配，而进行对应的数据库更新工作。
 * 因此我们在比较方法中只比较主版本号。因为次版本号的变化不会影响数据库结构形式。
 */
public class VersionComparator {

    public static int compare(String v1, String v2) {
        /*
        int e1 = Integer.parseInt(v1.split("\\.")[0]);
        int e2 = Integer.parseInt(v2.split("\\.")[0]);
        int res = e1 - e2;
        return res > 0 ? 1 : (res == 0 ? 0 : -1);
        */
        return 1;
    }

    public static void main(String[] args) {
        String s1 = "1234";
        String s2 = "0.7236";
        int res = VersionComparator.compare(s2, s1);
        System.out.println(res);
    }
}
