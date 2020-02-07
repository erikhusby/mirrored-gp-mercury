package org.broadinstitute.gpinformatics.infrastructure.common;

import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Allows Date classes to be used in JAXB DTOs, which makes them easier to work with in Stripes.
 */
public class DateAdapter {

    public static Date parseDate(String s) {
        return DatatypeConverter.parseDate(s).getTime();
    }

    public static String printDate(Date dt) {
        if (dt == null) {
            return null;
        } else {
            Calendar cal = new GregorianCalendar();
            cal.setTime(dt);
            return DatatypeConverter.printDateTime(cal);
        }
    }
}
