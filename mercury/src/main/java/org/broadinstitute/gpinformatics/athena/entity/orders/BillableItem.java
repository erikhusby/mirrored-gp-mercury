package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.hibernate.envers.Audited;

import javax.persistence.*;
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
@Entity
@Audited
@Table(schema = "athena")
public class BillableItem {

    @Id
    @SequenceGenerator(name="SEQ_BILLABLE_ITEM", schema = "athena", sequenceName="SEQ_BILLABLE_ITEM")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="SEQ_BILLABLE_ITEM")
    private Long billableItemId;

    @OneToOne
    private PriceItem priceItem;
    private BigDecimal count = new BigDecimal("0");    //initialize to zero.

    BillableItem() {
    }

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

    public void setCount(final BigDecimal count) {
        this.count = count;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BillableItem)) return false;

        final BillableItem that = (BillableItem) o;

        if (count != null ? !count.equals(that.count) : that.count != null) return false;
        if (priceItem != null ? !priceItem.equals(that.priceItem) : that.priceItem != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = priceItem != null ? priceItem.hashCode() : 0;
        result = 31 * result + (count != null ? count.hashCode() : 0);
        return result;
    }

}
