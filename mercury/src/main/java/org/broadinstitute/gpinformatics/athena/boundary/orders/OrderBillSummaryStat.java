package org.broadinstitute.gpinformatics.athena.boundary.orders;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 11/21/12
 * Time: 1:04 PM
 */
public class OrderBillSummaryStat {

    private double charge;
    private double credit;

    public void applyDelta(double delta) {
        if (delta < 0) {
            credit += delta;
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
