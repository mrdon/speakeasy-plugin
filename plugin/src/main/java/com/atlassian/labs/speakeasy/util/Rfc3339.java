package com.atlassian.labs.speakeasy.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 */
public class Rfc3339
{

    /**
     * A date formatting object that does most of the formatting work for RFC3339.  Note that since
     * Java's SimpleDateFormat does not provide all the facilities needed for RFC3339 there is still
     * some custom code to finish the job.
     */
    private static DateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * A number formatting object to format the the timezone offset info in RFC3339 output.
     */
    private static NumberFormat doubleDigit = new DecimalFormat("00");

    public static String dateToRFC3339(Date d)
    {
        StringBuilder result = new StringBuilder(rfc3339.format(d));
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        cal.setTimeZone(TimeZone.getDefault());
        int offset_millis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        int offset_hours = Math.abs(offset_millis / (1000 * 60 * 60));
        int offset_minutes = Math.abs((offset_millis / (1000 * 60)) % 60);

        if (offset_millis == 0)
        {
            result.append("Z");
        }
        else
        {
            result
                    .append((offset_millis > 0) ? "+" : "-")
                    .append(doubleDigit.format(offset_hours))
                    .append(":")
                    .append(doubleDigit.format(offset_minutes));
        }

        return result.toString();
    }

}
