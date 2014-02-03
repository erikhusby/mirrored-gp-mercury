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

import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

/**
 * This enum is used to link the material info types to kit types. In the UI when selecting the Kit type,
 * It will help populate the MaterialInfoDto types.
 *
 * name should map to KitTypeAllowanceSpecification in BSPCore
 *
 */
public enum KitType implements Displayable {

    /*
     FIXME:  This should not only represent other kit types but also more receptacles per kit type
      */
    DNA_MATRIX(KitTypeAllowanceSpecification.DNA_MATRIX_KIT, "Matrix Tube [0.75mL]");

    private final KitTypeAllowanceSpecification kitName;
    private final String displayName;

//    private String name;

    KitType(KitTypeAllowanceSpecification kitName, String displayName) {
        this.kitName = kitName;
        this.displayName = displayName;
//        name=name();
    }

    public String getKitName() {
        return kitName.getText();
    }

    public String getName() {
        return name();
    }


    @Override
    public String getDisplayName() {
        return displayName;
    }
}
