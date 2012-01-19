package org.broadinstitute.sequel;

/**
 * A quote, as per the quote server
 */
public interface Quote {

    public String getFundingSource();

    public String getCostObject();

    public String getPurchaseOrder();

    public void addWorkItem();

    public void updateWorkItem();

}
