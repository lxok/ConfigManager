package web.pkusz.ca;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Created by craig on 2018/2/3.
 * 说明：包含X509证书和其密钥的对象，方便EjbcaWeb在新增或者修改用户信息后直接获取这两个对象
 */
public class X509Cert {
    // 变量类型为X509Certificate
    X509Certificate x509Certificate;
    // 变量类型为PrivateKey
    PrivateKey privateKey;
    // 构造方法
    public X509Cert(X509Certificate x509Certificate, PrivateKey privateKey){
        this.x509Certificate = x509Certificate;
        this.privateKey = privateKey;
    }
    // 获取证书
    public X509Certificate getX509Cert() {
        return this.x509Certificate;
    }
    // 获取密钥
    public PrivateKey getPrivateKey(){
        return this.privateKey;
    }
}
