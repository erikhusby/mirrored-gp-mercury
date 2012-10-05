package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.math.BigDecimal;

/**
 * Class to contain billing info. It encapsulates
 * the priceItem and the number of times to bill for this billable item.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 11:31 AM
 */
public class BillableItem {

    private PriceItem priceItem;
    private BigDecimal count;

    public BillableItem(final PriceItem priceItem, final BigDecimal count) {
        this.priceItem = priceItem;
        this.count = count;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public void setPriceItem(final PriceItem priceItem) {
        this.priceItem = priceItem;
    }

    public BigDecimal getCount() {
        return count;
    }

    public void BigDecimal(final BigDecimal count) {
        this.count = count;
    }
}
