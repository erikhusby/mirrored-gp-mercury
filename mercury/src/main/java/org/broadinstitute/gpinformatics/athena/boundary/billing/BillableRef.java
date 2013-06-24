package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class holds a particular billable item, which is a product (part number) and the price item name. Price
 * item names are unique per all products, replacement items and add ons. There are validation errors in place to
 * make sure this stays the case in the future.
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
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof BillableRef)) {
            return false;
        }

        BillableRef castOther = (BillableRef) other;
        return new EqualsBuilder()
                .append(productPartNumber, castOther.getProductPartNumber())
                .append(priceItemName, castOther.getPriceItemName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(productPartNumber).append(priceItemName).toHashCode();
    }

}

