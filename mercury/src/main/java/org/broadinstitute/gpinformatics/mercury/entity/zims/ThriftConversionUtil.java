package org.broadinstitute.gpinformatics.mercury.entity.zims;

public class ThriftConversionUtil {

    public static Integer zeroAsNull(int number) {
        if (number == 0) {
            return null;
        } else {
            return number;
        }
    }

    public static Double zeroAsNull(double number) {
        if (number == 0) {
            return null;
        } else {
            return number;
        }
    }
}
