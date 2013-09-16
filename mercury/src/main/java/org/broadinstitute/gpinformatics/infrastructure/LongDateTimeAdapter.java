package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LongDateTimeAdapter extends XmlAdapter<String, Date> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.LONG_DATE_TIME_MILLIS);

    @Override
    public String marshal(Date v) throws Exception {
        return dateFormat.format(v);
    }

    @Override
    public Date unmarshal(String v) throws Exception {
        return dateFormat.parse(v);
    }

}
