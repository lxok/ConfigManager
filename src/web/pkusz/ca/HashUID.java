package web.pkusz.ca;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by craig on 2018/1/20.
 * 说明：对硬件信息取hash
 */
public class HashUID {
    public static String getSHA1(String osinfo, String cpuinfo, String motherboard, String IP, String MAC) throws Exception {
        try {
            String str =  osinfo + cpuinfo + motherboard + IP + MAC;
            // 生成一个SHA1加密计算摘要
            MessageDigest md = MessageDigest.getInstance("SHA1");
            // 计算SHA1函数
            md.update(str.getBytes());
            // digest()最后确定返回SHA1 hash值，返回值为8为字符串。因为SHA1 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            throw new Exception("hash出现错误:"+e.toString());
        }
    }
}
