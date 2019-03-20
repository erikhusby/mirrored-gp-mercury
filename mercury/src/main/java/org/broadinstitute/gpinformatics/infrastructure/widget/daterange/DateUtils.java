/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2004 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.widget.daterange;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/*
 * This class is a utility class for various date functions.
 * 
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class DateUtils {
    public static final long MILLISECONDS_IN_DAY = 86400000;

    private static final Log log = LogFactory.getLog(DateUtils.class);

    private static final String DATE_PATTERN = "MM/dd/yyyy";

    private static final String TIME_PATTERN = DATE_PATTERN + " hh:mm a";

    private static final String DATE_PATTERN_YY = "MM/dd/yy";

    private static final String DATE_PATTERN_YY_DASH = "MM-dd-yy";

    private static final String DATE_TIME_PATTERN_YY_DASH = "yy-MM-dd hh:mm a";

    private static final String DATE_TIME_PATTERN_YYYYMMDD_DASH = "yyyy-MMM-dd hh:mm a";

    public static final String LONG_DATE_TIME_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    // private static String TIME_PATTERN_DASH = DATE_PATTERN_DASH + " HH:MM a";

    /** Cached default date format for performance. */
    public static final Format defaultDateFormat = FastDateFormat.getInstance(DATE_PATTERN);

    /** Cached default date/time format for performance. */
    public static final Format defaultTimeFormat = FastDateFormat.getInstance(TIME_PATTERN);

    /** Cached default year-first date/time format for performance. */
    public static final Format dateTimeFormat = FastDateFormat.getInstance(DATE_TIME_PATTERN_YY_DASH);

    public static final FastDateFormat yyyymmmdddDateTimeFormat = FastDateFormat.getInstance(DATE_TIME_PATTERN_YYYYMMDD_DASH);

    /**
     * Return default DATE_PATTERN (MM/dd/yyyy)
     * 
     * @return a string representing the date pattern on the UI
     */
    public static String getDatePattern() {
        return DATE_PATTERN;
    }

    public static String getDateTimePattern() {
        return TIME_PATTERN;
    }

    /**
     * This method attempts to convert a Date object into a string predetermined and
     * formatted string in the form MM/dd/yyyy.
     * 
     * @param inDate The input date object
     * @return A formatted string representation of the Date in the form MM/dd/yyyy
     */
    public static String getDate(Date inDate) {
        if (inDate == null) {
            return "";
        }

        return defaultDateFormat.format(inDate);
    }

    /**
     * This method generates a string representation of a date/time in the format you
     * specify on input
     * 
     * @param aMask the date pattern the string is in
     * @param strDate a string representation of a date
     * @return A converted Date object
     * @see java.text.SimpleDateFormat
     * @throws java.text.ParseException on parse exception
     */
    public static Date convertStringToDate(String aMask, String strDate)
            throws ParseException {
        Date date;
        SimpleDateFormat df = new SimpleDateFormat(aMask);

        try {
            date = df.parse(strDate);
        } catch (ParseException pe) {
            // log.error("ParseException: " + pe);
            throw new ParseException(pe.getMessage(), pe.getErrorOffset());
        }

        return (date);
    }

    /**
     * Convert from a util.Date to a sql.Timestamp for Oracle operations.
     *
     * @param inDate A util.Date object
     * @return A date of type java.sql.Timestamp
     */
    public static java.sql.Timestamp convertDateToSQLTimestamp(Date inDate) {
        return (new java.sql.Timestamp(inDate.getTime()));
    }

    /**
     * This method attempts to convert a Date object into a string pretermined and
     * formatted string in the form MM/dd/yyyy hh:mm a"
     *
     * @param theTime The input date object
     * @return A formatted string representation of the Date
     *
     */
    public static String getYearDateTime(Date theTime) {
        if (theTime == null) {
            return "";
        }

        return dateTimeFormat.format(theTime);
    }

    /**
     * This method attempts to convert a Date object into a string pretermined and
     * formatted string in the form yyyy-MMM-dd hh:mm a"
     *
     * @param theTime The input date object
     * @return A formatted string representation of the Date
     *
     */
    public static String getYYYYMMMDDTime(Date theTime) {
        if (theTime == null) {
            return "";
        }

        return yyyymmmdddDateTimeFormat.format(theTime);
    }

    /**
     * This method attempts to convert a Date object into a string pretermined and
     * formatted string in the form MM/dd/yyyy hh:mm a"
     *
     * @param theTime The input date object
     * @return A formatted string representation of the Date
     *
     */
    public static String getDateTime(Date theTime) {
        if (theTime == null) {
            return "";
        }

        return defaultTimeFormat.format(theTime);
    }

    /**
     * This method generates a string representation of a date's date/time in the format
     * you specify on input
     *
     * @param aMask the date pattern the string is in
     * @param aDate a date object
     * @return a formatted string representation of the date
     *
     * @see java.text.SimpleDateFormat
     */
    public static String getDateTime(String aMask, Date aDate) {
        SimpleDateFormat df;
        String returnValue = "";

        if (aDate == null) {
            log.error("The date to convert into a string, getDateTime(), is null!");
        } else {
            df = new SimpleDateFormat(aMask);
            returnValue = df.format(aDate);
        }

        return (returnValue);
    }

    /**
     * This method generates a string representation of a date based on the System
     * Property 'dateFormat' in the format you specify on input
     *
     * @param aDate A date to convert
     * @return a string representation of the date
     */
    public static String convertDateToString(Date aDate) {
        return getDate(aDate);
    }

    /**
     * This method generates a string representation of a date based on the System
     * Property 'timestampFormat' in the format you specify on input
     *
     * @param aDate A date to convert
     * @return a string representation of the date
     */
    public static String convertDateTimeToString(Date aDate) {
        return getDateTime(aDate);
    }

    /**
     * This method generates a string representation of a date based on the System
     * Property 'dateFormat' in the format you specify on input
     *
     * @param aDate A date to convert
     * @param aMask The mask
     *
     * @return a string representation of the date
     */
    public static String convertDateToString(String aMask, Date aDate) {
        if (aMask == null) {
            return getDate(aDate);
        }

        return getDateTime(aMask, aDate);
    }

    /**
     * This method converts a String to a date using the DATE_PATTERN_YY and if that
     * fails, then it also tries to convert it with the DATE_PATTERN_YY_DASH.
     *
     * Note this method was changed to use the 'yy' year formatter. if you use the 'yyyy'
     * then 3/12/07 is treated as 3/12/0007. 'yy' handles 3/12/07 and 3/12/2007 properly.
     * (it will cause problems if 3/12/007 is input...)
     *
     * @param strDate the date to convert (in format MM/dd/yyyy or MM-dd-yyyy)
     * @return a date object
     *
     * @throws java.text.ParseException on parse exception
     */
    public static Date convertStringToDateTime(String strDate) throws ParseException {
        Date aDate;

        try {
            aDate = convertStringToDate(TIME_PATTERN, strDate);
        } catch (ParseException pe) {
            try {
                aDate = convertStringToDate(DATE_PATTERN, strDate);
            } catch (ParseException pe2) {
                throw new ParseException(pe2.getMessage(), pe2.getErrorOffset());
            }
        }

        return aDate;
    }

    /**
     * This method converts a String to a date using the DATE_PATTERN_YY and if that
     * fails, then it also tries to convert it with the DATE_PATTERN_YY_DASH.
     *
     * Note this method was changed to use the 'yy' year formatter. if you use the 'yyyy'
     * then 3/12/07 is treated as 3/12/0007. 'yy' handles 3/12/07 and 3/12/2007 properly.
     * (it will cause problems if 3/12/007 is input...)
     *
     * @param strDate the date to convert (in format MM/dd/yyyy or MM-dd-yyyy)
     * @return a date object
     *
     * @throws java.text.ParseException on parse exception
     */
    public static Date convertStringToDate(String strDate) throws ParseException {
        Date aDate = null;
        boolean isGood = false;

        if ((null == strDate) || (0 == strDate.length())) {
            return aDate;
        }

        try {
            aDate = convertStringToDate(DATE_PATTERN_YY, strDate);
            isGood = true;
        } catch (ParseException pe) {
            // still one other format to try
        }

        if (!isGood) {
            try {
                aDate = convertStringToDate(DATE_PATTERN_YY_DASH, strDate);
            } catch (ParseException pe) {
                // log.warn("Could not convert '" + strDate + "' to a date, throwing
                // exception");
                throw new ParseException(pe.getMessage(), pe.getErrorOffset());
            }
        }

        return aDate;
    }

    /**
     * Format the given date in an ISO 8601 compliant format.
     *
     * The format is yyyy-MM-ddThh:mm:ss+/-hh:mm. The suffix indicates the time zone
     * offset ahead or behind UTC. For example: "1994-11-05T08:15:30-05:00" corresponds to
     * November 5, 1994, 8:15:30 am, US Eastern Standard Time.
     *
     * This format is typically used in XML and W3C standards. This method produces a
     * local time with time zone offset (as opposed to a UTC format). See the ISO 8601
     * standard for details.
     *
     * This method uses the local time format with the thought that it is somewhat easier
     * for people (as opposed to programs) to understand and deal with.
     *
     * @param date The date to format.
     * @return String representing this date in ISO 8601 format.
     */
    public static String formatISO8601DateTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Calendar calendar = format.getCalendar();
        calendar.setTime(date);
        int offset = calendar.get(Calendar.ZONE_OFFSET)
                + calendar.get(Calendar.DST_OFFSET);
        StringBuilder builder = new StringBuilder();
        builder.append(format.format(date));
        if (offset != 0) {
            // Format time zone offset from UTC,
            // E.g. "-05:00" for Eastern Standard Time.
            if (offset > 0) {
                builder.append('+');
            } else {
                builder.append('-');
                offset = -offset;
            }
            int minutes = offset / (1000 * 60);
            int hours = minutes / 60;
            minutes = minutes % 60;
            if (hours < 10) {
                builder.append('0');
            }
            builder.append(hours);
            builder.append(':');
            if (minutes < 10) {
                builder.append('0');
            }
            builder.append(minutes);
        }
        return builder.toString();
    }

    /**
     * Format a date into a string in the general time zone format. This looks like this:
     * Wed, 4 Jul 2001 12:08:56 -0700
     *
     * @param date The date to format
     * @return The formatted string
     */
    public static String formatGeneralTimeZone(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        return format.format(date);
    }

    /**
     * Format the given date in an ISO 8601 compliant format.
     *
     * The format is yyyy-MM-dd
     *
     * @param date The date to format.
     * @return String representing this date in ISO 8601 format.
     */
    public static String formatISO8601Date(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);
    }

    /**
     * Format the given date in an ISO 8601 compliant format.
     *
     * The format is yyyyMMdd
     *
     * @param date The date to format.
     * @return String representing this date in ISO 8601 format.
     */
    public static String formatISO8601DateBasic(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(date);
    }

    /**
     * Parse the given date in an ISO 8601 compliant format.
     *
     * The format is yyyy-MM-dd
     *
     * @param mask The date pattern to use
     * @param date The date string to parse
     * @return Date
     * @throws java.text.ParseException on parse exception
     */
    public static Date parseDate(String mask, String date) throws ParseException {
        if (mask == null) {
            return parseDate(date);
        }

        return new SimpleDateFormat(mask).parse(date);
    }

    /**
     * Parse the given date in an ISO 8601 compliant format.
     *
     * The format is yyyy-MM-dd
     *
     * @param date The date string to parse
     * @return Date
     * @throws java.text.ParseException on parse exception
     */
    public static Date parseDate(String date) throws ParseException {
        return new SimpleDateFormat(DATE_PATTERN).parse(date);
    }

    /**
     * Parse the given date in an ISO 8601 compliant format.
     *
     * The format is yyyy-MM-dd
     *
     * @param date The date string to parse
     * @return Date
     * @throws java.text.ParseException on parse exception
     */
    public static Date parseISO8601Date(String date) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").parse(date);
    }

    /**
     *
     * @param date the
     * @return Date
     * @throws java.text.ParseException on parse exception
     */
    public static Date parseISO8601DateTime(String date) throws ParseException {
        DateFormat df;

        // 2001-12-17T09:30:47.0Z
        // GET RID OF Z?
        if (date.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z")) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            date = date.replace("Z", "");
        }
        // yyyy-MM-ddThh:mm:ss+/-hh:mm
        // 2001-12-17T09:30:47+13:59
        // GET RID OF THE LAST :
        else if (date
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[\\+\\-]\\d{2}:\\d{2}")) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            date = date.substring(0, 22) + date.substring(23);
        }
        // 2005-06-23T19:11:04.692051-04:00
        else if (date
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+[\\+\\-]\\d{2}:\\d{2}")) {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
            int idx = date.lastIndexOf(":");
            date = date.substring(0, idx) + date.substring(idx + 1);
        } else {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
        }
        // System.out.println(" call(2):\t"+date);
        return df.parse(date);
    }

    /**
     * Add a number of days, months, years, etc to a date.
     * 
     * @param date The date
     * @param interval The interval
     * @param amount The amount
     * @return The date
     */
    public static Date addToDate(Date date, int interval, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(interval, amount);
        return cal.getTime();
    }

    public static Date getOneWeek() {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        return cal.getTime();
    }

    public static Date getTwoWeeks() {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.WEEK_OF_YEAR, 2);
        return cal.getTime();
    }

    /**
     * Get the date "monthCount" ago or until now.
     * @param monthCount how many months ahead or behinde (negative number)
     * @return
     */
    public static Date getByMonthOffset(int monthCount) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, monthCount);
        return calendar.getTime();
    }

    /**
     * Get the Date for the start of the day.
     * 
     * @return first possible millisecond of the day
     */
    public static Date getStartOfDay() {
        return getStartOfDay(null);
    }

    /**
     * Get the Date for the end of the day.
     * 
     * @return last possible millisecond of the day
     */
    public static Date getEndOfDay() {
        return getEndOfDay(null);
    }

    /**
     * Get the Date for the end of the day.
     * 
     * @param date a Date, or null for current Date
     * @return last possible millisecond of the day
     */
    public static Date getEndOfDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 23);
        calendar.set(GregorianCalendar.MINUTE, 59);
        calendar.set(GregorianCalendar.SECOND, 59);
        calendar.set(GregorianCalendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     * Get the Date for the start of the day.
     * 
     * @param date a Date, or null for current Date
     * @return first possible millisecond of the day
     */
    public static Date getStartOfDay(Date date) {
        if (date == null) {
            date = new Date();
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * Returns true if the two dates are within a specified date range defined by a time
     * interval. The difference is computed by subtracting the second date from the first
     * and comparing it to our default.
     * 
     * @param first the initial date time
     * @param second the date time after the initial
     * @param timeInterval is the time period used for the comparison of dates
     * @return
     */
    public static boolean happenedRecently(Date first, Date second, long timeInterval) {
        return (first.getTime() + timeInterval) <= second.getTime();
    }

    /**
     * Returns the number of days between the two dates.  First it zeroes outs the time so it compares midnight to midnight. 
     *
     * @param startDate Earlier date to compare
     * @param endDate Later date to compare
     */
    public static long getNumDaysBetween(Date startDate, Date endDate) {

        if (org.apache.commons.lang3.time.DateUtils.isSameDay(startDate, endDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(startDate.toInstant(),endDate.toInstant());
    }

    public static Date parseXmlDate(String s) {
        return DatatypeConverter.parseDate(s).getTime();
    }
    public static String printXmlDate(Date dt) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(dt);
        return DatatypeConverter.printDate(cal);
    }
}
