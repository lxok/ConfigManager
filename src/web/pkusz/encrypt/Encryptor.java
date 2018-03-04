package web.pkusz.encrypt;

import jdk.internal.util.xml.impl.ReaderUTF8;
import org.apache.zookeeper.data.Stat;
import web.pkusz.data.DatabaseZK;
import web.pkusz.manage.ConfigManager;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by nick on 2017/11/12.
 */
/**
 Encryptor类是序列化数据加密器类，实现了对于序列化数据与密文之间的加解密过程，加密方式为AES。
 CM系统是依赖zk实现信息数据存储的，CM中的数据根据安全性需求可以由CM将数据加密再保存在zk中。
 Encryptor模块被组合在序列化工具SerializeUtil类中，在将数据序列化和反序列化时，将根据CM当前加密模式来选择是否使用加密器。
 加密模式在系统配置文件configmanager.properties中的encrypt属性被设置，若值为true，则开启，若值为false或空值，则不开启。

 在当前实现中，加密器的AES密钥被保存在zk的/mimic/server/key路径的值中。
 CM服务器的主备节点需要具有完全系统的配置，在CM中若开启加密模式，则zk中全部数据值都将被加密，CM服务器的备节点同样需要开启加密
 模式，否则备节点将无法正确得到信息数据而且会写入未加密数据，这样zk中将同时包含加密和未加密的数据，系统运行将产生错误。

 * 加密器与CM运行目前尚未经过完整测试，不建议在当前CM使用中开启加密模式。
 * 这是因为CM服务器主程序运行和加密器在zk中的原始信息初始化的顺序并未被完全验证。
 */
public class Encryptor {

    byte[] password;
    Cipher encryptCipher;
    Cipher decryptCipher;

    public boolean init(DatabaseZK db) {

        String keyPath = ConfigManager.SERVER + "/key";
        boolean needCreatePath = true;
        if (db.exists(keyPath) != null) {
            password = db.getData(keyPath, null);
            if (password != null && password.length != 0) {
                return true;
            }
            needCreatePath = false;
        }
        password = new Date().toString().getBytes();
        if (needCreatePath) {
            if (!db.create(keyPath, password)) {
                return false;
            }
        } else {
            Stat s = db.setData(keyPath, password, -1);
            if (s == null) {
                return false;
            }
        }

        //password = "password".getBytes(); //be used to test

        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(password));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            encryptCipher = Cipher.getInstance("AES");// 创建密码器
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
        } catch (Exception e) {
            //log
            System.out.println("EncryptCipher init fail.");
            System.out.println(e.toString());
            return false;
        }

        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(password));
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            decryptCipher = Cipher.getInstance("AES");// 创建密码器
            decryptCipher.init(Cipher.DECRYPT_MODE, key);// 初始化
        } catch (Exception e) {
            //log
            System.out.println("DecryptCipher init fail.");
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    public byte[] encrpyt(byte[] byteContent) {
        try {
            byte[] result = encryptCipher.doFinal(byteContent);
            return result; // 加密
        } catch (Exception e) {
            //log
            System.out.println("Encryptor encrypt data fail.");
            System.out.println(e.toString());
        }
        return null;
    }

    public byte[] decrypt(byte[] content) {
        try {
            byte[] result = decryptCipher.doFinal(content);
            return result; // 解密
        } catch (Exception e) {
            //log
            System.out.println("Encryptor decrypt data fail.");
            System.out.println(e.toString());
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        ArrayList<byte[]> n = new ArrayList<>();
        Encryptor en = new Encryptor();
        en.password = "key".getBytes();
        //en.init();
        for (int i = 0; i < 100; i++) {
            String value = "test" + i;
            byte[] res = en.encrpyt(value.getBytes());
            n.add(res);
        }

        for (int i = 0; i < n.size(); i++) {
            byte[] res = n.get(i);
            byte[] f = en.decrypt(res);
            ByteArrayInputStream bais = new ByteArrayInputStream(f);
            Reader r = new ReaderUTF8(bais);
            char[] c = new char[f.length];
            r.read(c);
            String yes = String.valueOf(c);
            System.out.println(yes);
        }
    }
}
