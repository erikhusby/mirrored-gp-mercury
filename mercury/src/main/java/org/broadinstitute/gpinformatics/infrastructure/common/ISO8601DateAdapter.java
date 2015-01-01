package org.broadinstitute.gpinformatics.infrastructure.common;


import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>ISO8601DateAdapter</code> is an {@link XmlAdapter} implementation that
 * (un)marshals dates between <code>String</code> and <code>Date</code> representations.
 * All date strings meet <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO
 * 8601</a> basic format. For example, June 16, 2011 16:46:01 GMT is
 * "20110616164601Z". Adapted from
 * http://blogs.oracle.com/CoreJavaTechTips/entry/exchanging_data_with_xml_and
 */

public class ISO8601DateAdapter extends XmlAdapter<String, Date> {

    private static Logger logger = Logger
            .getLogger(ISO8601DateAdapter.class.getName());
    private SimpleDateFormat format;

    public ISO8601DateAdapter() {
        format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(Calendar.getInstance().getTimeZone());
    }

    @Override
    public String marshal(Date d) throws Exception {
        try {
            return format.format(d);
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    String.format("Failed to format date %s", d.toString()), e);
            return null;
        }
    }

    @Override
    public Date unmarshal(String d) throws Exception {

        if (d == null) {
            return null;
        }

        try {
            return format.parse(d);
        } catch (ParseException e) {
            logger.log(Level.WARNING,
                    String.format("Failed to parse string %s", d), e);
            return null;
        }
    }

}
