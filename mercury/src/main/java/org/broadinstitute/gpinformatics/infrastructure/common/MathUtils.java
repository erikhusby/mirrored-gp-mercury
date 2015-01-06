package org.broadinstitute.gpinformatics.infrastructure.common;

import java.math.BigDecimal;

/**
 * A collection of utility functions for working with numbers.
 */
public class MathUtils {

    /**
     * Test two numbers and return true if they are the same.  This takes into account the inherent lack of
     * precision in the way floating point numbers are represented in Java.
     *
     * @see <a href="http://www.ibm.com/developerworks/java/library/j-math2/index.html">Java's new math,
     * Part 2: Floating-point numbers</a>
     * @return true if the numbers are the same
     */
    public static boolean isSame(double actual, double expected) {
        return Double.compare(actual, expected) == 0
               || (Math.abs(actual - expected) <= 5 * Math.ulp(expected));
    }

    /**
     * Test two numbers and return true if they are the same.  This takes into account the inherent lack of
     * precision in the way floating point numbers are represented in Java.
     *
     * @see <a href="http://www.ibm.com/developerworks/java/library/j-math2/index.html">Java's new math,
     * Part 2: Floating-point numbers</a>
     * @return true if the numbers are the same
     */
    public static boolean isSame(float actual, float expected) {
        return Float.compare(actual, expected) == 0
               || (Math.abs(actual - expected) <= 5 * Math.ulp(expected));
    }

    /**
     * Mercury uses a (semi) standard scaling of BigDecimal values to 2 decimal places.
     */
    public static BigDecimal scaleTwoDecimalPlaces( BigDecimal input ) {
        if( input == null ) {
            return null;
        }
        return input.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
