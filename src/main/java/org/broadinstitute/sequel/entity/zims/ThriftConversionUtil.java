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
}
