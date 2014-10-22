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

import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * A utility class for common BSP code.
 */
public class BSPUtil {
    private static final Pattern BSP_SAMPLE_SHORT_BARCODE_PATTERN = Pattern.compile("S[MP]-[A-Z1-9]{4,6}");
    private static final Pattern BSP_BARE_ID_BARCODE_PATTERN = Pattern.compile("[A-Z1-9]{4,6}");
    private static final Pattern CRSP_BSP_SAMPLE_SHORT_BARCODE_PATTERN = Pattern.compile("CS[MP]-[A-Z1-9]{4,6}");

    /**
     * Tests if the sampleName is in a valid BSP barcode format,
     * such as SM-4FHTK.  The "bare id" 4FHTK is not considered
     * to be in BSP barcode format.
     * @param sampleName the name of the sample you are testing.
     *
     * @return true if the sample name is a valid BSP Sample name.
     */
    public static boolean isInBspFormat(@Nonnull String sampleName) {
        return  BSP_SAMPLE_SHORT_BARCODE_PATTERN.matcher(sampleName).matches() ||
                (CRSP_BSP_SAMPLE_SHORT_BARCODE_PATTERN.matcher(sampleName).matches() && ApplicationInstance.CRSP.isCurrent());
    }

    /**
     * Tests if the sampleName could be a valid BSP barcode, such as SM-4FHTK.
     *
     * Bare IDs, e.g. 4FHTK, may or may not represent BSP samples. However, since they may represent parts extracted
     * from LSIDs, they can't be ruled out so this method returns true in these cases (unlike
     * {@link #isInBspFormat(String)}).
     *
     * @param sampleName    the name of the sample to check
     * @return true if the sample name could be a valid BSP barcode or bare sample ID (as possibly extracted from an LSID
     */
    public static boolean isInBspFormatOrBareId(@Nonnull String sampleName) {
        return isInBspFormat(sampleName) || BSP_BARE_ID_BARCODE_PATTERN.matcher(sampleName).matches();
    }
}
