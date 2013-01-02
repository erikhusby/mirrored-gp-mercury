package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
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
    public static final double NO_BILL_COUNT = 0;
    public static final String TUMOR_IND = BSPSampleDTO.TUMOR_IND;
    public static final String NORMAL_IND = BSPSampleDTO.NORMAL_IND;
    public static final String FEMALE_IND = BSPSampleDTO.FEMALE_IND;
    public static final String MALE_IND = BSPSampleDTO.MALE_IND;
    public static final String ACTIVE_IND = BSPSampleDTO.ACTIVE_IND;

    @Id
    @SequenceGenerator(name = "SEQ_ORDER_SAMPLE", schema = "athena", sequenceName = "SEQ_ORDER_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDER_SAMPLE")
    private Long productOrderSampleId;

    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile("SM-[A-Z1-9]{4,6}");

    static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("Sample data not available");

    @Index(name = "ix_pos_sample_name")
    @Column(nullable = false)
    private String sampleName;      // This is the name of the BSP or Non-BSP sample.

    private String sampleComment;

    @Index(name = "ix_pos_product_order")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(insertable = false, updatable = false)
    private ProductOrder productOrder;

    @OneToMany(mappedBy = "productOrderSample", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<BillingLedger> ledgerItems = new HashSet<BillingLedger>();

    @Column(name="SAMPLE_POSITION", updatable = false, insertable = false, nullable=false)
    private Integer samplePosition;

    public static enum DeliveryStatus implements StatusType {
        NOT_STARTED("Not Started"),
        DELIVERED("Delivered"),
        ABANDONED("Abandoned");

        private String displayName;

        DeliveryStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.NOT_STARTED;

    @Transient
    private BSPSampleDTO bspDTO = BSPSampleDTO.DUMMY;

    @Transient
    private boolean hasBspDTOBeenInitialized;

    @Transient
    private final Log log = LogFactory.getLog(ProductOrderSample.class);

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

    public String getSampleComment() {
        return sampleComment;
    }

    public void setSampleComment(String sampleComment) {
        this.sampleComment = sampleComment;
    }

    public boolean needsBspMetaData() {
        return isInBspFormat() && !hasBspDTOBeenInitialized;
    }

    public Integer getSamplePosition() {
        return samplePosition;
    }

    public void setSamplePosition(Integer samplePosition) {
        this.samplePosition = samplePosition;
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
        if (!hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
                bspDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(getSampleName());
            } else {
                // not BSP format, but we still need a semblance of a BSP DTO
                bspDTO = BSPSampleDTO.DUMMY;
            }
            hasBspDTOBeenInitialized = true;
        }
        return bspDTO;
    }

    public Set<BillingLedger> getLedgerItems() {
        return ledgerItems;
    }

    public Long getProductOrderSampleId() {
        return productOrderSampleId;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
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
        return new EqualsBuilder().append(sampleName, that.getSampleName()).append(samplePosition, that.getSamplePosition())
                .append(productOrder, that.getProductOrder()).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(sampleName).append(samplePosition).append(productOrder).build();
    }

    public Set<BillingLedger> getBillableLedgerItems() {
        Set<BillingLedger> billableLedgerItems = new HashSet<BillingLedger>();

        if (getLedgerItems() != null) {
            for ( BillingLedger billingLedger : getLedgerItems() ) {
                // Only count the null-Billing Session ledgerItems.
                if (billingLedger.getBillingSession() == null) {
                    billableLedgerItems.add(billingLedger);
                }
            }
        }

        return billableLedgerItems;
    }

    public String getUnbilledLedgerItemMessages() {
        StringBuilder builder = new StringBuilder();

        if (getLedgerItems() != null) {
            for (BillingLedger billingLedger : getLedgerItems() ) {
                // If there is a message that is not success, add the message to the end
                if ((billingLedger.getBillingMessage() != null) && billingLedger.isBilled()) {
                    builder.append(billingLedger.getBillingMessage()).append("\n");
                }
            }
        }

        return builder.toString();
    }

    /**
     * Given a sample, compute its billable price items based on its material type.  We assume that all add-ons
     * in the sample's order's product that can accept the sample's material type are required, in addition to the
     * product's primary price item.
     * @return the list of required add-ons.
     */
    List<PriceItem> getBillablePriceItems() {
        List<PriceItem> items = new ArrayList<PriceItem>();
        items.add(getProductOrder().getProduct().getPrimaryPriceItem());
        String sampleMaterialType = getBspDTO().getMaterialType();
        Set<Product> productAddOns = productOrder.getProduct().getAddOns();
        if (!StringUtils.isEmpty(sampleMaterialType) && !productAddOns.isEmpty()) {
            for (Product addOn : productAddOns) {
                for (MaterialType materialType : addOn.getAllowableMaterialTypes()) {
                    if (materialType.getName().equalsIgnoreCase(sampleMaterialType)) {
                        items.add(addOn.getPrimaryPriceItem());
                        // Skip to the next add-on.
                        break;
                    }
                }
            }
        }

        return items;
    }

    /**
     * Automatically generate the billing ledger items for this sample.  Once this is done, its price items will be
     * correctly billed when the next billing session is created.
     * @param completedDate completion date for billing
     * @param quantity quantity for billing
     */
    public void autoBillSample(Date completedDate, double quantity) {
        List<PriceItem> itemsToBill = getBillablePriceItems();
        Map<PriceItem, LedgerQuantities> ledgerQuantitiesMap = getLedgerQuantities();
        for (PriceItem priceItem : itemsToBill) {
            LedgerQuantities quantities = ledgerQuantitiesMap.get(priceItem);
            if (quantities == null) {
                // No ledger item exists for this price item, create it.
                addLedgerItem(completedDate, priceItem, quantity);
            } else {
                // This price item already has a ledger entry.
                // - If it's been billed, don't bill it again, but report this as an issue.
                // - If it hasn't been billed check & see if the quantity is the same as the current.  If they differ,
                // replace the existing quantity with the new quantity.
                if (quantities.getBilled() != 0) {
                    log.debug(MessageFormat.format(
                            "Trying to update an already billed sample, PDO: {0}, sample: {1}, price item: {2}",
                            productOrder.getJiraTicketKey(), sampleName, priceItem.getName()));
                } else if (quantities.getUploaded() == quantity) {
                    log.debug(MessageFormat.format(
                            "Sample already has the same quantity to bill, PDO: {0}, sample: {1}, price item: {2}, quantity {3}",
                            productOrder.getJiraTicketKey(), sampleName, priceItem.getName(), quantity));
                } else {
                    for (BillingLedger item : getLedgerItems()) {
                        if (item.getPriceItem().equals(priceItem)) {
                            item.setQuantity(quantity);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This class holds the billed and uploaded ledger counts for a particular pdo and price item
     */
    public static class LedgerQuantities {
        // If nothing is billed yet, then the total is still 0.
        private double billed;

        // If nothing has been uploaded, we want to just ignore this for upload
        private double uploaded;

        public void addToBilled(double quantity) {
            billed += quantity;
        }

        public void addToUploaded(double quantity) {
            // Should only be one quantity uploaded at any time so, no adding needed
            uploaded = quantity;
        }

        public double getBilled() {
            return billed;
        }

        public double getUploaded() {
            return billed + uploaded;
        }
    }

    public Map<PriceItem, LedgerQuantities> getLedgerQuantities() {
        if (ledgerItems.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<PriceItem, LedgerQuantities> sampleStatus = new HashMap<PriceItem, LedgerQuantities>();
        for (BillingLedger item : ledgerItems) {
            if (!sampleStatus.containsKey(item.getPriceItem())) {
                sampleStatus.put(item.getPriceItem(), new LedgerQuantities());
            }

            if (item.isBilled()) {
                sampleStatus.get(item.getPriceItem()).addToBilled(item.getQuantity());
            } else {
                // The item is not part of a completed billed session or successfully billed item
                // from an active session.
                sampleStatus.get(item.getPriceItem()).addToUploaded(item.getQuantity());
            }
        }

        return sampleStatus;
    }

    public void addLedgerItem(Date workCompleteDate, PriceItem priceItem, double delta) {
        BillingLedger billingLedger = new BillingLedger(this, priceItem, workCompleteDate, delta);
        ledgerItems.add(billingLedger);
        log.debug(MessageFormat.format(
                "Added BillingLedger item for sample {0} to PDO {1} for PriceItemName: {2} - Quantity:{3}",
                sampleName, productOrder.getBusinessKey(), priceItem.getName(), delta));
    }
}