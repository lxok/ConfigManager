package web.pkusz.ca;

/**
 * Created by craig on 2018/1/27.
 * 说明：RootCA的测试类，就是看一下结果对不对
 */
public class RootCATest {
    public static void main(String[] args){
        try {
            System.out.println(RootCA.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
