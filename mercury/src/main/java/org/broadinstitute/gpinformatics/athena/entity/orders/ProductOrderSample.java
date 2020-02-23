package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.ProductLedgerIndex;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Index;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class to describe Athena's view of a Sample. A Sample is identified by a sample Id and
 * a billableItem and an optionally comment which may be in most cases empty but on
 * occasion can actually have a value to describe "exceptions" that occur for a particular sample.
 */
@Entity
@Audited
@Table(name = "PRODUCT_ORDER_SAMPLE", schema = "athena")
public class ProductOrderSample extends AbstractSample implements BusinessObject, Serializable {
    private static final long serialVersionUID = 8645451167948826402L;
    private static final Log log = LogFactory.getLog(ProductOrderSample.class);

    /**
     * Count shown when no billing has occurred.
     */
    public static final double NO_BILL_COUNT = 0;
    public static final String TUMOR_IND = BspSampleData.TUMOR_IND;
    public static final String NORMAL_IND = BspSampleData.NORMAL_IND;
    public static final String FEMALE_IND = BspSampleData.FEMALE_IND;
    public static final String MALE_IND = BspSampleData.MALE_IND;
    public static final String ACTIVE_IND = BspSampleData.ACTIVE_IND;

    @Id
    @SequenceGenerator(name = "SEQ_ORDER_SAMPLE", schema = "athena", sequenceName = "SEQ_ORDER_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDER_SAMPLE")
    private Long productOrderSampleId;

    @Index(name = "ix_pos_sample_name")
    @Column(nullable = false)
    private String sampleName;

    private String sampleComment;

    @Index(name = "ix_pos_product_order")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(insertable = false, updatable = false, name = "PRODUCT_ORDER")
    private ProductOrder productOrder;

    @OneToMany(mappedBy = "productOrderSample", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    @BatchSize(size = 100)
    private final Set<LedgerEntry> ledgerItems = new HashSet<>();

    @Column(name = "SAMPLE_POSITION", updatable = false, insertable = false, nullable = false)
    private Integer samplePosition;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product_order_sample", nullable = false)
    @AuditJoinTable(name = "po_sample_risk_join_aud")
    @BatchSize(size = 100)
    private final Set<RiskItem> riskItems = new HashSet<>();

    /**
     * Aliquot ID is used by the auto-bill code to track pipeline results.  It will only be set when a sample has
     * been billed by the auto-bill code.
     */
    @Column(name = "ALIQUOT_ID")
    private String aliquotId;

    @OneToMany(mappedBy = "productOrderSample", cascade = {CascadeType.PERSIST}, orphanRemoval = true)
    @BatchSize(size = 100)
    Set<SampleReceiptValidation> sampleReceiptValidations = new HashSet<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="MERCURY_SAMPLE")
    private MercurySample mercurySample;

    private String aggregationParticle;

    public static ProductOrderSample cloneProductOrderSample(ProductOrderSample sampleToClone) {
        ProductOrderSample newSample = new ProductOrderSample(sampleToClone.sampleName, sampleToClone.getSampleData());

        newSample.setSampleComment(sampleToClone.sampleComment);
        sampleToClone.getMercurySample().addProductOrderSample(newSample);
        newSample.setAggregationParticle(sampleToClone.aggregationParticle);
        newSample.setAliquotId(sampleToClone.aliquotId);
        newSample.setDeliveryStatus(sampleToClone.deliveryStatus);

        Set<RiskItem> clonedRiskItems = new HashSet<>();
        sampleToClone.getRiskItems().forEach(riskItem -> clonedRiskItems.add(new RiskItem(riskItem.getRiskCriterion(),
                riskItem.getComparedValue(),riskItem.getRemark())));
        newSample.setRiskItems(clonedRiskItems);

        sampleToClone.getSampleReceiptValidations()
                .forEach(sampleReceiptValidation ->
                        newSample.addValidation(new SampleReceiptValidation(newSample,
                                sampleReceiptValidation.getCreatedBy(), sampleReceiptValidation.getStatus(),
                                sampleReceiptValidation.getValidationType(), sampleReceiptValidation.getReason())));

        sampleToClone.getLedgerItems().forEach(ledgerEntry -> newSample.addClonedLedgerItem(LedgerEntry.cloneLedgerEntryToNewSample(ledgerEntry, newSample)));

        return newSample;
    }

    /**
     * Detach this ProductOrderSample from all other objects so it can be removed, most importantly MercurySample whose
     * reference would otherwise keep this sample alive.
     *
     * For simplicity, it's currently not allowed to remove a sample that has any billing activity. If the ledgers are
     * unbilled, then the quantity could be zeroed and the sample removed. If there are billed ledgers, the consequences
     * on reporting and such would need to be explored before allowing this removal.
     */
    public void remove() {
        if (ledgerItems != null && !ledgerItems.isEmpty()) {
            throw new IllegalStateException("Samples with billing activity cannot be removed from their product order");
        }

        productOrder = null;

        if (mercurySample != null) {
            mercurySample.removeProductOrderSample(this);
            /*
             * MercurySample.removeProductOrderSample() should do this in order to be symmetric with
             * MercurySample.addProductOrderSample(), but it's important enough that this happens to be defensive about
             * it here.
             */
            mercurySample = null;
        }

        riskItems.clear();

        for (SampleReceiptValidation validation : sampleReceiptValidations) {
            validation.remove();
        }
        sampleReceiptValidations.clear();
    }

    public Product getProductForPriceItem(PriceItem priceItem) {
        Product result = getProductOrder().getProduct();
        if(Objects.equals(getProductOrder().getProduct().getPrimaryPriceItem(),priceItem)) {
            result = getProductOrder().getProduct();
        } else {
            for(ProductOrderAddOn addOn:getProductOrder().getAddOns()) {
                if(Objects.equals(addOn.getAddOn().getPrimaryPriceItem(),priceItem)) {
                    result = addOn.getAddOn();
                    break;
                }
            }
        }
        if(result == null) {
            throw new RuntimeException("Unable to find a product associated with the given price item");
        }
        return result;
    }

    /**
     * Whether to continue processing a sample if a quantification (e.g. Pico) is out of specification
     * (e.g. concentration is too low).
     */
    public enum ProceedIfOutOfSpec implements StatusType {
        YES("Yes"),
        NO("No");

        private String displayName;

