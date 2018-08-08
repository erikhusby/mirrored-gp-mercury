package org.broadinstitute.gpinformatics.infrastructure.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalDouble;

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
    /**
     * Arrays call rates are scaled to 3 decimal places.
     */
    public static BigDecimal scaleThreeDecimalPlaces( BigDecimal input ) {
        if( input == null ) {
            return null;
        }
        return input.setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    public static boolean areTooFarApart(float lowerValue, float higherValue, float maxPercentageBetweenReads) {
        float percentDiff = (Math.abs(higherValue - lowerValue)) / ((higherValue + lowerValue) / 2);
        return percentDiff > maxPercentageBetweenReads;
    }

    public static OptionalDouble average(List<Float> vals) {
        return vals.stream().mapToDouble(val -> val).average();
    }
}
