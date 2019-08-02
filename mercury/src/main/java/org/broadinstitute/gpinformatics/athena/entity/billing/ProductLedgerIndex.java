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
    private boolean sapIndex = false;

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

    public String getDisplayValue() {
        String display = "";

        if(sapIndex) {
            display = product.getDisplayName();
        } else {
            display = priceItem.getDisplayName();
        }

        return display.toString();
    }

    public String getName() {
        String name = "";
        if(sapIndex) {
            name = product.getPartNumber();
        } else {
            name = priceItem.getName();
        }
        return name;
    }

    public Long getIndexId() {
        Long id;
        if(sapIndex) {
            id = priceItem.getPriceItemId();
        } else {
            id = product.getProductId();
        }
        return id;
    }

    public static ProductLedgerIndex create(Product product, PriceItem priceItem, boolean sapOrder) {
        ProductLedgerIndex ledgerIndex;
        if(sapOrder) {
            ledgerIndex = new ProductLedgerIndex(product, null);
        } else {
            ledgerIndex = new ProductLedgerIndex(null, priceItem);
        }
        ledgerIndex.sapIndex = sapOrder;
        return ledgerIndex;
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
