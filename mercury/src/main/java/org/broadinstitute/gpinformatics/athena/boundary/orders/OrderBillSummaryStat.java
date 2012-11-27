package org.broadinstitute.gpinformatics.athena.boundary.orders;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 11/21/12
 * Time: 1:04 PM
 */
public class OrderBillSummaryStat {

    private double charge=0;
    private double credit=0;

    public OrderBillSummaryStat() {
    }

    public double getCharge() {
        return charge;
    }
    public void addCharge(double charge) {
        this.charge = this.charge + charge;
    }

    public double getCredit() {
        return credit;
    }
    public void addCredit(double credit) {
        this.credit =  this.credit + credit;
    }

}
