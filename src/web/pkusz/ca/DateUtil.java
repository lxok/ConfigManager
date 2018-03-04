package web.pkusz.ca;

import org.apache.commons.lang.time.FastDateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by craig on 2018/1/26.
 * 说明：时间Date和String之间相互转化的类，然而只用到了一半，另一半的实现还有点问题（互相不兼容）
 */
public class DateUtil {
    public static String formatDate(Date date) throws ParseException{
        // ISO 8601标准时间格式
        FastDateFormat fastDateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ssZZ");
        return fastDateFormat.format(date);
    }
    // 还没有用到这个，不知道会不会有用到
//    public static Date parse(String strDate) throws ParseException{
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZ");
//        return sdf.parse(strDate);
//    }
}
