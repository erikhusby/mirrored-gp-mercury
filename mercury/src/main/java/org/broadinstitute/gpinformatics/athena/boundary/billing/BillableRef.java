package org.broadinstitute.gpinformatics.athena.boundary.billing;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 11/26/12
 * Time: 4:00 PM
 */
public class BillableRef {

    private String productPartNumber;
    private String priceItemName;

    public BillableRef(String productPartNumber, String priceItemName) {
        this.productPartNumber = productPartNumber;
        this.priceItemName = priceItemName;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getPriceItemName() {
        return priceItemName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillableRef)) return false;

        final BillableRef that = (BillableRef) o;

        if (!priceItemName.equals(that.priceItemName)) return false;
        if (!productPartNumber.equals(that.productPartNumber)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = productPartNumber.hashCode();
        result = 31 * result + priceItemName.hashCode();
        return result;
    }
}

