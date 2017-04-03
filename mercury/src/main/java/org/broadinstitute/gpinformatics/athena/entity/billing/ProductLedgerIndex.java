package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

/**
 * For the Transition to SAP, we must ensure that we are preserving the association of PriceItems to ledger entries
 * while at the same time introducing products as the association to the same ledger entries.  Historically our
 * references to ledger entries have been only price items
 */
public class ProductLedgerIndex {
    private Product product;
    private PriceItem priceItem;

    public ProductLedgerIndex(Product product, PriceItem priceItem) {
        this.product = product;
        this.priceItem = priceItem;
    }

    public Product getProduct() {
        return product;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProductLedgerIndex that = (ProductLedgerIndex) o;

        return new EqualsBuilder()
                .append(getProduct(), that.getProduct())
                .append(getPriceItem(), that.getPriceItem())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getProduct())
                .append(getPriceItem())
                .toHashCode();
    }
}
