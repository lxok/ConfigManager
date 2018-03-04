package web.pkusz.ca;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;

/**
 * Created by craig on 2018/1/27.
 * 说明：获取CA的公钥
 */
public class RootCA {
    public static PublicKey getPublicKey() throws Exception {
        try{
            java.security.KeyStore keyStore = KeyStore.getInstance("JKS");;
            FileInputStream fileInputStream = new FileInputStream(EjbcaWeb.TRUSTSTORE_FILE_PATH);
            // 密码没有修改，直接是changeit
            keyStore.load(fileInputStream,"changeit".toCharArray());
            // 取对应的CA名称的公钥，暂时叫做test
            java.security.cert.Certificate certificate = keyStore.getCertificate("test");
            return certificate.getPublicKey();
        } catch (Exception e){
            throw new Exception("获取rootCA公钥出错："+e.toString());
        }
    }
}
