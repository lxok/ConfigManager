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
