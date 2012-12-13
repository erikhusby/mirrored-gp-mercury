package org.broadinstitute.gpinformatics.athena.control;

public class ProductUtil {

    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;

    /**
     * Converts cycle times from days to seconds.
     * @return the number of seconds.
     */
    public static Integer convertCycleTimeDaysToSeconds(Integer cycleTimeDays) {
        Integer cycleTimeSeconds = null;
        if ( cycleTimeDays != null ) {
            cycleTimeSeconds = ( cycleTimeDays == null ? 0 : cycleTimeDays.intValue() * ONE_DAY_IN_SECONDS);
        }
        return cycleTimeSeconds;
    }

    /**
     * Converts cycle times from seconds to days.
     * This method rounds down to the nearest day
     * @param cycleTimeSeconds
     * @return the number of days.
     */
    public static Integer convertCycleTimeSecondsToDays(Integer cycleTimeSeconds) {
        Integer cycleTimeDays = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_DAY_IN_SECONDS) {
            cycleTimeDays =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_DAY_IN_SECONDS)) / ONE_DAY_IN_SECONDS;
        }
        return cycleTimeDays;
    }
}
