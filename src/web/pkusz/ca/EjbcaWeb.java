package web.pkusz.ca;

import org.apache.commons.lang.time.DateUtils;
import org.cesecore.certificates.endentity.EndEntityConstants;
import org.cesecore.certificates.util.AlgorithmConstants;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.protocol.ws.client.gen.*;
import org.ejbca.core.protocol.ws.common.KeyStoreHelper;
import web.pkusz.manage.GlobalProperties;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * craig创建于2018-03-01
 * 说明：EjbcaWeb是主要的对外服务，包含对证书的增删改查。
 * 基本没有复杂的逻辑，都是对EJBCA本身的接口做一些封装，所以需要后续开发人员对着EJBCA的接口说明文件进行二次开发任务。
 * 其中校验证书的逻辑略复杂，还有一些额外的说明都在相应的文档里。
 */
public class EjbcaWeb {
    public static String TRUSTSTORE_FILE_PATH = "resources/ca/truststore.jks";
    public static String SUPERADMIN_FILE_PATH = "resources/ca/superadmin.p12";
    public static String CA_CERTS_PATH = "ca-certs";

    // 饿汉式单例
    private static EjbcaWeb ourInstance = new EjbcaWeb();
    // web service连接实例
    private EjbcaWS ejbcaWS = null;

    // 在这里做了初始化工作，不知道合适不？
    private EjbcaWeb() {
        this.ejbcaWS = this.getEjbcaWs();
    }

    public static EjbcaWeb getInstance() {
        return ourInstance;
    }

