package gr.com.ist.commun.utils;

import java.util.Date;

import org.joda.time.DateTime;

public class DateUtils {
    @SuppressWarnings("deprecation")
    public static Integer getMonth(Date date) {
        if (date == null) {
            return null;
        }
        return date.getMonth() + 1;       
    }
    
    @SuppressWarnings("deprecation")
    public static Integer getYear(Date date) {
        if (date == null) {
            return null;
        }
        return date.getYear() + 1900;       
    }

    @SuppressWarnings("deprecation")
    public static Date adjustToEndOfDay(Date dt) {
        if (dt == null) {
            return null;
        }
        DateTime adjusted = new DateTime(dt.getYear() + 1900, dt.getMonth() + 1, dt.getDate(), 23, 59, 59, 999);
        return new Date(adjusted.getMillis());
    }

}
