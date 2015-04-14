package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @JoinColumn(insertable = false, updatable = false)
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
    private MercurySample mercurySample;

    @Transient
    private MercurySample.MetadataSource metadataSource;

    @Transient
    private boolean isMetadataSourceInitialized;

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

    /**
     * A sample is completely billed if its primary or replacement for the primary price item has been billed. If
     * only an add-on has been billed, it's not yet completely billed.
     *
     * @return true if this sample has been completely billed.
     */
    public boolean isCompletelyBilled() {
        for (LedgerEntry entry : ledgerItems) {
            if (entry.isBilled() && entry.getPriceItemType() != LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected SampleData makeSampleData() {
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
        if (!isMetadataSourceInitialized) {
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
        isMetadataSourceInitialized = true;
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
     * Given a sample, compute its billable price items based on its material type.  We assume that all add-ons
     * in the sample's order's product that can accept the sample's material type are required, in addition to the
     * product's primary price item.
     *
     * @return the list of required add-ons.
     */
    List<PriceItem> getBillablePriceItems() {
        List<PriceItem> items = new ArrayList<>();
        items.add(getProductOrder().getProduct().getPrimaryPriceItem());
        org.broadinstitute.bsp.client.sample.MaterialType materialTypeObject =
                getSampleData().getMaterialTypeObject();
        Set<Product> productAddOns = productOrder.getProduct().getAddOns();
        if (materialTypeObject != null && !productAddOns.isEmpty()) {
            MaterialType sampleMaterialType =
                    new MaterialType(materialTypeObject.getCategory(), materialTypeObject.getName());
            for (Product addOn : productAddOns) {
                for (MaterialType materialType : addOn.getAllowableMaterialTypes()) {
                    if (materialType.equals(sampleMaterialType)) {
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
     *
     * @param completedDate completion date for billing
     * @param quantity      quantity for billing
     */
    public void autoBillSample(Date completedDate, double quantity) {
        Date now = new Date();

        List<PriceItem> itemsToBill = getBillablePriceItems();
        Map<PriceItem, LedgerQuantities> ledgerQuantitiesMap = getLedgerQuantities();

        for (PriceItem priceItem : itemsToBill) {
            LedgerQuantities quantities = ledgerQuantitiesMap.get(priceItem);
            if (quantities == null) {
                // No ledger item exists for this price item, create it using the current order's price item
                addAutoLedgerItem(completedDate, priceItem, quantity, now);
            } else {
                // This price item already has a ledger entry.
                // - If it's been billed, don't bill it again, but report this as an issue.
                // - If it hasn't been billed check & see if the quantity is the same as the current.  If they differ,
                // replace the existing quantity with the new quantity. When replacing, also set the timestamp so
                // the PDM can be warned about downloading the spreadsheet AFTER this change.
                if (quantities.getBilled() != 0) {
                    log.debug(MessageFormat.format(
                            "Trying to update an already billed sample, PDO: {0}, sample: {1}, price item: {2}",
                            productOrder.getJiraTicketKey(), sampleName, priceItem.getName()));
                } else if (MathUtils.isSame(quantities.getUploaded(), quantity)) {
                    log.debug(MessageFormat.format(
                            "Sample already has the same quantity to bill, PDO: {0}, sample: {1}, price item: {2}, quantity {3}",
                            productOrder.getJiraTicketKey(), sampleName, priceItem.getName(), quantity));
                } else {
                    for (LedgerEntry item : getLedgerItems()) {
                        if (item.getPriceItem().equals(priceItem)) {
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

        Map<PriceItem, LedgerQuantities> sampleStatus = new HashMap<>();
        for (LedgerEntry item : ledgerItems) {
            if (!sampleStatus.containsKey(item.getPriceItem())) {
                sampleStatus.put(item.getPriceItem(), new LedgerQuantities());
            }

            if (item.isBilled()) {
                sampleStatus.get(item.getPriceItem()).addToBilled(item.getQuantity());
            } else {
                // The item is not part of a completed billed session or successfully billed item from an active session.
                sampleStatus.get(item.getPriceItem()).addToUploaded(item.getQuantity());
            }
        }

        return sampleStatus;
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
    public void addAutoLedgerItem(Date workCompleteDate, PriceItem priceItem, double delta, Date currentDate) {
        addLedgerItem(workCompleteDate, priceItem, delta, currentDate);
    }

    /**
     * This adds a manually uploaded ledger entry. The auto timestamp of NULL indicates that the upload happened, which
     * will lock out auto billing and sets to Can Bill.
     *
     * @param workCompleteDate The date completed.
     * @param priceItem        The price item to charge.
     * @param delta            The plus or minus value to bill to the quote server.
     */
    public void addLedgerItem(Date workCompleteDate, PriceItem priceItem, double delta) {
        addLedgerItem(workCompleteDate, priceItem, delta, null);
    }

    public void addLedgerItem(Date workCompleteDate, PriceItem priceItem, double delta, Date autoLedgerTimestamp) {
        LedgerEntry ledgerEntry = new LedgerEntry(this, priceItem, workCompleteDate, delta);
        ledgerEntry.setAutoLedgerTimestamp(autoLedgerTimestamp);
        ledgerItems.add(ledgerEntry);
        log.debug(MessageFormat.format(
                "Added LedgerEntry item for sample {0} to PDO {1} for PriceItemName: {2} - Quantity:{3}",
                sampleName, productOrder.getBusinessKey(), priceItem.getName(), delta));
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
        StringBuilder riskStringBuilder = new StringBuilder();

        if (isOnRisk()) {
            for (RiskItem riskItem : riskItems) {
                // We need to remove newlines and carriage returns as ETL will not accept the values.
                riskStringBuilder.append(riskItem.getInformation().replaceAll("\r?\n", " "));
                riskStringBuilder.append(" AND ");
            }
        }

        return formatAndString(riskStringBuilder.toString());
    }

    /**
     * @return A string of each {@link RiskItem}s {@link RiskCriterion} type for the sample.
     */
    public String getRiskTypeString() {
        StringBuilder riskTypeStringBuilder = new StringBuilder();

        if (isOnRisk()) {
            for (RiskItem riskItem : riskItems) {
                riskTypeStringBuilder.append(riskItem.getRiskCriterion().getType().getLabel());
                riskTypeStringBuilder.append(" AND ");
            }
        }

        return formatAndString(riskTypeStringBuilder.toString());
    }


    public String formatAndString(String criteriaString) {
        if (StringUtils.isBlank(criteriaString)) {
            return "";
        }

        // If the string had a value then we need to strip the trailing ' AND '.
        return criteriaString.substring(0, criteriaString.lastIndexOf(" AND "));
    }

    /**
     * Rolls up visibility of the samples availability from just the sample data
     */
    public boolean isSampleAvailable() {
        boolean available;

        switch (getMetadataSource()) {
        case BSP:
            available = getSampleData().isSampleReceived();
            break;
        case MERCURY:
            available = isSampleAccessioned();
            break;
        default:
            throw new IllegalStateException("The metadata Source is undetermined");
        }
        return available;
    }

    /**
     * Exposes if a sample has been Accessioned.
     */
    private boolean isSampleAccessioned() {
        boolean sampleAccessioned = true;
        if(mercurySample != null &&
           mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
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

        if (ledgerEntries == null) {
            return null;
        }

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

        if (ledgerEntries == null) {
            return null;
        }

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

}
