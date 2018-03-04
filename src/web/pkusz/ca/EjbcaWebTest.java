package web.pkusz.ca;

/**
 * Created by craig on 2018/1/6.
 * 说明：EjbcaWeb的功能测试类
 * 下面3个try分别是整个EjbcaWeb暴露出来的3个接口的测试语句
 */
public class EjbcaWebTest {
    public static void main(String[] args) {

        EjbcaWeb ejbcaWeb = EjbcaWeb.getInstance();
        X509Cert x509Cert = null;
        try {
            x509Cert = ejbcaWeb.editUser("tester1", "123456", "linux", "amd", "hehe", "1.1.1.1", "abc123");
            System.out.println(x509Cert);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        try {
            Boolean b = ejbcaWeb.verifyCrt(x509Cert.getX509Cert(),"tester1", "123456", "linux", "amd", "hehe", "1.1.1.1", "abc123");
            System.out.println(b);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        try {
            Boolean b = ejbcaWeb.revokeUser("tester1");
            System.out.println(b);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }
}
