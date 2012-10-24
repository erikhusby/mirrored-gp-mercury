package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 9/26/12
 * Time: 3:06 PM
 */
public enum BillingStatus implements StatusType {
    NotYetBilled("Not Yet Billed"),
    EligibleForBilling("Eligible For Billing"),
    Billed("Billed"),
    NotBillable("Not Billable");

    private final String displayName;

    @Override
    public String getDisplayName() {
        return displayName;
    }

    private BillingStatus(String displayName) {
        this.displayName = displayName;
    }
}
