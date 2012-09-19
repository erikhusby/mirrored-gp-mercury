package org.broadinstitute.gpinformatics.athena.infrastructure.bsp;

import java.text.NumberFormat;

public enum BSPSampleSearchResultFormatter {
    NONE,
    TWO_DECIMAL_PLACES,
    UG_TO_NG_WITH_TWO_DECIMAL_PLACES,
    FINGERPRINT;

    private static NumberFormat numberFormat = NumberFormat.getInstance();

    static {
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setGroupingUsed(false);
    }

    public String format(String data) {
        String ret = null;
        switch (this) {
        case NONE:
            ret = data;
            break;
        case UG_TO_NG_WITH_TWO_DECIMAL_PLACES:
            ret = "" + Double.valueOf(data) * 1000;
            ret = numberFormat.format(Double.valueOf(ret));
            break;
        case TWO_DECIMAL_PLACES:
            ret = numberFormat.format(Double.valueOf(data));
            break;
        case FINGERPRINT:
            ret = (data == null || "".equals(data)) ? "false" : "true";
            break;            

        default:
            throw new RuntimeException("format not found: " + this);
        }
        return ret;

    }

}
