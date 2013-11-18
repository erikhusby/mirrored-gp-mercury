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

package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

/**
 * This enum is used to link the material info types to kit types. In the UI when selecting the Kit type,
 * It will help populate the MaterialInfo types.
 *
 * name should map to KitTypeAllowanceSpecification in BSPCore
 *
 */
public enum KitType implements Displayable {
    DNA_MATRIX("DNA Matrix Kit", "Matrix Tube [0.75mL]");

    private final String kitName;
    private final String displayName;

    KitType(String kitName, String displayName) {
        this.kitName = kitName;
        this.displayName = displayName;
    }

    public String getKitName() {
        return kitName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
