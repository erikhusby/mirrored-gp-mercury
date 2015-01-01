/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2008 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.time.FastDateFormat;

import java.sql.Timestamp;
import java.text.Format;
import java.util.Date;

/**
 * Holds the return values from a constrained value expression.
 *
 * @author thompson
 */
public class ConstrainedValue implements Comparable<ConstrainedValue> {
    private final String code;

    private final String label;

    private static final Format mmDdYyyyFormat = FastDateFormat.getInstance("MM/dd/yyyy");

    public ConstrainedValue(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public ConstrainedValue(String label) {
        this(null, label);
    }

    public ConstrainedValue(Timestamp label) {
        this(null, mmDdYyyyFormat.format(new Date(label.getTime())));
    }

    public String getCode() {
        if (code == null) {
            return label;
        }
        return code;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int compareTo(ConstrainedValue o) {
        return label.compareTo(o.getLabel());
    }
}
