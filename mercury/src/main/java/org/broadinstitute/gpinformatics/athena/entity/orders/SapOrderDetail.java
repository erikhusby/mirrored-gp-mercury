package org.broadinstitute.gpinformatics.athena.entity.orders;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Going forward Product Orders will be reflected in SAP as orders.  There are certain restrictions on what can change
 * and when in SAP during the lifecycle of a Product order.  To ensure Mercury is properly applying timely restrictions
 * on changing an order, this Entity was conceived.  There are different reasons for different properties of this
 * Entity:
 *
 * <ul>
 *     <li><b>Quote ID</b> -- Because certain finacial information on the SAP order cannot be changed, changes to the
 *     quote will require the creation of a new SAP order.  Storing the Quote ID with the current representation of the
 *     SAP order will allow us to compare requeste quote with previously selected quote and know if we need to create
 *     a new SAP order</li>
 *     <li><b>Order Number</b> -- As it seems, this is a logical way to save what the ordr number is with the order
 *     details</li>
 *     <li><b>Primary Quantity</b> -- Because of the limitations on updating SAP orders, discrepancies can occur
 *     between the actual quantity of the Product order, and the known quantities in the SAP order.   This will give us
 *     the ability to know if that discrepancy exists to allow us to target the logic accordingly.  This could manifest
 *     in knowing when to request an order to be closed, or when to allow child product orders to be created to replace
 *     samples</li>
 *     <li><b>Company Code</b> -- This is in part a safety measure to detect if a change to the Product order may alter
 *     the perceived company code with which the Product order will be associated (e.g. the product).  If that happens,
 *     we can prevent the attempt to make that change ahead of time.</li>
 * </ul>
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(name = "SAP_ORDER_DETAIL", schema = "athena")
public class SapOrderDetail implements Serializable, Updatable, Comparable<SapOrderDetail> {

    private static final long serialVersionUID = -4618988251536159923L;

    @Id
    @SequenceGenerator(name = "SEQ_SAP_ORDER_DETAIL", schema = "athena", sequenceName = "SEQ_SAP_ORDER_DETAIL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAP_ORDER_DETAIL")
    @Column(name = "SAP_ORDER_DETAIL_ID")
    private Long sapOrderDetailId;

    @Column(name = "SAP_ORDER_NUMBER")
    private String sapOrderNumber;

    @Embedded
    private UpdateData updateData = new UpdateData();

    @Column(name = "PRIMARY_QUANTITY")
    private int primaryQuantity;

    private String quoteId;

    private String companyCode;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "sapReferenceOrders")
    private Set<ProductOrder> referenceProductOrder = new HashSet<>();

    @OneToMany(mappedBy = "sapOrderDetail", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private Set<LedgerEntry> ledgerEntries = new HashSet<>();

    public SapOrderDetail() {
    }

    public SapOrderDetail(String sapOrderNumber, int primaryQuantity, String quoteId, String companyCode) {
        this.sapOrderNumber = sapOrderNumber;
        this.primaryQuantity = primaryQuantity;
        this.quoteId = quoteId;
        this.companyCode = companyCode;
    }

    public String getSapOrderNumber() {
        return sapOrderNumber;
    }

    public int getPrimaryQuantity() {
        return primaryQuantity;
    }

    public void setPrimaryQuantity(int primaryQuantity) {
        this.primaryQuantity = primaryQuantity;
    }

    public void addReferenceProductOrder(
            ProductOrder referenceProductOrder) {
        this.referenceProductOrder.add(referenceProductOrder);
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Set<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public void addLedgerEntries(Collection<LedgerEntry> ledgerEntries) {
        for (LedgerEntry ledgerEntry : ledgerEntries) {
            addLedgerEntry(ledgerEntry);
        }
    }

    public void addLedgerEntry(LedgerEntry ledgerEntry) {
        ledgerEntry.setSapOrderDetail(this);
        this.ledgerEntries.add(ledgerEntry);
    }

    public Map<Product, Double> getNumberOfBilledEntriesByProduct() {
        Map<Product, Double> billedCount = new HashMap<>();
        for (LedgerEntry ledgerEntry : this.ledgerEntries) {
            final ProductOrder productOrder = ledgerEntry.getProductOrderSample().getProductOrder();
            Product aggregatingProduct = null;

            if(hasLedgerMatch(productOrder, productOrder.getProduct().getPrimaryPriceItem(),
                    productOrder.getProduct(), ledgerEntry)) {
                aggregatingProduct = productOrder.getProduct();
            } else {
                for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
                    if(hasLedgerMatch(productOrder, productOrderAddOn.getAddOn().getPrimaryPriceItem(),
                            productOrderAddOn.getAddOn(), ledgerEntry)) {
                        aggregatingProduct = productOrderAddOn.getAddOn();
                        break;
                    }
                }
            }

            if (ledgerEntry.isBilled()) {
                double oldCount = billedCount.getOrDefault(aggregatingProduct, 0d);
                billedCount.put(aggregatingProduct, oldCount + ledgerEntry.getQuantity());
            }
        }

        return billedCount;
    }

    private boolean hasLedgerMatch(ProductOrder order, PriceItem priceItem, Product product, LedgerEntry ledgerEntry) {
        boolean ledgerIndexMatch = false;

        if(order.hasSapQuote()) {
            if (ledgerEntry.getProduct().equals(product)) {
                ledgerIndexMatch = true;
            }
        } else {
            if(ledgerEntry.getPriceItem().equals(priceItem)) {
                ledgerIndexMatch = true;
            }
        }

        return ledgerIndexMatch;
    }

    @Override
    public int compareTo(SapOrderDetail that) {

        CompareToBuilder compareToBuilder = new CompareToBuilder();

        compareToBuilder.append(updateData.getCreatedDate(), that.updateData.getCreatedDate());

        return compareToBuilder.build();
    }

    @Override
    public UpdateData getUpdateData() {
        return updateData;
    }

    public double getBilledSampleQuantity(final Product targetProduct) {
        final Iterable<LedgerEntry> billedSamplesByPriceItemFilter = Iterables
                .filter(getLedgerEntries(), new Predicate<LedgerEntry>() {

                    @Override
                    public boolean apply(@Nullable LedgerEntry ledgerEntry) {
                        return (ledgerEntry.getPriceItem() == null ||
                                Objects.equals(ledgerEntry.getPriceItem(),targetProduct.getPrimaryPriceItem())) &&
                               StringUtils.equals(ledgerEntry.getBillingMessage(),BillingSession.SUCCESS);
                    }
                });

        double billedSampleResult = 0d;
        for (LedgerEntry ledgerEntry : billedSamplesByPriceItemFilter) {
            billedSampleResult += ledgerEntry.getQuantity();
        }

        return billedSampleResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SapOrderDetail that = (SapOrderDetail) o;

        return new EqualsBuilder()
                .append(sapOrderNumber, that.sapOrderNumber)
                .append(referenceProductOrder, that.referenceProductOrder)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(sapOrderNumber)
                .append(referenceProductOrder)
                .toHashCode();
    }
}
