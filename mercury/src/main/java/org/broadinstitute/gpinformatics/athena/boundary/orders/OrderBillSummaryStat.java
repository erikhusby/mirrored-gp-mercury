package org.broadinstitute.gpinformatics.athena.boundary.orders;

/**
 * This class holds the charges and credits for ledger entry groupings.
 */
public class OrderBillSummaryStat {

    private double charge;
    private double credit;

    public void applyDelta(double delta) {

        // If the delta is negative, then we want to INCREASE the credits a POSITIVE amount.
        if (delta < 0) {
            credit += -delta;
        } else {
            charge += delta;
        }
    }

    public double getCharge() {
        return charge;
    }

    public double getCredit() {
        return credit;
    }
}
