/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bsp;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * A utility class for common BSP code.
 */
public class BSPUtil {
    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile("^S[MP]-[A-Z1-9]{4,6}$");

    public static final Pattern NO_SM_NAME_PATTERN = Pattern.compile("^[A-Z1-9]{4,6}$");

    /**
     * Tests if the sampleName is in a valid BSP format.
     * @param sampleName the name of the sample you are testing.
     *
     * @return true if the sample name is a valid BSP Sample name.
     */
    public static boolean isInBspFormat(@Nonnull String sampleName) {
        // SM-4FHTK and 4FHTK are equally valid sample ids
        return BSP_SAMPLE_NAME_PATTERN.matcher(sampleName).matches() || NO_SM_NAME_PATTERN.matcher(sampleName).matches();
    }
}
