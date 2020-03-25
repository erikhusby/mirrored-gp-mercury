/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum BillingTrigger implements Displayable {
    NONE("Manual Billing Only"),
    ADDONS_ON_RECEIPT("Bill Add-ons on Sample Receipt"),
    DATA_REVIEW("Bill at Data Review");

    private final String displayName;

    BillingTrigger(String displayName) {
        this.displayName = displayName;
    }

    public static Set<BillingTrigger> defaultValues() {
        return new HashSet<>(Collections.singleton(BillingTrigger.NONE));
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }


}