    // 获取web service连接
    public EjbcaWS getEjbcaWs() {
        System.out.println("get EjbcWs");
        CryptoProviderTools.installBCProvider();
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.keyStore", SUPERADMIN_FILE_PATH);
        System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");
        System.setProperty("javax.net.ssl.keyStorePassword", "ejbca");

        QName qname = new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSService");

        try {
            /*
            String caAddr = GlobalProperties.getGlobalProp().getProperty("ca_server");
            if (caAddr == null || caAddr.length() == 0) {
                caAddr = "ca.test.com";
            }
            */
            String caAddr = "ca.test.com";
            System.out.println("caAddr:" + caAddr);
            String url = "https://" + caAddr +  ":8443/ejbca/ejbcaws/ejbcaws?wsdl";
            EjbcaWSService service = new EjbcaWSService(new URL(url), qname);
            EjbcaWS ejbcaWS = service.getEjbcaWSPort();
            return ejbcaWS;
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    // 新增或者修改用户信息
    public X509Cert editUser(String username, String password,
                         String osinfo, String cpuinfo, String motherboard, String IP, String MAC) throws Exception {
        String UID = HashUID.getSHA1(osinfo, cpuinfo, motherboard, IP, MAC);
        String s = "CN=" + username + ",UID=" + UID;
        UserDataVOWS user = new UserDataVOWS();
        // 用户名
        user.setUsername(username);
        // 密码
        user.setPassword(password);
        user.setClearPwd(false);
        user.setSubjectDN(s);
        // 指定生成证书的CA
        user.setCaName("test");
        // 自定义证书有效期
        //证书有效起始日期
        user.setStartTime(DateUtil.formatDate(new Date()));
        //结束日期
        user.setEndTime(DateUtil.formatDate(DateUtils.addDays(new Date(), 100)));
        user.setEmail(null);
        user.setSubjectAltName(null);
        // 表示新增用户
        user.setStatus(EndEntityConstants.STATUS_NEW);
        user.setTokenType(UserDataVOWS.TOKEN_TYPE_P12);
        user.setEndEntityProfileName("EMPTY");
        user.setCertificateProfileName("ENDUSER");

        try {
            ejbcaWS.editUser(user);
            X509Cert x509Cert = this.createCert(username, password, CA_CERTS_PATH);
            return x509Cert;
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    // 校验证书，这里的逻辑略复杂，详细的内容可以查看相应的文档
    public boolean verifyCrt(X509Certificate x509Certificate, String username, String password,
                             String osinfo, String cpuinfo, String motherboard, String IP, String MAC) throws Exception {
        String UID = HashUID.getSHA1(osinfo, cpuinfo, motherboard, IP, MAC);
        String subjectDN = x509Certificate.getSubjectDN().toString();
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue(username);
        // 验证证书是否由我们的CA签发
        try {
            x509Certificate.verify(RootCA.getPublicKey());
        } catch (Exception e) {
            System.out.println("证书CA不匹配：" + e.toString());
            return false;
        }
        try {
            // 找到与username匹配的证书信息，并验证时间有效性和信息有效性
            // 验证证书序列号
            X509Certificate x = this.findCrt(username);
            if (!x.getSerialNumber().equals(x509Certificate.getSerialNumber())) return false;

            List<UserDataVOWS> users = ejbcaWS.findUser(usermatch);
            if (users != null && users.size() > 0) {
                // 只会有一个匹配的用户
                UserDataVOWS userDataVOWS = users.get(0);
                // 检查证书的subjectDN是否和用户名、ip等信息匹配，是否和服务器保存的结果匹配
                if (!subjectDN.contains(username)) return false;
                if (!subjectDN.contains(UID)) return false;
                if (!userDataVOWS.getSubjectDN().equals(subjectDN)) return false;

            } else {
                return false;
            }
        } catch (Exception e) {
            throw new Exception("获取用户信息出错：" + e.toString());
        }

        // 验证证书有效期
        try {
            x509Certificate.checkValidity(new Date());
        } catch (Exception e) {
            System.out.println("证书已失效：" + e.toString());
            return false;
        }
        return true;
    }
    // 撤销用户，即吊销证书。
    public boolean revokeUser(String username) throws Exception{
        try{
            ejbcaWS.revokeUser(username, RevokeStatus.REVOKATION_REASON_PRIVILEGESWITHDRAWN, true);
            return true;
        }catch (Exception e){
            throw new Exception("删除用户失败："+e.toString());
        }
    }
    // 检查用户名是否存在，目前还没有用上，但是感觉应该会有用的
    public boolean isExist(String username) throws Exception {
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue(username);
        try {
            List<UserDataVOWS> users = ejbcaWS.findUser(usermatch);
            if (users != null && users.size() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new Exception("检查用户 " + username + " 是否存在时出错：" + e.getMessage());
        }
    }
    // 根据用户名和密码创建证书，可以指定保存的路径
    private X509Cert createCert(String username, String password, String path) throws Exception {

        try {
            // 创建证书文件和私钥文件
            KeyStore ksenv = ejbcaWS.pkcs12Req(username, password, null, "2048", AlgorithmConstants.KEYALGORITHM_RSA);

            java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(), "PKCS12", password);
            X509Certificate x509Certificate = (X509Certificate) ks.getCertificate(username);
            PrivateKey privateKey = (PrivateKey) ks.getKey(username, password.toCharArray());
            this.exportCert(path + "/" + username + ".crt", x509Certificate);
            this.exportKey(path + "/" + username + ".key", privateKey);

            return new X509Cert(x509Certificate,privateKey);
        } catch (Exception e) {
            throw new Exception("用户  " + username + " 证书创建失败：" + e.getMessage());
        }

    }
    // 根据用户名查找证书
    private X509Certificate findCrt(String username) throws Exception {
        try {
            List<Certificate> cert = ejbcaWS.findCerts(username, true);

            if (cert != null && cert.size() > 0) {
                // 生成证书对象，只能形成文件流。对此，要先得到未编码的证书内容
                byte[] b = cert.get(0).getRawCertificateData();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(b);

                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

                X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream);

                return x509Certificate;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new Exception("查找证书出错：" + e.toString());
        }
    }
    // 将证书对象输出至文件，很烦的，一般这种文件都具有一定的格式
    private void exportCert(String path, X509Certificate x509Certificate) throws Exception {
        try {
            String s = Base64.getEncoder().encodeToString(x509Certificate.getEncoded());
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write("-----BEGIN CERTIFICATE-----\n");
            fileWriter.write(s);
            fileWriter.write("\n");
            fileWriter.write("-----BEGIN CERTIFICATE-----");
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    // 将密钥对象输出至文件，很烦的，一般这种文件都具有一定的格式
    private void exportKey(String path, PrivateKey privateKey) throws Exception {
        try {
            String s = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write("-----BEGIN PRIVATE KEY-----\n");
            fileWriter.write(s);
            fileWriter.write("\n");
            fileWriter.write("-----END PRIVATE KEY-----");
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}