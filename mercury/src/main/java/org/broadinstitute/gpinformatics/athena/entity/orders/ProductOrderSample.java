package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class to describe Athena's view of a Sample. A Sample is identified by a sample Id and
 * a billableItem and an optionally comment which may be in most cases empty but on
 * occasion can actually have a value to describe "exceptions" that occur for a particular sample.
 *
 * @author mccrory
 */
@Entity
@Audited
@Table(name= "PRODUCT_ORDER_SAMPLE", schema = "athena")
public class ProductOrderSample implements Serializable {

    /** Count shown when no billing has occurred. */
    public static final Double NO_BILL_COUNT = 0d;

    @Id
    @SequenceGenerator(name = "SEQ_ORDER_SAMPLE", schema = "athena", sequenceName = "SEQ_ORDER_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDER_SAMPLE")
    private Long productOrderSampleId;

    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile("SM-[A-Z1-9]{4,6}");

    static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("Sample data not available");

    @Index(name = "ix_pos_sample_name")
    @Column(nullable = false)
    private String sampleName;      // This is the name of the BSP or Non-BSP sample.

    private BillingStatus billingStatus = BillingStatus.NotYetBilled;

    private String sampleComment;

    @Index(name = "ix_pos_product_order")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(insertable = false, updatable = false)
    private ProductOrder productOrder;

    @OneToMany(mappedBy = "productOrderSample", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<BillingLedger> ledgerItems = new HashSet<BillingLedger>();

    @Transient
    private BSPSampleDTO bspDTO = BSPSampleDTO.DUMMY;

    @Transient
    private boolean hasBspDTOBeenInitialized;

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(@Nonnull ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    ProductOrderSample() {
    }

    public ProductOrderSample(@Nonnull String sampleName) {
        this.sampleName = sampleName;
    }

    /**
     * Used for testing only.
     */
    public ProductOrderSample(@Nonnull String sampleName,
                              @Nonnull BSPSampleDTO bspDTO) {
        this.sampleName = sampleName;
        setBspDTO(bspDTO);
    }

    public String getSampleName() {
        return sampleName;
    }

    public BillingStatus getBillingStatus() {
        return billingStatus;
    }

    public void setBillingStatus(BillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }

    public String getSampleComment() {
        return sampleComment;
    }

    public void setSampleComment(String sampleComment) {
        this.sampleComment = sampleComment;
    }

    public boolean needsBspMetaData() {
        return isInBspFormat() && !hasBspDTOBeenInitialized;
    }

    /**
     * @return true if sample is a loaded BSP sample but BSP didn't have any data for it.
     */
    public boolean bspMetaDataMissing() {
        // Use == here, we want to match the exact object.
        //noinspection ObjectEquality
        return isInBspFormat() && hasBspDTOBeenInitialized && bspDTO == BSPSampleDTO.DUMMY;
    }

    public BSPSampleDTO getBspDTO() {
        if ( ! hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                // load BSP DTOs for all PDO samples in our PDO if we have a PDO
                if (productOrder != null) {
                    productOrder.loadBspData();
                } else {
                    BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
                    bspDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(getSampleName());
                }
            } else {
                // not BSP format, but we still need a semblance of a BSP DTO
                bspDTO = BSPSampleDTO.DUMMY;
            }
            hasBspDTOBeenInitialized = true;
        }
        return bspDTO;
    }

    public Set<BillingLedger> getBillableItems() {
        return ledgerItems;
    }

    public Long getProductOrderSampleId() {
        return productOrderSampleId;
    }

    public void setBspDTO(@Nonnull BSPSampleDTO bspDTO) {
        if (bspDTO == null) {
            throw new NullPointerException("BSP Sample DTO cannot be null");
        }
        this.bspDTO = bspDTO;
        hasBspDTOBeenInitialized = true;
    }

    public boolean isInBspFormat() {
        return isInBspFormat(sampleName);
    }

    public static boolean isInBspFormat(@Nonnull String sampleName) {
        if (sampleName == null) {
            throw new NullPointerException("Sample name cannot be null");
        }

        return BSP_SAMPLE_NAME_PATTERN.matcher(sampleName).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductOrderSample)) {
            return false;
        }

        ProductOrderSample that = (ProductOrderSample) o;
        return new EqualsBuilder().append(sampleName, that.sampleName).append(billingStatus, that.billingStatus)
                .append(sampleComment, that.sampleComment)
                .append(productOrder, that.productOrder).append(bspDTO, that.bspDTO).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(sampleName).append(billingStatus)
                .append(sampleComment).append(productOrder).append(bspDTO).build();
    }

    /**
     * This class holds the billed and uploaded ledger counts for a particular pdo and price item
     */
    public static class LedgerQuantities {
        private Double billed = NO_BILL_COUNT;   // If nothing is billed yet, then the total is still 0.
        private Double uploaded = null;          // If nothing has been uploaded, we want to just ignore this for upload

        public void addToBilled(Double quantity) {
            billed += quantity;
        }

        public void addToUploaded(Double quantity) {

            // Should only be one quantity uploaded at any time
            if (uploaded != null) {
                throw new IllegalStateException("Should only have one quantity being uploaded for this price item and PDO Sample");
            }

            // so, no adding needed
            uploaded = quantity;
        }

        public String getBilled() {
            return billed.toString();
        }

        public String getUploaded() {
            if (uploaded == null) {
                return getBilled();
            }

            return getBilled() + uploaded.toString();
        }
    }

    public static Map<PriceItem, LedgerQuantities> getLedgerQuantities(ProductOrderSample sample) {
        Set<BillingLedger> ledgerItems = sample.getBillableItems();
        if (ledgerItems.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<PriceItem, LedgerQuantities> sampleStatus = new HashMap<PriceItem, LedgerQuantities>();
        for (BillingLedger item : ledgerItems) {
            if (!sampleStatus.containsKey(item.getPriceItem())) {
                sampleStatus.put(item.getPriceItem(), new LedgerQuantities());
            }

            if (item.getBillingSession() != null) {
                sampleStatus.get(item.getPriceItem()).addToBilled(item.getQuantity());
            } else {
                sampleStatus.get(item.getPriceItem()).addToUploaded(item.getQuantity());
            }
        }

        return sampleStatus;
    }
}