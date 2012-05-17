package org.broadinstitute.sequel.entity.zims;

public class ThriftConversionUtil {

    public static Integer zeroAsNull(int number) {
        if (number == 0) {
            return null;
        }
        else {
            return new Integer(number);
        }
    }

    public static Double zeroAsNull(double number) {
        if (number == 0) {
            return null;
        }
        else {
            return new Double(number);
        }
    }
}
