package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 9/26/12
 * Time: 3:06 PM
 */
public enum BillingStatus {

    NotYetBilled("Not Yet Billed"),
    EligibleForBilling("Eligible For Billing"),
    Billed("Billed"),
    NotBillable("Not Billable");

    private String displayName;

    private BillingStatus(final String displayName) {
        this.displayName = displayName;
    }

}