        ProceedIfOutOfSpec(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    @Enumerated(EnumType.STRING)
    private ProceedIfOutOfSpec proceedIfOutOfSpec = ProceedIfOutOfSpec.NO;

    @Transient
    private MercurySample.MetadataSource metadataSource;

    @Transient
    private List<OrspProject> orspProjects = new ArrayList<>();

    public List<OrspProject> getOrspProjects() {
        return orspProjects;
    }

    public void addOrspProject(OrspProject orspProject) {
        orspProjects.add(orspProject);
    }

    /**
     * Convert a list of ProductOrderSamples into a list of sample names.
     *
     * @param samples the samples to convert.
     *
     * @return the names of the samples, in the same order as the input.
     */
    public static List<String> getSampleNames(Collection<ProductOrderSample> samples) {
        List<String> names = new ArrayList<>(samples.size());
        for (ProductOrderSample productOrderSample : samples) {
            names.add(productOrderSample.getName());
        }
        return names;
    }

    /**
     * Convert a list of ProductOrderSamples into a list of sample names.
     *
     * @param samples the samples to convert.
     *
     * @return the names of the samples, in the same order as the input.
     */
    public static List<Long> getSampleIDs(Collection<ProductOrderSample> samples) {
        List<Long> ids = new ArrayList<>(samples.size());
        for (ProductOrderSample productOrderSample : samples) {
            ids.add(productOrderSample.getProductOrderSampleId());
        }
        return ids;
    }

    public boolean calculateRisk() {
        riskItems.clear();

        boolean isOnRisk = false;
        RiskCriterion rinRisk = null;
        RiskCriterion rqsRisk = null;

        // Go through each risk check on the product
        for (RiskCriterion criterion : productOrder.getProduct().getRiskCriteria()) {
            switch (criterion.getType()) {
            case RIN:
                rinRisk = criterion;
                break;
            case RQS:
                rqsRisk = criterion;
                break;
            default:
                isOnRisk = evaluateCriterion(criterion, isOnRisk);
            }
        }

        // Special handling for the combination of RIN and RQS risk criteria.
        if (rinRisk != null && rqsRisk != null) {
            if (hasRin() == hasRqs()) {
                isOnRisk = evaluateCriterion(rinRisk, isOnRisk);
                isOnRisk = evaluateCriterion(rqsRisk, isOnRisk);
            } else if (hasRin()) {
                isOnRisk = evaluateCriterion(rinRisk, isOnRisk);
                // suppress RQS risk criterion
            } else if (hasRqs()) {
                // suppress RIN risk criterion
                isOnRisk = evaluateCriterion(rqsRisk, isOnRisk);
            }
        } else {
            if (rinRisk != null) {
                isOnRisk = evaluateCriterion(rinRisk, isOnRisk);
            }
            if (rqsRisk != null) {
                isOnRisk = evaluateCriterion(rqsRisk, isOnRisk);
            }
        }

        // If there are no risk checks that failed, then create a risk item with no criteria to represent NO RISK
        // and this will distinguish NO RISK from never checked
        if (riskItems.isEmpty()) {
            riskItems.add(new RiskItem("Determined no risk"));
        }

        return isOnRisk;
    }

    private boolean evaluateCriterion(RiskCriterion criterion, boolean isOnRisk) {
        // If this is on risk, then create a risk item for it and add it in
        if (criterion.onRisk(this)) {
            riskItems.add(new RiskItem(criterion, criterion.getValueProvider().getValue(this)));
            isOnRisk = true;
        }
        return isOnRisk;
    }

    private boolean hasRin() {
        return isInBspFormat() && getSampleData().getRin() != null;
    }

    private boolean hasRqs() {
        return isInBspFormat() && getSampleData().getRqs() != null;
    }

    public void setManualOnRisk(RiskCriterion criterion, String comment) {
        riskItems.clear();
        riskItems.add(new RiskItem(criterion, Boolean.toString(true), comment));
    }

    public void setManualNotOnRisk(String comment) {
        riskItems.clear();
        riskItems.add(new RiskItem(comment));
    }

    /**
     * Returns true if there's a RIN score for this sample and it can
     * be converted to a number.  Otherwise, returns false;
     */
    public boolean canRinScoreBeUsedForOnRiskCalculation() {
        boolean canRinScoreBeUsed = false;
        if (isInBspFormat()) {

            SampleData sampleData = getSampleData();
            // at time of comment, getSampleData will never return null so a null check is not required
            canRinScoreBeUsed = sampleData.canRinScoreBeUsedForOnRiskCalculation();
        }
        return canRinScoreBeUsed;
    }

    public boolean isCompletelyBilledOld() {
        for (LedgerEntry entry : ledgerItems) {
            if (entry.isBilled() && entry.getPriceItemType() != LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM) {
                return true;
            }
        }
        return false;
    }

    /**
     * A sample is completely billed if its primary or replacement for the primary price item has been billed. If
     * only an add-on has been billed, it's not yet completely billed. If the net billed quantity is 0 due to a credit
     * being billed after a debit, the sample has not been billed. Add-ons are only considered if the same price item
     * has also been billed as a primary or replacement.
     *
     * @return true if this sample has been completely billed.
     */
    public boolean isCompletelyBilled() {

        /*
         * Gather the net quantity billed to each price item by price item type. Separate Maps are used because they
         * contribute to this calculation in different ways. This is like {@link getLedgerQuantities}, but only counts
         * quantities actually billed and also separates the quantities depending on price item type.
         */
        Map<ProductLedgerIndex, BigDecimal> primaryAndReplacementQuantities = new HashMap<>();
        Map<ProductLedgerIndex, BigDecimal> addOnQuantities = new HashMap<>();
        collectLedgerEntryDetails(primaryAndReplacementQuantities, addOnQuantities);

        for (Map.Entry<ProductLedgerIndex, BigDecimal> entry : primaryAndReplacementQuantities.entrySet()) {
            ProductLedgerIndex ledgerIndex = entry.getKey();
            BigDecimal quantity = entry.getValue();

            // Include add-on quantities if this price item was accidentally billed as an add-on as well as a primary.
            if (addOnQuantities.containsKey(ledgerIndex)) {
                quantity.add(addOnQuantities.get(ledgerIndex));
            }
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    public List<String> completelyBilledDetails() {

        /*
         * Gather the net quantity billed to each price item by price item type. Separate Maps are used because they
         * contribute to this calculation in different ways. This is like {@link getLedgerQuantities}, but only counts
         * quantities actually billed and also separates the quantities depending on price item type.
         */
        Map<ProductLedgerIndex, BigDecimal> primaryAndReplacementQuantities = new HashMap<>();
        Map<ProductLedgerIndex, BigDecimal> addOnQuantities = new HashMap<>();

        List<String> results = new ArrayList<>();

        collectLedgerEntryDetails(primaryAndReplacementQuantities, addOnQuantities);
        for (Map.Entry<ProductLedgerIndex, BigDecimal> priceItemDoubleEntry : primaryAndReplacementQuantities.entrySet()) {
            String doubleEntryIndexName = "";

            String resultsMessage = String.format("Billed %s samples for " + priceItemDoubleEntry.getKey()
                    .getDisplayValue(), priceItemDoubleEntry.getValue());
            results.add(resultsMessage);
        }

        for (Map.Entry<ProductLedgerIndex, BigDecimal> addOnPriceItemDoubleEntry : addOnQuantities.entrySet()) {
            results.add("Billed "+addOnPriceItemDoubleEntry.getValue() + " samples for " + addOnPriceItemDoubleEntry.getKey().getDisplayValue());
        }

        return results;
    }

    public void collectLedgerEntryDetails(Map<ProductLedgerIndex, BigDecimal> primaryAndReplacementQuantities,
                                          Map<ProductLedgerIndex, BigDecimal> addOnQuantities) {
        for (LedgerEntry item : ledgerItems) {
            if (item.isBilled()) {

                // Guard against some initial testing data that is billed but doesn't have a price item type.
                if (item.getPriceItemType() != null) {
                    Map<ProductLedgerIndex, BigDecimal> typeSpecificQuantities = null;
                    switch (item.getPriceItemType()) {
                    case PRIMARY_PRICE_ITEM:
                    case REPLACEMENT_PRICE_ITEM:
                        typeSpecificQuantities = primaryAndReplacementQuantities;
                        break;
                    case ADD_ON_PRICE_ITEM:
                        typeSpecificQuantities = addOnQuantities;
                        break;
                    }

                    ProductLedgerIndex priceItem = ProductLedgerIndex.create(item.getProduct(), item.getPriceItem(),
                            item.getProductOrderSample().productOrder.hasSapQuote());
                    BigDecimal currentQuantity = typeSpecificQuantities.get(priceItem);
                    if (currentQuantity == null) {
                        currentQuantity = BigDecimal.ZERO;
                    }
                    typeSpecificQuantities.put(priceItem, currentQuantity.add(item.getQuantity()));
                }
            }
        }
    }

    @Override
    public void setSampleData(@Nonnull SampleData sampleData) {
        super.setSampleData(sampleData);
        if (mercurySample != null) {
            mercurySample.setSampleData(sampleData);
        }
    }

    @Override
    public SampleData makeSampleData() {
        SampleData sampleData;
        if(mercurySample != null) {
            sampleData = mercurySample.makeSampleData();
        } else {
            sampleData = new BspSampleData();
        }
        return sampleData;
    }

    @Override
    public MercurySample.MetadataSource getMetadataSource() {
        if (mercurySample != null) {
            return mercurySample.getMetadataSource();
        }
        if (!isMetadataSourceInitialized()) {
           throw new IllegalStateException(String.format("ProductOrderSample %s transient metadataSource has not been initialized", sampleName));
        }
        return metadataSource;
    }

    public static Map<String, MercurySample.MetadataSource> getMetadataSourcesForBoundProductOrderSamples(
            Collection<ProductOrderSample> samples) {

        Map<String, MercurySample.MetadataSource> results = new HashMap<>();
        for (ProductOrderSample sample : samples) {
            if (sample.getMercurySample() != null) {
                results.put(sample.getSampleKey(), sample.getMetadataSource());
            }
        }
        return results;
    }

    public void setMetadataSource(MercurySample.MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public Date getReceiptDate() {
        return (mercurySample!= null)?mercurySample.getReceivedDate():getSampleData().getReceiptDate();
    }

    /**
     * Find the latest material type by first searching the event history then falling back on the sample's metadata.
     */
    public String getLatestMaterialType() {
        if (mercurySample != null && mercurySample.getLatestMaterialType() != null) {
            return mercurySample.getLatestMaterialType().getDisplayName();
        }
        return getSampleData().getMaterialType();
    }

    public String getFormattedReceiptDate() {
        Date receiptDate = getReceiptDate();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(LabEventSampleDTO.BSP_DATE_FORMAT_STRING);
        if (receiptDate == null) {
            return "";
        }
        return simpleDateFormat.format(receiptDate);
    }

    /**
     * Determines whether or not a sample exists at the Broad. This includes containers that were sent to collaborators
     * and sent back with samples as well as samples derived from those root samples.
     *
     * This is unlike BSP, which only considers root samples to have been received; child samples are effectively
     * received because they came from a root sample, but they themselves are not "received".
     *
     * @return true if the sample is received or derived; false otherwise
     */
    public boolean isSampleReceived() {
        return (mercurySample != null && mercurySample.getReceivedDate() != null) || getSampleData().isSampleReceived();
    }

    public enum DeliveryStatus implements StatusType {
        NOT_STARTED(""),
        DELIVERED("Delivered"),
        ABANDONED("Abandoned");

        private final String displayName;

        DeliveryStatus(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public boolean isAbandoned() {
            return this == ABANDONED;
        }
    }

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.NOT_STARTED;

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(@Nonnull ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    // Required by JPA.
    @SuppressWarnings("UnusedDeclaration")
    protected ProductOrderSample() {
    }

    public ProductOrderSample(@Nonnull String sampleName) {
        if (!StringUtils.isAsciiPrintable(sampleName)) {
            throw new RuntimeException("Sample name " + sampleName + " is not ASCII printable");
        }
        this.sampleName = sampleName;
    }

    /**
     * TEST-ONLY delegating constructor that also sets the entity's primary key.
     *
     * @param sampleName the sample ID
     * @param primaryKey the primary key
     *
     * @see #ProductOrderSample(String)
     */
    public ProductOrderSample(@Nonnull String sampleName, Long primaryKey) {
        this(sampleName);
        this.productOrderSampleId = primaryKey;
    }

    /**
     * Used for testing only.
     */
    public ProductOrderSample(@Nonnull String sampleName,
                              @Nonnull SampleData sampleData) {
        super(sampleData);
        this.sampleName = sampleName;
    }

    /**
     * TEST-ONLY delegating constructor that also sets the entity's primary key.
     *
     * @param sampleName the sample ID
     * @param sampleData the sample data
     * @param primaryKey the primary key
     *
     * @see #ProductOrderSample(String, SampleData)
     */
    public ProductOrderSample(@Nonnull String sampleName, @Nonnull SampleData sampleData, Long primaryKey) {
        this(sampleName, sampleData);
        this.productOrderSampleId = primaryKey;
    }

    @Override
    public String getName() {
        return sampleName;
    }

    /**
     * Returns the public name of the key that should be used for display purposes.
     *
     * @return the name of the sample
     */
    @Override
    public String getBusinessKey() {
        return sampleName;
    }

    @Override
    public String getSampleKey() {
        return sampleName;
    }

    public String getSampleComment() {
        return sampleComment;
    }

    public void setSampleComment(String sampleComment) {
        this.sampleComment = sampleComment;
    }

    public Integer getSamplePosition() {
        return samplePosition;
    }

    public void setSamplePosition(Integer samplePosition) {
        this.samplePosition = samplePosition;
    }

    public Set<LedgerEntry> getLedgerItems() {
        return ledgerItems;
    }

    /**
     * Returns the internal database key, not to be used for display purposes (use #getBusinessKey() instead).
     *
     * @return the database ID for the {@link ProductOrderSample}
     */
    public Long getProductOrderSampleId() {
        return productOrderSampleId;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    @Nonnull
    public Set<LedgerEntry> getBillableLedgerItems() {
        Set<LedgerEntry> billableLedgerItems = new HashSet<>();

        if (getLedgerItems() != null) {
            for (LedgerEntry ledgerEntry : getLedgerItems()) {
                // Only count the null-Billing Session ledgerItems.
                if (ledgerEntry.getBillingSession() == null) {
                    billableLedgerItems.add(ledgerEntry);
                }
            }
        }

        return billableLedgerItems;
    }

    public Set<LedgerEntry> getBilledLedgerItems() {
        Set<LedgerEntry> billedEntries = new HashSet<>();

        if(CollectionUtils.isNotEmpty(getLedgerItems())) {
            for (LedgerEntry ledgerEntry : getLedgerItems()) {
                if(StringUtils.equals(ledgerEntry.getBillingMessage(), BillingSession.SUCCESS)) {
                    billedEntries.add(ledgerEntry);
                }
            }

        }
        return billedEntries;
    }

    /**
     * Go through each ledger item and and construct the messages.
     *
     * @return The messages.
     */
    public String getUnbilledLedgerItemMessages() {
        StringBuilder builder = new StringBuilder();

        if (getLedgerItems() != null) {
            for (LedgerEntry ledgerEntry : getLedgerItems()) {
                if ((ledgerEntry.getBillingMessage() != null) && !ledgerEntry.isBilled()) {
                    builder.append(ledgerEntry.getBillingMessage()).append("\n");
                }
            }
        }

        return builder.toString();
    }

    /**
     * Automatically generate the billing ledger items for this sample.  Once this is done, its price items will be
     * correctly billed when the next billing session is created.
     *
     * @param completedDate completion date for billing
     * @param quantity      quantity for billing
     */
    public void autoBillSample(Date completedDate, BigDecimal quantity) {
        Date now = new Date();
        Map<ProductLedgerIndex, LedgerQuantities> ledgerQuantitiesMap = getLedgerQuantities();
        Product product = getProductOrder().getProduct();
        Optional<PriceItem> nullablePriceItem = Optional.ofNullable(product.getPrimaryPriceItem());
        PriceItem priceItem = null;
        if(nullablePriceItem.isPresent()) {
            priceItem = product.getPrimaryPriceItem();
        }

        LedgerQuantities quantities = ledgerQuantitiesMap.get(ProductLedgerIndex.create(product, nullablePriceItem.orElse(null),
                getProductOrder().hasSapQuote()));
        if (quantities == null) {
            // No ledger item exists for this price item, create it using the current order's price item
            if(getProductOrder().hasSapQuote()) {
                addAutoLedgerItem(completedDate, product, quantity, now, false);
            } else {
                addAutoLedgerItem(completedDate, nullablePriceItem.orElse(null), quantity, now);
            }
        } else {
            // This price item already has a ledger entry.
            // - If it's been billed, don't bill it again, but report this as an issue.
            // - If it hasn't been billed check & see if the quantity is the same as the current.  If they differ,
            // replace the existing quantity with the new quantity. When replacing, also set the timestamp so
            // the PDM can be warned about downloading the spreadsheet AFTER this change.
            String priceItemName = "";
            if (nullablePriceItem.isPresent()) {
                priceItemName = priceItem.getName();
            }

            if (quantities.getBilled().compareTo(BigDecimal.ZERO) != 0) {
                log.debug(MessageFormat.format(
                        "Trying to update an already billed sample, PDO: {0}, sample: {1}, price item: {2}, Product: [3]",
                        productOrder.getJiraTicketKey(), sampleName, priceItemName, product.getDisplayName()));
            } else if (MathUtils.isSame(quantities.getTotal(), quantity)) {
                log.debug(MessageFormat.format(
                        "Sample already has the same quantity to bill, PDO: {0}, sample: {1}, price item: {2}, Product: [3], quantity {4}",
                        productOrder.getJiraTicketKey(), sampleName, priceItemName, product.getDisplayName(), quantity));
            } else {
                // This price item already has a ledger entry.
                // - If it's been billed, don't bill it again, but report this as an issue.
                // - If it hasn't been billed check & see if the quantity is the same as the current.  If they differ,
                // replace the existing quantity with the new quantity. When replacing, also set the timestamp so
                // the PDM can be warned about downloading the spreadsheet AFTER this change.
                if (quantities.getBilled().compareTo(BigDecimal.ZERO) != 0) {
                    log.debug(MessageFormat.format(
                            "Trying to update an already billed sample, PDO: {0}, sample: {1}, price item: {2}, Product: [3]",
                            productOrder.getJiraTicketKey(), sampleName, priceItemName, product.getDisplayName()));
                } else if (MathUtils.isSame(quantities.getTotal(), quantity)) {
                    log.debug(MessageFormat.format(
                            "Sample already has the same quantity to bill, PDO: {0}, sample: {1}, price item: {2}, Product: [3], quantity {4}",
                            productOrder.getJiraTicketKey(), sampleName, priceItemName, product.getDisplayName(), quantity));
                } else {
                    /*
                     * Why overwrite what a user likely manually uploaded? This seems wrong and/or dangerous.
                     */
                    for (LedgerEntry item : getLedgerItems()) {
                        if ((getProductOrder().hasSapQuote() && item.getProduct().equals(product)) ||
                            !getProductOrder().hasSapQuote() && item.getPriceItem().equals(priceItem)) {
                            item.setQuantity(quantity);
                            item.setAutoLedgerTimestamp(now);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This class holds the ledger counts for a particular product order sample and price item.
     */
    public static class LedgerQuantities {

        /**
         * The ID of the sample for these quantities.
         */
        private String sampleName;

        /**
         * The price item for these quantities.
         */
        private String priceItemName;

        /**
         * The Product Partnumber associated with these Quantities
         */
        private String productPartNumber;

        /**
         * The quantity uploaded to Mercury that is pending billing (not yet in a billing session).
         */
        private BigDecimal uploaded = BigDecimal.ZERO;

        /**
         * The quantity currently in active billing sessions but not yet billed externally.
         */
        private BigDecimal inProgress = BigDecimal.ZERO;

        /**
         * The total quantity that has been billed externally (Broad Quotes, SAP, etc.).
         */
        private BigDecimal billed = BigDecimal.ZERO;

        public LedgerQuantities(String sampleName, String priceItemName, String partNumber) {
            this.sampleName = sampleName;
            this.priceItemName = priceItemName;
            this.productPartNumber = partNumber;
        }

        /**
         * Accumulate additional quantity that has not yet been billed. There should never be more than one unbilled
         * LedgerEntry per sample and price item. Therefore, this method is expected to only be called once per sample
         * and price item.
         *
         * @param quantity    the quantity that has been uploaded for billing
         */
        public void addToUploaded(BigDecimal quantity) {
            if (BigDecimal.ZERO.compareTo(uploaded) != 0) {
                throw new RuntimeException(String.format(
                        "There should only be one unbilled LedgerEntry for sample %s, price item %s, and product %s. "
                        + "This needs to be corrected in the database to avoid data inconsistencies and possible "
                        + "double-billing.",
                        sampleName, priceItemName, productPartNumber));
            }
            uploaded = uploaded.add(quantity);
        }

        /**
         * Accumulate additional quantity that is currently being billed (is in an active billing session). Because
         * there should never be more than one unbilled LedgerEntry per sample and price item, that same rule should
         * also apply to in-progress ledger entries. Therefore, this method is expected to only be called once per
         * sample and price item.
         *
         * @param quantity    the quantity currently being billed
         * @throws RuntimeException
         */
        public void addToInProgress(BigDecimal quantity) {
            if (inProgress.compareTo(BigDecimal.ZERO) != 0) {
                throw new RuntimeException(String.format(
                        "There should only be one in-progress LedgerEntry for sample %s, price item %s, "
                        + "and product %s. This needs to be corrected in the database to avoid data inconsistencies "
                        + "and possible double-billing.",
                        sampleName, priceItemName, productPartNumber));
            }
            inProgress = inProgress.add(quantity);
        }

        /**
         * Accumulate additional quantity that has been billed externally (Broad Quotes, SAP, etc.). This can be called
         * multiple times for a sample due to billing of quantity > 1, billing followed by crediting, etc.
         *
         * @param quantity    the quantity that was billed
         */
        public void addToBilled(BigDecimal quantity) {
            billed = billed.add(quantity);
        }

        /**
         * Get the total quantity successfully billed for this sample and price item.
         *
         * @return the quantity successfully billed
         */
        public BigDecimal getBilled() {
            return billed;
        }

        /**
         * Get the total quantity uploaded, in-progress, and billed for this sample and price item. This represents the
         * total intended net quantity to be billed regardless of the state of actually being billed.
         *
         * @return the total quantity for billing
         */
        public BigDecimal getTotal() {
            return billed.add(inProgress).add(uploaded);
        }

        public boolean isBeingBilled() {
            return inProgress.compareTo(BigDecimal.ZERO) != 0;
        }
    }

    public Map<ProductLedgerIndex, LedgerQuantities> getLedgerQuantities() {
        if (ledgerItems.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ProductLedgerIndex, LedgerQuantities> sampleStatus = new HashMap<>();
        for (LedgerEntry item : ledgerItems) {
            Optional<PriceItem> priceItem = Optional.ofNullable(item.getPriceItem());
            Optional<Product> product = Optional.ofNullable(item.getProduct());
            String priceItemName = "";
            String partNumber= "";
            if(product.isPresent()) {
                partNumber = product.get().getPartNumber();
            }
            if(priceItem.isPresent()) {
                priceItemName = priceItem.get().getName();
            }
            Set<ProductLedgerIndex> indexForPriceItem=
                    sampleStatus.keySet().stream().filter(index -> {
                        if(getProductOrder().hasSapQuote()) {
                            return index.getProduct().equals(product.orElse(null));
                        } else {
                            return index.getPriceItem().equals(priceItem.orElse(null));
                        }
                    }).collect(Collectors.toSet());
            if (indexForPriceItem.isEmpty()) {
                sampleStatus.put(ProductLedgerIndex.create(product.orElse(null), priceItem.orElse(null),
                        getProductOrder().hasSapQuote()), new LedgerQuantities(sampleName, priceItemName, partNumber));
            }

            if (item.isBilled()) {
                sampleStatus.get(ProductLedgerIndex.create(product.orElse(null), priceItem.orElse(null),
                        getProductOrder().hasSapQuote())).addToBilled(item.getQuantity());
            } else if (item.isBeingBilled()) {
                // The item is part of an active billing session.
                sampleStatus.get(ProductLedgerIndex.create(product.orElse(null), priceItem.orElse(null),
                        getProductOrder().hasSapQuote())).addToInProgress(item.getQuantity());
            } else {
                // The item is not part of a completed billed session or successfully billed item from an active session.
                sampleStatus.get(ProductLedgerIndex.create(product.orElse(null), priceItem.orElse(null),
                        getProductOrder().hasSapQuote())).addToUploaded(item.getQuantity());
            }
        }

        return sampleStatus;
    }

    public LedgerEntry findUnbilledLedgerEntryForPriceItem(PriceItem priceItem) {
        LedgerEntry ledgerEntry = null;
        for (LedgerEntry ledgerItem : ledgerItems) {
            if (ledgerItem.getPriceItem().equals(priceItem) && !ledgerItem.isBilled()) {
                if (ledgerEntry != null) {
                    throw new RuntimeException(
                            "Found multiple unbilled ledger entries for sample '" + sampleName + "' and price item '"
                            + priceItem.getName()
                            + "'. This situation has been known to lead to serious billing problems in the past!");
                }
                ledgerEntry = ledgerItem;
            }
        }
        return ledgerEntry;
    }

    public LedgerEntry findUnbilledLedgerEntryForProduct(Product product) {
        LedgerEntry ledgerEntry = null;
        for (LedgerEntry ledgerItem : ledgerItems) {
            if (ledgerItem.getProduct().equals(product) && !ledgerItem.isBilled()) {
                if (ledgerEntry != null) {
                    throw new RuntimeException(
                            "Found multiple unbilled ledger entries for sample '" + sampleName + "' and product '"
                            + product.getDisplayName()
                            + "'. This situation has been known to lead to serious billing problems in the past!");
                }
                ledgerEntry = ledgerItem;
            }
        }
        return ledgerEntry;
    }

    /**
     * Tracks the details of a requested update to the billed quantity of a sample for a price item.
     *
     * Note that this must contain the old and new quantities instead of just the change requested.
     * <ul>
     *     <li>The old quantity is compared against the current total to be certain that the requested change is based
     *     on up-to-date information.</li>
     *     <li>The new quantity is compared against the current total to detect when a requested change has already been
     *     applied by another user or an automatic billing process.</li>
     *     <li>The old and new quantities are compared to determine whether or not a change was requested and are used
     *     to calculate the difference for this change request.</li>
     * </ul>
     */
    public static class LedgerUpdate {

        /**
         * The name of the sample being updated.
         */
        private String sampleName;

        /**
         * The price item for the ledger update.
         */
        private PriceItem priceItem;

        /**
         * The product for the ledger update
         */
        private Product product;

        /**
         * The total quantity that the user was viewing when deciding what the new quantity should be.
         */
        private BigDecimal oldQuantity = BigDecimal.ZERO;

        /**
         * The current quantity loaded from the database at the time the update request was received. This may be
         * different than oldQuantity if it was changed by another user/process while this user was making billing
         * decisions.
         */
        private BigDecimal currentQuantity = BigDecimal.ZERO;

        /**
         * The new total quantity being requested.
         */
        private BigDecimal newQuantity = BigDecimal.ZERO;

        /**
         * The work complete date for any new or updated ledger entries. Used as a bill date when billing to
         * Broad Quotes/SAP.
         */
        private Date workCompleteDate;

        private boolean replacementUsed;

        /**
         * Create a new LedgerUpdate. This is only a representation of the requested change; no updates are performed.
         *
         * @param sampleName          the sample being updated
         * @param priceItem           the price item being billed
         * @param oldQuantity         the quantity at the time the form was rendered
         * @param currentQuantity     the quantity at the time the form was submitted
         * @param newQuantity         the requested quantity
         * @param workCompleteDate    the date that work was completed
         */
        public LedgerUpdate(String sampleName, PriceItem priceItem, Product product, BigDecimal oldQuantity, BigDecimal currentQuantity,
                            BigDecimal newQuantity, Date workCompleteDate) {
            this.sampleName = sampleName;
            this.priceItem = priceItem;
            this.product = product;
            this.currentQuantity = currentQuantity;
            this.oldQuantity = oldQuantity;
            this.newQuantity = newQuantity;
            this.workCompleteDate = workCompleteDate;
        }

        /**
         * Create a new LedgerUpdate. This is only a representation of the requested change; no updates are performed.
         *  @param sampleName          the sample being updated
         * @param oldQuantity         the quantity at the time the form was rendered
         * @param currentQuantity     the quantity at the time the form was submitted
         * @param newQuantity         the requested quantity
         * @param workCompleteDate    the date that work was completed
         * @param replacementUsed
         */
        public LedgerUpdate(String sampleName, Product product, BigDecimal oldQuantity, BigDecimal currentQuantity,
                            BigDecimal newQuantity, Date workCompleteDate, boolean replacementUsed) {
            this.sampleName = sampleName;
            this.product = product;
            this.currentQuantity = currentQuantity;
            this.oldQuantity = oldQuantity;
            this.newQuantity = newQuantity;
            this.workCompleteDate = workCompleteDate;
            this.replacementUsed = replacementUsed;
        }

        /**
         * Determine whether or not the user's intent was to change the quantity billed by looking at the original value
         * they were presented with and the new value that they submitted.
         *
         * @return true if the user requested a change; false otherwise
         */
        public boolean isQuantityChangeIntended() {
            return !newQuantity.equals(oldQuantity);
        }

        /**
         * Determine whether or not a change needs to be applied. This could return false in the case where a change was
         * requested but an equivalent change was persisted by someone else.
         *
         * @return true if the quantity submitted is different than the current quantity; false otherwise
         */
        public boolean isQuantityChangeNeeded() {
            return !newQuantity.equals(currentQuantity);
        }

        /**
         * Determine whether or not the decision to request any change is based on current quantity data. This
         * ensures that a requested change will result in the total quantity that the user is expecting it to.
         *
         * @return true if the change request is based on current data; false otherwise
         */
        public boolean isChangeRequestCurrent() {
            return oldQuantity.equals(currentQuantity);
        }

        /**
         * Determine whether or not this update request represents a change to the current quantity. This is a
         * combination of {@link #isQuantityChangeIntended()} and {@link #isQuantityChangeNeeded()} for cases where it
         * isn't necessary to handle those cases separately.
         *
         * @return true if action needs to be taken to change the quantity from what it currently is; false otherwise
         */
        public boolean isQuantityChanging() {
            return isQuantityChangeIntended() && isQuantityChangeNeeded();
        }

        /**
         * Calculate the difference in quantity to apply. Can be negative if issuing a credit.
         *
         * @return the quantity to apply in a ledger entry
         */
        public BigDecimal getQuantityDelta() {
            return newQuantity.subtract(oldQuantity);
        }

        public String getSampleName() {
            return sampleName;
        }

        public PriceItem getPriceItem() {
            return priceItem;
        }

        public Product getProduct() {
            return product;
        }

        public BigDecimal getOldQuantity() {
            return oldQuantity;
        }

        public BigDecimal getCurrentQuantity() {
            return currentQuantity;
        }

        public BigDecimal getNewQuantity() {
            return newQuantity;
        }

        public Date getWorkCompleteDate() {
            return workCompleteDate;
        }
    }

    /**
     * Apply a ledger update to this ProductOrderSample by adding, modifying, or cancelling unbilled LedgerEntry
     * instances. Because of all of the data contained in the LedgerUpdate, this operation can be optimized to make the
     * minimum change necessary to produce the desired result, including recognizing that a requested change has already
     * been applied by another user/process.
     *
     * @param ledgerUpdate    the ledger update to apply to this sample
     * @throws StaleLedgerUpdateException if the data in the ledger update indicates that the update decision was made
     *                                    based on old data
     * @throws RuntimeException if the work complete date is required and missing
     *                          or if an update would need to be made to a ledger entry in an active billing session
     */
    public void applyLedgerUpdate(LedgerUpdate ledgerUpdate) throws StaleLedgerUpdateException {
        LedgerEntry existingLedgerEntry;
        if(ledgerUpdate.getPriceItem() == null) {
            existingLedgerEntry = findUnbilledLedgerEntryForProduct(ledgerUpdate.getProduct());
        } else {
            existingLedgerEntry = findUnbilledLedgerEntryForPriceItem(ledgerUpdate.getPriceItem());
        }

        /*
         * These 2 conditions both require validation of the work complete date. The actions taken beyond that depend on
         * the further combinatorial possibilities of these conditions.
         */
        boolean haveExistingEntry = existingLedgerEntry != null;
        if (ledgerUpdate.isQuantityChanging() || haveExistingEntry) {

            // Validate work complete date.
            if (ledgerUpdate.getWorkCompleteDate() == null) {
                throw new IllegalArgumentException(
                        "Work complete date is missing for sample " + ledgerUpdate.getSampleName());
            }
            Date now = new Date();
            if (now.before(ledgerUpdate.getWorkCompleteDate())) {
                final String futureErrorMessage =
                        String.format("Sample %s cannot have a completed date of %s because it is in the future.",
                                ledgerUpdate.getSampleName(),
                                new SimpleDateFormat(LedgerEntry.BILLING_LEDGER_DATE_FORMAT)
                                        .format(ledgerUpdate.getWorkCompleteDate()));
                throw new IllegalArgumentException( futureErrorMessage);
            }


            // Update quantity, adding a new ledger entry if needed.
            if (ledgerUpdate.isQuantityChanging()) {
                if (ledgerUpdate.isChangeRequestCurrent()) {

                    BigDecimal quantityDelta = ledgerUpdate.getQuantityDelta();

                    if (!haveExistingEntry) {

                        if(ledgerUpdate.getPriceItem() == null) {

                            addLedgerItem(ledgerUpdate.getWorkCompleteDate(), ledgerUpdate.getProduct(), quantityDelta,
                                    ledgerUpdate.replacementUsed);
                        } else {
                            addLedgerItem(ledgerUpdate.getWorkCompleteDate(), ledgerUpdate.getPriceItem(),
                                    quantityDelta);
                        }
                    } else {

                        if (existingLedgerEntry.isBeingBilled()) {
                            throw new RuntimeException(
                                    "Cannot change quantity for sample that is currently being billed.");
                        }

                        BigDecimal newQuantity = existingLedgerEntry.getQuantity().add(quantityDelta);
                        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
                            ledgerItems.remove(existingLedgerEntry);
                        } else {
                            existingLedgerEntry.setQuantity(newQuantity);
                        }
                    }
                } else {
                    throw new StaleLedgerUpdateException(ledgerUpdate);
                }
            }

            // Update work complete date for existing entry, whether or not the quantity is changing.
            if (haveExistingEntry) {
                existingLedgerEntry.setWorkCompleteDate(ledgerUpdate.getWorkCompleteDate());
                existingLedgerEntry.setSapReplacementPricing(ledgerUpdate.replacementUsed);
            }
        }
    }

    /**
     * This adds an automatically uploaded ledger entry. The auto timestamp is set to right now to indicate that the
     * upload happened and needs to be reviewed.
     *
     * @param workCompleteDate The date completed.
     * @param priceItem        The price item to charge.
     * @param delta            The plus or minus value to bill to the quote server.
     * @param currentDate      The ledger entry needs a date to say when the auto entry was made.
     */
    public void addAutoLedgerItem(Date workCompleteDate, PriceItem priceItem, BigDecimal delta, Date currentDate) {
        addLedgerItem(workCompleteDate, priceItem, delta, currentDate);
    }

    /**
     * This adds an automatically uploaded ledger entry. The auto timestamp is set to right now to indicate that the
     * upload happened and needs to be reviewed.
     *
     * @param workCompleteDate The date completed.
     * @param product        The product to charge.
     * @param delta            The plus or minus value to bill to the quote server.
     * @param currentDate      The ledger entry needs a date to say when the auto entry was made.
     * @param sapReplacement
     */
    public void addAutoLedgerItem(Date workCompleteDate, Product product, BigDecimal delta, Date currentDate,
                                  Boolean sapReplacement) {
        addLedgerItem(workCompleteDate, product, delta, currentDate, sapReplacement);
    }

    /**
     * This adds a manually uploaded ledger entry. The auto timestamp of NULL indicates that the upload happened, which
     * will lock out auto billing and sets to Can Bill.
     *
     * @param workCompleteDate The date completed.
     * @param priceItem        The price item to charge.
     * @param delta            The plus or minus value to bill to the quote server.
     */
    public void addLedgerItem(Date workCompleteDate, PriceItem priceItem, BigDecimal delta) {
        addLedgerItem(workCompleteDate, priceItem, delta, null);
    }

    /**
     * This adds a manually uploaded ledger entry. The auto timestamp of NULL indicates that the upload happened, which
     * will lock out auto billing and sets to Can Bill.
     *
     * @param workCompleteDate The date completed.
     * @param product        The productto charge.
     * @param delta            The plus or minus value to bill to the quote server.
     * @param sapReplacement
     */
    public void addLedgerItem(Date workCompleteDate, Product product, BigDecimal delta, Boolean sapReplacement) {
        addAutoLedgerItem(workCompleteDate, product, delta, null, sapReplacement);
    }

    public void addLedgerItem(Date workCompleteDate, PriceItem priceItem, BigDecimal delta, Date autoLedgerTimestamp) {
        LedgerEntry ledgerEntry = new LedgerEntry(this, priceItem, workCompleteDate, delta);
        ledgerEntry.setAutoLedgerTimestamp(autoLedgerTimestamp);
        ledgerItems.add(ledgerEntry);
        String priceItemName = "";
        if(priceItem != null) {
            priceItemName = priceItem.getName();
        }
        log.debug(MessageFormat.format(
                "Added LedgerEntry item for sample {0} to PDO {1} for PriceItemName: {2} - Quantity:{3}",
                sampleName, productOrder.getBusinessKey(), priceItemName , delta));
    }

    public void addLedgerItem(Date workCompleteDate, Product product, BigDecimal delta, Date autoLedgerTimestamp,
                              Boolean sapReplacement) {
        LedgerEntry ledgerEntry = new LedgerEntry(this, product, workCompleteDate, delta);
        ledgerEntry.setAutoLedgerTimestamp(autoLedgerTimestamp);
        ledgerEntry.setSapReplacementPricing(sapReplacement);
        ledgerItems.add(ledgerEntry);
        log.debug(MessageFormat.format(
                "Added LedgerEntry item for sample {0} to PDO {1} for partNumber: {2} - Quantity:{3}",
                sampleName, productOrder.getBusinessKey(), product.getName(), delta));
    }

    /**
     * Added primarily for the purpose of fixup tests
     * @param clonedLedgerItem
     */
    public void addClonedLedgerItem(LedgerEntry clonedLedgerItem) {
        ledgerItems.add(clonedLedgerItem);

    }

    /**
     * @return If there are any risk items with non-null criteria, then it is on risk.
     */
    public boolean isOnRisk() {
        for (RiskItem item : riskItems) {
            if (item.isOnRisk()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return A string with the full details for each {@link RiskItem} for the sample.
     */
    public String getRiskString() {
        if (!isOnRisk()) {
            return "";
        }

        if( riskItems.size() == 0) {
            return "";
        } else if( riskItems.size() == 1 ) {
            return riskItems.iterator().next().getInformation().replaceAll("\r?\n", " ");
        } else {
            StringBuilder riskTypeStringBuilder = new StringBuilder();

            boolean isFirst = true;
            for (RiskItem riskItem : riskItems) {
                if( !isFirst ) {
                    riskTypeStringBuilder.append(" AND ");
                } else {
                    isFirst = false;
                }
                riskTypeStringBuilder.append(riskItem.getInformation().replaceAll("\r?\n", " "));
            }
            return riskTypeStringBuilder.toString();
        }
    }

    /**
     * @return A string of each {@link RiskItem}s {@link RiskCriterion} type for the sample.
     */
    public String getRiskTypeString() {
        if (!isOnRisk()) {
            return "";
        }
        if( riskItems.size() == 0) {
            return "";
        } else if( riskItems.size() == 1 ) {
            return riskItems.iterator().next().getRiskCriterion().getType().getLabel();
        } else {
            StringBuilder riskTypeStringBuilder = new StringBuilder();

            // Multiple risk items need to be consistently sorted and de-duped
            Set<String> sortedNames = new TreeSet<>();

            int i = 0;
            for (RiskItem riskItem : riskItems) {
                sortedNames.add(riskItem.getRiskCriterion().getType().getLabel());
            }

            boolean isFirst = true;
            for ( String label : sortedNames ) {
                if( !isFirst ) {
                    riskTypeStringBuilder.append(" AND ");
                }
                isFirst = false;
                riskTypeStringBuilder.append(label);
            }

            return riskTypeStringBuilder.toString();
        }
    }

    /**
     * Rolls up visibility of the samples availability from just the sample data
     *
     * If the metadata is essentially BSP,
     */
    public boolean isSampleAvailable() {
        boolean available;
        if(!isMetadataSourceInitialized()) {
            available = false;
        } else {
            switch (getMetadataSource()) {
            case BSP:
                available = isSampleReceived();
                break;
            case MERCURY:
                available = isSampleAccessioned() && isSampleReceived();
                break;
            default:
                throw new IllegalStateException("The metadata Source is undetermined");
            }
        }
        return available;
    }

    private boolean isMetadataSourceInitialized() {
        return mercurySample != null || metadataSource != null ;
    }

    /**
     * Exposes if a sample has been Accessioned.
     */
    private boolean isSampleAccessioned() {
        boolean sampleAccessioned = false;
        if (mercurySample != null) {
            sampleAccessioned = mercurySample.hasSampleBeenAccessioned();
        }
        return sampleAccessioned;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Collection<RiskItem> getRiskItems() {
        return riskItems;
    }

    // Only called from test code.
    public void setRiskItems(Collection<RiskItem> riskItems) {
        this.riskItems.clear();
        this.riskItems.addAll(riskItems);
    }

    public String getAliquotId() {
        return aliquotId;
    }

    public void setAliquotId(String aliquotId) {
        this.aliquotId = aliquotId;
    }

    public Date getLatestAutoLedgerTimestamp() {
        Set<LedgerEntry> ledgerEntries = getBillableLedgerItems();

        // Use the latest auto ledger timestamp so we can error out if anything later is in the ledger.
        Date latestAutoDate = null;
        for (LedgerEntry ledgerEntry : ledgerEntries) {
            if (ledgerEntry.getAutoLedgerTimestamp() != null) {
                if ((latestAutoDate == null) || (ledgerEntry.getAutoLedgerTimestamp().after(latestAutoDate))) {
                    latestAutoDate = ledgerEntry.getAutoLedgerTimestamp();
                }
            }
        }

        return latestAutoDate;
    }

    public Date getWorkCompleteDate() {
        Set<LedgerEntry> ledgerEntries = getBillableLedgerItems();

        // Very simple logic that for now rolls up all work complete dates and assumes they are the same across
        // all price items on the PDO sample.
        for (LedgerEntry ledgerEntry : ledgerEntries) {
            if (!ledgerEntry.isBilled()) {
                return ledgerEntry.getWorkCompleteDate();
            }
        }

        return null;
    }

    public Set<SampleReceiptValidation> getSampleReceiptValidations() {
        return sampleReceiptValidations;
    }

    public void addValidation(SampleReceiptValidation validation) {
        validation.setProductOrderSample(this);
        this.sampleReceiptValidations.add(validation);
    }

    public void setName(String newName) {
        if (!StringUtils.isAsciiPrintable(newName)) {
            throw new RuntimeException("Sample name is not ASCII printable");
        }
        sampleName = newName;
    }

    public MercurySample getMercurySample() {
        return mercurySample;
    }

    public void setMercurySample(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }

    public ProceedIfOutOfSpec getProceedIfOutOfSpec() {
        return proceedIfOutOfSpec;
    }

    public void setProceedIfOutOfSpec(ProceedIfOutOfSpec proceedIfOutOfSpec) {
        this.proceedIfOutOfSpec = proceedIfOutOfSpec;
    }

    public boolean isToBeBilled() {
        return getDeliveryStatus() != ProductOrderSample.DeliveryStatus.ABANDONED
        && !isCompletelyBilled();
    }

    public String getAggregationParticle() {
        return aggregationParticle;
    }

    public void setAggregationParticle(String aggregationParticle) {
        this.aggregationParticle = aggregationParticle;
    }
}
