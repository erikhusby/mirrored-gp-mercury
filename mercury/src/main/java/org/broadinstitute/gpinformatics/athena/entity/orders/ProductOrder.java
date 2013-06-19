package org.broadinstitute.gpinformatics.athena.entity.orders;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
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

@SuppressWarnings("UnusedDeclaration")
@Entity
@Audited
@Table(name = "PRODUCT_ORDER", schema = "athena")
public class ProductOrder implements BusinessObject, Serializable {
    private static final long serialVersionUID = 2712946561792445251L;

    private static final String DRAFT_PREFIX = "Draft-";

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_ORDER", schema = "athena", sequenceName = "SEQ_PRODUCT_ORDER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_ORDER")
    private Long productOrderId;

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "PLACED_DATE")
    private Date placedDate;

    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    @Column(name = "MODIFIED_BY", nullable = false)
    private long modifiedBy;

    /** Unique title for the order */
    @Column(name = "TITLE", unique = true)
    private String title = "";

    @ManyToOne
    private ResearchProject researchProject;

    @OneToOne
    private Product product;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.Draft;

    /** Alphanumeric Id */
    @Column(name = "QUOTE_ID")
    private String quoteId = "";

    /** Additional comments on the order. */
    @Column(length = 2000)
    private String comments;

    @Column(name="FUNDING_DEADLINE")
    private Date fundingDeadline;

    @Column(name="PUBLICATION_DEADLINE")
    private Date publicationDeadline;

    /** Reference to the Jira Ticket created when the order is placed. Null means that the order is a draft. */
    @Column(name = "JIRA_TICKET_KEY", nullable = true)
    private String jiraTicketKey;

    @Column(name = "count")
    /** Counts the number of lanes, the default value is one lane. */
    private int laneCount = 1;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product_order", nullable = false)
    @OrderColumn(name = "SAMPLE_POSITION", nullable = false)
    @AuditJoinTable(name = "product_order_sample_join_aud")
    private final List<ProductOrderSample> samples = new ArrayList<>();

    @Transient
    private final SampleCounts sampleCounts = new SampleCounts();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "productOrder", orphanRemoval = true)
    private final Set<ProductOrderAddOn> addOns = new HashSet<>();

    // This is used for edit to keep track of changes to the object.
    @Transient
    private String originalTitle;

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalTitle = title;
    }

    /**
     * @return The business key is the jira ticket key when this is not a draft, otherwise it is the DRAFT_KEY plus the
     * internal database id.
     */
    @Override
    @Nonnull
    public String getBusinessKey() {
        return createBusinessKey(productOrderId, jiraTicketKey);
    }

    public String getAddOnList() {
        if (addOns.isEmpty()) {
            return "no Add-ons";
        }

        String[] addOnArray = new String[addOns.size()];
        int i = 0;
        for (ProductOrderAddOn poAddOn : addOns) {
            addOnArray[i++] = poAddOn.getAddOn().getProductName();
        }

        return StringUtils.join(addOnArray, ", ");
    }

    public int getLaneCount() {
        return laneCount;
    }

    public void setLaneCount(int laneCount) {
        this.laneCount = laneCount;
    }

    public void updateData(ResearchProject researchProject, Product product, List<Product> addOnProducts, List<ProductOrderSample> samples) {
        updateAddOnProducts(addOnProducts);
        this.product = product;
        this.researchProject = researchProject;
        setSamples(samples);
    }

    /**
     * This calculates risk for all samples in the order.
     *
     * @return The number of samples calculated to be on risk.
     */
    public int calculateRisk(List<ProductOrderSample> selectedSamples) {
        Set<String> uniqueSampleNamesOnRisk = new HashSet<>();

        for (ProductOrderSample sample : selectedSamples) {
            if (sample.calculateRisk()) {
                uniqueSampleNamesOnRisk.add(sample.getSampleName());
            }
        }

        return uniqueSampleNamesOnRisk.size();
    }

    /**
     * This calculates risk for all samples in the order.
     *
     * @return The number of samples calculated to be on risk.
     */
    public int calculateRisk() {
        return calculateRisk(samples);
    }

    /**
     * Utility method to create a PDO business key given an order ID and a JIRA ticket.
     *
     * @param productOrderId the order ID
     * @param jiraTicketKey the JIRA ticket, can be null
     * @return the business key for the PDO
     */
    public static String createBusinessKey(Long productOrderId, @Nullable String jiraTicketKey) {
        if (jiraTicketKey == null) {
            return DRAFT_PREFIX + productOrderId;
        }

        return jiraTicketKey;
    }

    /**
     * Count the number of unique samples on risk on the product order. This is calculated on the summary page, but
     * for this call, we only want to do the calculation without going to BSP.
     *
     * @return The count of items on risk.
     */
    public int countItemsOnRisk() {
        int count = 0;
        Set<String> uniqueKeys = new HashSet<>();

        for (ProductOrderSample sample : samples) {
            if (sample.isOnRisk() && (!uniqueKeys.contains(sample.getSampleName()))) {
                uniqueKeys.add(sample.getSampleName());
                count++;
            }
        }

        return count;
    }

    public static class JiraOrId {
        @Nullable
        public final String jiraTicketKey;
        public final long productOrderId;

        public JiraOrId(long productOrderId, @Nullable String jiraTicketKey) {
            this.jiraTicketKey = jiraTicketKey;
            this.productOrderId = productOrderId;
        }
    }

    /**
     * Use This method to convert from a business key to either a JIRA ID or a product order ID.
     * @param businessKey the key to convert
     * @return either a JIRA ID or a product order ID.
     */
    public static JiraOrId convertBusinessKeyToJiraOrId(@Nonnull String businessKey) {
        // This is currently happening in DEV at least, not sure why.
        //noinspection ConstantConditions
        if (businessKey == null) {
            return null;
        }

        if (businessKey.startsWith(DRAFT_PREFIX)) {
            return new JiraOrId(Long.parseLong(businessKey.substring(DRAFT_PREFIX.length())), null);
        }

        return new JiraOrId(0, businessKey);
    }

    private static class Counter implements Serializable {
        private static final long serialVersionUID = 4354572758557412220L;

        private final Map<String, Integer> countMap = new HashMap<>();

        private void clear() {
            countMap.clear();
        }

        private void increment(@Nonnull String key) {
            if (!StringUtils.isEmpty(key)) {
                Integer count = countMap.get(key);
                if (count == null) {
                    count = 0;
                }
                countMap.put(key, count + 1);
            }
        }

        private int get(@Nonnull String key) {
            Integer count = countMap.get(key);
            return count == null ? 0 : count;
        }

        private void output(List<String> output, @Nonnull String label, int compareCount) {
            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                output.add(MessageFormat.format(
                    "{0} ''{1}'': {2}", label, entry.getKey(), formatCountTotal(entry.getValue(), compareCount)));
            }
        }
    }

    private static Object formatCountTotal(int count, int compareCount) {
        if (count == 0) {
            return "None";
        }

        if (count == compareCount) {
            return "All";
        }

        return count;
    }

    /**
     * Class that encapsulates counting samples and storing the results.
     */
    private class SampleCounts implements Serializable {
        private static final long serialVersionUID = -6031146789417566007L;
        private boolean countsValid;
        private int totalSampleCount;
        private int onRiskCount;
        private int bspSampleCount;
        private int lastPicoCount;
        private int receivedSampleCount;
        private int activeSampleCount;
        private int hasFPCount;
        private int hasSampleKitUploadRackscanMismatch;
        private int missingBspMetaDataCount;
        private final Counter stockTypeCounter = new Counter();
        private final Counter primaryDiseaseCounter = new Counter();
        private final Counter genderCounter = new Counter();
        private final Counter sampleTypeCounter = new Counter();
        private int uniqueSampleCount;
        private int uniqueParticipantCount;

        /**
         * Go through all the samples and tabulate statistics.
         */
        private void generateCounts() {
            if (countsValid) {
                return;
            }

            // This gets the BSP data for every sample in the order.
            loadBspData();

            // Initialize all counts.
            clearAllData();

            Set<String> sampleSet = new HashSet<>(samples.size());
            Set<String> participantSet = new HashSet<>();

            for (ProductOrderSample sample : samples) {
                if (sampleSet.add(sample.getSampleName())) {
                    // This is a unique sample name, so do any counts that are only needed for unique names. Since
                    // BSP looks up samples by name, it would always get the same data, so only counting unique values.
                    if (sample.isInBspFormat()) {
                        bspSampleCount++;

                        if (sample.bspMetaDataMissing()) {
                            missingBspMetaDataCount++;
                        } else {
                            updateDTOCounts(participantSet, sample.getBspSampleDTO());
                        }
                    }
                }

                // We can calculate on risk for any sample on the product and it can be done differently for each
                // sample of the same name. Therefore, we want to calculate this for each item.
                if (sample.isOnRisk()) {
                    onRiskCount++;
                }
            }

            uniqueSampleCount = sampleSet.size();
            uniqueParticipantCount = participantSet.size();
            countsValid = true;
        }

        /**
         * initialize all the counters.
         */
        private void clearAllData() {
            totalSampleCount = samples.size();
            bspSampleCount = 0;
            receivedSampleCount = 0;
            activeSampleCount = 0;
            hasFPCount = 0;
            hasSampleKitUploadRackscanMismatch = 0;
            missingBspMetaDataCount = 0;
            onRiskCount = 0;
            stockTypeCounter.clear();
            primaryDiseaseCounter.clear();
            genderCounter.clear();
        }

        /**
         * Update all the counts related to BSP information.
         *
         * @param participantSet The unique collection of participants by Id.
         * @param bspDTO The BSP DTO.
         */
        private void updateDTOCounts(Set<String> participantSet, BSPSampleDTO bspDTO) {
            if (bspDTO.isSampleReceived()) {
                receivedSampleCount++;
            }

            if (bspDTO.isActiveStock()) {
                activeSampleCount++;
            }

            // If the pico has never been run then it is not warned in the last pico date highlighting.
            Date picoRunDate = bspDTO.getPicoRunDate();
            if ((picoRunDate == null) || picoRunDate.before(bspDTO.getOneYearAgo())) {
                lastPicoCount++;
            }

            stockTypeCounter.increment(bspDTO.getStockType());

            String participantId = bspDTO.getPatientId();
            if (StringUtils.isNotBlank(participantId)) {
                participantSet.add(participantId);
            }

            primaryDiseaseCounter.increment(bspDTO.getPrimaryDisease());
            genderCounter.increment(bspDTO.getGender());
            if (bspDTO.getHasFingerprint()) {
                hasFPCount++;
            }

            if (bspDTO.getHasSampleKitUploadRackscanMismatch()) {
                hasSampleKitUploadRackscanMismatch++;
            }

            sampleTypeCounter.increment(bspDTO.getSampleType());
        }

        /**
         * Format the number to say None if the value is zero.
         *
         * @param count The number to format
         */
        private void formatSummaryNumber(List<String> output, String message, int count) {
            output.add(MessageFormat.format(message, (count == 0) ? "None" : count));
        }

        /**
         * Format the number to say None if the value is zero, or All if it matches the comparison number.
         *
         * @param count The number to format
         * @param compareCount The number to compare to
         */
        private void formatSummaryNumber(List<String> output, String message, int count, int compareCount) {
            output.add(MessageFormat.format(message, formatCountTotal(count, compareCount)));
        }

        public List<String> sampleSummary() {
            generateCounts();

            List<String> output = new ArrayList<>();
            if (totalSampleCount == 0) {
                output.add("Total: None");
            } else {
                formatSummaryNumber(output, "Total: {0}", totalSampleCount);

                formatSummaryNumber(output, "Unique: {0}", uniqueSampleCount, totalSampleCount);
                formatSummaryNumber(output, "Duplicate: {0}", totalSampleCount - uniqueSampleCount, totalSampleCount);

                formatSummaryNumber(output, "On Risk: {0}", onRiskCount, totalSampleCount);

                if (bspSampleCount == uniqueSampleCount) {
                    output.add("From BSP: All");
                } else if (bspSampleCount != 0) {
                    formatSummaryNumber(output, "Unique BSP: {0}", bspSampleCount);
                    formatSummaryNumber(output, "Unique Not BSP: {0}", uniqueSampleCount - bspSampleCount);
                } else {
                    output.add("From BSP: None");
                }
            }

            if (uniqueParticipantCount != 0) {
                formatSummaryNumber(output, "Unique Participants: {0}", uniqueParticipantCount, totalSampleCount);
            }

            stockTypeCounter.output(output, "Stock Type", totalSampleCount);
            primaryDiseaseCounter.output(output, "Disease", totalSampleCount);
            genderCounter.output(output, "Gender", totalSampleCount);
            sampleTypeCounter.output(output, "Sample Type", totalSampleCount);

            if (hasFPCount != 0) {
                formatSummaryNumber(output, "Fingerprint Data: {0}", hasFPCount, totalSampleCount);
            }

            if (getProduct().isSupportsPico()) {
                formatSummaryNumber(output, "Last Pico over a year ago: {0}",
                        lastPicoCount);
            }

            if (hasSampleKitUploadRackscanMismatch != 0) {
                formatSummaryNumber(output, "<div class=\"text-error\">Rackscan Mismatch: {0}</div>",
                        hasSampleKitUploadRackscanMismatch, totalSampleCount);
            }

            return output;
        }

        private void checkCount(int count, String message, List<String> output) {
            if (count != 0) {
                output.add(MessageFormat.format(message, count));
            }
        }

        /**
         * Check to see if the samples are valid.
         * - for all BSP formatted sample IDs, do we have BSP data?
         * - all BSP samples are RECEIVED
         * - all BSP samples' stock is ACTIVE
         */
        public List<String> sampleValidation() {
            List<String> output = new ArrayList<>();
            checkCount(missingBspMetaDataCount, "No BSP Data: {0}", output);
            checkCount(bspSampleCount - activeSampleCount, "Not ACTIVE: {0}", output);
            checkCount(bspSampleCount - receivedSampleCount, "Not RECEIVED: {0}", output);
            return output;
        }

        public void invalidate() {
            countsValid = false;
        }
    }

    /**
     * Default no-arg constructor, also used when creating a new ProductOrder.
     */
    public ProductOrder() {
    }

    /**
     * Constructor called when creating a new ProductOrder.
     */
    public ProductOrder(@Nonnull BspUser createdBy, ResearchProject researchProject) {
        this(createdBy.getUserId(), "", new ArrayList<ProductOrderSample>(), "", null, researchProject);
    }

    /**
     * Used for test purposes only.
     */
    public ProductOrder(@Nonnull Long creatorId, @Nonnull String title,
                        @Nonnull List<ProductOrderSample> samples, String quoteId,
                        Product product, ResearchProject researchProject) {

        // Set the dates and modified values so that tests don't have to call prepare and will never create bogus
        // data. Before adding this, tests were being created with null values, which caused problems, so taking out
        // the requirement on prepare.
        Date now = new Date();
        createdBy = creatorId;
        createdDate = now;
        modifiedBy = creatorId;
        modifiedDate = now;

        this.title = title;
        setSamples(samples);
        this.quoteId = quoteId;
        this.product = product;
        this.researchProject = researchProject;
    }

    /**
     * Call this method before saving changes to the database.  It updates the modified date and modified user.
     * @param user the user doing the save operation.
     */
    public void prepareToSave(BspUser user) {
        prepareToSave(user, false);
    }

    /**
     * Call this method before saving changes to the database.  It updates the modified date and modified user,
     * and sets the create date and create user if these haven't been set yet.
     * @param user the user doing the save operation.
     * @param isCreating true if creating a new PDO
     */
    public void prepareToSave(BspUser user, boolean isCreating) {
        Date now = new Date();
        long userId = user.getUserId();
        if (isCreating) {
            // createdBy is now set in the UI.
            if (createdBy == null) {
                // Used by tests only.
                createdBy = userId;
            }
            createdDate = now;
        }
        modifiedBy = userId;
        modifiedDate = now;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getName() {
        return getTitle();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ProductOrderAddOn> getAddOns() {
        return ImmutableList.copyOf(addOns);
    }

    public void updateAddOnProducts(List<Product> addOnList) {
        addOns.clear();
        for (Product addOn : addOnList) {
            addOns.add(new ProductOrderAddOn(addOn, this));
        }
    }

    public void setProductOrderAddOns(Collection<ProductOrderAddOn> addOns) {
        this.addOns.clear();
        this.addOns.addAll(addOns);
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        this.researchProject = researchProject;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getFundingDeadline() {
        return fundingDeadline;
    }

    public void setFundingDeadline(Date fundingDeadline) {
        this.fundingDeadline = fundingDeadline;
    }

    public Date getPublicationDeadline() {
        return publicationDeadline;
    }

    public void setPublicationDeadline(Date publicationDeadline) {
        this.publicationDeadline = publicationDeadline;
    }

    public List<ProductOrderSample> getSamples() {
        return samples;
    }

    private void addSamplesInternal(List<ProductOrderSample> newSamples, int samplePos) {
        for (ProductOrderSample sample : newSamples) {
            sample.setProductOrder(this);
            sample.setSamplePosition(samplePos++);
            samples.add(sample);
        }
        sampleCounts.invalidate();
    }

    public void setSamples(@Nonnull List<ProductOrderSample> samples) {
        if (samples.isEmpty()) {
            // FIXME: This seems incorrect in the case where current sample list is non-empty and incoming samples are empty.
            return;
        }

        // Only update samples if the new sample list is different from the old one.
        if (isSampleListDifferent(samples)) {
            this.samples.clear();

            addSamplesInternal(samples, 0);
        }
    }

    public void addSamples(@Nonnull List<ProductOrderSample> newSamples) {
        if (samples.isEmpty()) {
            setSamples(newSamples);
        } else {
            int samplePos = samples.get(samples.size() - 1).getSamplePosition();
            addSamplesInternal(newSamples, samplePos);
        }
    }

    /**
     * Determine if the list of samples names is exactly the same as the original list of sample names.
     *
     * @param newSamples new sample list
     *
     * @return true, if the name lists are different
     */
    private boolean isSampleListDifferent(List<ProductOrderSample> newSamples) {
        List<String> originalSampleNames = ProductOrderSample.getSampleNames(samples);
        List<String> newSampleNames = ProductOrderSample.getSampleNames(newSamples);

        // If not equals, then this has changed.
        return !newSampleNames.equals(originalSampleNames);
    }

    /**
     * @return If any sample has ledger items, then return true. Otherwise return false
     */
    private boolean hasLedgerItems() {
        for (ProductOrderSample sample : samples) {
            if (!sample.getLedgerItems().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Allows a user of this class to gain access to the Unique key representing the Jira Ticket for
     * which this Product ProductOrder is associated.
     *
     * @return a {@link String} that represents the unique Jira Ticket key.
     */
    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    /**
     * Used for test purposes only.
     */
    public void setJiraTicketKey(String jiraTicketKey) {
        this.jiraTicketKey = jiraTicketKey;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    /**
     * Call this method to change the 'owner' attribute of a Product Order.  We are currently storing this
     * attribute in the created by column in the database.
     *
     * @param userId the owner ID
     */
    public void setCreatedBy(@Nullable Long userId) {
        createdBy = userId;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getPlacedDate() {
        return placedDate;
    }

    /**
     * This should only be called from tests or for database backpopulation.  The placed date is normally set internally
     * when an order is placed.
     * @param placedDate the date to set
     */
    public void setPlacedDate(Date placedDate) {
        this.placedDate = placedDate;
    }

    /**
     * Use the BSP Manager to load the bsp data for every sample in this product order.
     */
    public void loadBspData() {
        // Can't use SampleCounts here, it calls this recursively.
        Set<String> uniqueNames = new HashSet<>(samples.size());
        for (ProductOrderSample sample : samples) {
            if (sample.needsBspMetaData()) {
                uniqueNames.add(sample.getSampleName());
            }
        }

        if (uniqueNames.isEmpty()) {
            // This early return is needed to avoid making a unnecessary injection, which could cause
            // DB Free automated tests to fail.
            return;
        }

        loadBspData(uniqueNames, getSamples());
    }

    public static void loadBspData(Collection<String> names, List<ProductOrderSample> samples) {

        BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
        Map<String, BSPSampleDTO> bspSampleMetaData = bspSampleDataFetcher.fetchSamplesFromBSP(names);

        // The non-null DTOs which we use to look up FFPE status.
        List<BSPSampleDTO> nonNullDTOs = new ArrayList<>();
        for (ProductOrderSample sample : samples) {
            BSPSampleDTO bspSampleDTO = bspSampleMetaData.get(sample.getSampleName());

            // If the DTO is null, we do not need to set it because it defaults to DUMMY inside sample.
            if (bspSampleDTO != null) {
                sample.setBspSampleDTO(bspSampleDTO);
                nonNullDTOs.add(bspSampleDTO);
            }
        }

        // Fill out all the non-null DTOs with FFPE status in one shot.
        bspSampleDataFetcher.fetchFFPEDerived(nonNullDTOs);
    }

    // Return the sample counts object to allow call chaining.
    private SampleCounts updateSampleCounts() {
        sampleCounts.generateCounts();
        return sampleCounts;
    }

    /**
     * Provides the summation of all unique participants represented in the list of samples
     * registered to this product order.
     *
     * @return a count of every participant that is represented by at least one sample in the list of product order
     *         samples
     */
    public int getUniqueParticipantCount() {
        return updateSampleCounts().uniqueParticipantCount;
    }

    /**
     * @return the number of unique samples, as determined by the sample name
     */
    public int getUniqueSampleCount() {
        return updateSampleCounts().uniqueSampleCount;
    }

    /**
     * @return the list of sample names for this order, including duplicates. in the same sequence that
     * they were entered.
     */
    public String getSampleString() {
        return StringUtils.join(ProductOrderSample.getSampleNames(samples), '\n');
    }

    /**
     * Exposes how many samples are registered to this product order.
     *
     * @return a count of all samples registered to this product order
     */
    public int getTotalSampleCount() {
        return samples.size();
    }

    /**
     * Exposes how many samples registered to this product order are represented by more than one
     * sample in the list.
     *
     * @return a count of all samples that have more than one entry in the registered sample list
     */
    public int getDuplicateCount() {
        updateSampleCounts();
        return sampleCounts.totalSampleCount - sampleCounts.uniqueSampleCount;
    }

    /**
     * Exposes how many of the samples, which are registered to this product order, are from BSP.
     *
     * @return a count of all product order samples that come from bsp
     */
    public int getBspSampleCount() {
        return updateSampleCounts().bspSampleCount;
    }

    public int getTumorCount() {
        return updateSampleCounts().sampleTypeCounter.get(ProductOrderSample.TUMOR_IND);
    }

    public int getNormalCount() {
        return updateSampleCounts().sampleTypeCounter.get(ProductOrderSample.NORMAL_IND);
    }

    public int getFemaleCount() {
        return updateSampleCounts().genderCounter.get(ProductOrderSample.FEMALE_IND);
    }

    public int getMaleCount() {
        return updateSampleCounts().genderCounter.get(ProductOrderSample.MALE_IND);
    }

    /**
     * Calculates how many samples registered to this product order have a fingerprint associated
     * with it.
     *
     * @return a count of the samples that have a fingerprint
     */
    public int getFingerprintCount() {
        return updateSampleCounts().hasFPCount;
    }

    /**
     * Exposes the summation of each unique stock type found in the list of samples registered to
     * this product order.
     *
     * @return a Map, indexed by the unique stock type found, which gives a count of how many samples in the list of
     *         product order samples, are related to that stock type
     */
    public Map<String, Integer> getCountsByStockType() {
        return updateSampleCounts().stockTypeCounter.countMap;
    }

    /**
     * Helper method that determines how many BSP samples registered to this product order
     * are marked as Received.
     *
     * @return a count of all samples in this product order that are in a RECEIVED state
     */
    public int getReceivedSampleCount() {
        return updateSampleCounts().receivedSampleCount;
    }

    /**
     * Helper method that determines how many BSP samples registered to the product order are
     * in an Active state.
     *
     * @return a count of all samples in this product order that are in an ACTIVE state
     */
    public int getActiveSampleCount() {
        return updateSampleCounts().activeSampleCount;
    }

    public int getMissingBspMetaDataCount() {
        return updateSampleCounts().missingBspMetaDataCount;
    }

    public Long getProductOrderId() {
        return productOrderId;
    }

    /**
     * @return true if order is in Draft
     */
    public boolean isDraft() {
        return OrderStatus.Draft == orderStatus;
    }

    /**
     * @return true if order is abandoned
     */
    public boolean isAbandoned() {
        return OrderStatus.Abandoned == orderStatus;
    }

    /**
     * @return true if order is submitted
     */
    public boolean isSubmitted() {
        return OrderStatus.Submitted == orderStatus;
    }

    /**
     * This method encapsulates the set of steps necessary to finalize the submission of a product order.
     * This mainly deals with jira ticket creation.  This method will:
     * <ul>
     * <li>Create a new jira ticket and persist the reference to the ticket key</li>
     * <li>assign the submitter as a watcher to the ticket</li>
     * <li>Add a new comment listing all Samples contained within the order</li>
     * <li>Add any validation comments regarding the Samples contained within the order</li>
     * </ul>
     *
     * @throws IOException
     */
    public void placeOrder() throws IOException {
        placedDate = new Date();
        JiraService jiraService = ServiceAccessUtility.getBean(JiraService.class);
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

        List<CustomField> listOfFields = new ArrayList<>();

        listOfFields.add(new CustomField(submissionFields, JiraField.PRODUCT_FAMILY,
                product.getProductFamily() == null ? "" : product.getProductFamily().getName()));

        listOfFields.add(new CustomField(submissionFields, JiraField.PRODUCT,
                product.getProductName() == null ? "" : product.getProductName()));

        if (quoteId != null && !quoteId.isEmpty()) {
            listOfFields.add(new CustomField(submissionFields, JiraField.QUOTE_ID, quoteId));
        }

        listOfFields.add(new CustomField(submissionFields, JiraField.SAMPLE_IDS, getSampleString()));

        if (publicationDeadline != null) {
            listOfFields.add(new CustomField(submissionFields, JiraField.PUBLICATION_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(publicationDeadline)));
        }

        if (fundingDeadline != null) {
            listOfFields.add(new CustomField(submissionFields, JiraField.FUNDING_DEADLINE,
                    JiraService.JIRA_DATE_FORMAT.format(getFundingDeadline())));
        }

        BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);

        JiraIssue issue = jiraService.createIssue(
                fetchJiraProject().getKeyPrefix(), bspUserList.getById(createdBy).getUsername(),
                fetchJiraIssueType(), title, comments == null ? "" : comments, listOfFields);

        jiraTicketKey = issue.getKey();
        issue.addLink(researchProject.getJiraTicketKey());

        issue.addComment(StringUtils.join(getSampleSummaryComments(), "\n"));
        issue.addComment(StringUtils.join(getSampleValidationComments(), "\n"));
    }

    /**
     * This is a helper method encapsulating the validations run against the samples contained
     * within this product order.  The results of these validation checks are then added to the existing Jira Ticket.
     */
    public List<String> getSampleSummaryComments() {
        return updateSampleCounts().sampleSummary();
    }

    public List<String> getSampleValidationComments() {
        return updateSampleCounts().sampleValidation();
    }

    /**
     * @return true if all samples are of BSP Format. Note:
     * will return false if there are no samples on the sheet.
     */
    public boolean areAllSampleBSPFormat() {
        updateSampleCounts();
        return sampleCounts.bspSampleCount == sampleCounts.uniqueSampleCount;
    }

    /**
     * Validates the existence of samples in the product order.
     *
     * @return true if there are no samples currently assigned to this product order
     */
    private boolean isSheetEmpty() {
        return samples.isEmpty();
    }

    /**
     * This is a helper method that binds a specific Jira project to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum that represents the Jira Project for Product Orders
     */
    public CreateFields.ProjectType fetchJiraProject() {
        return CreateFields.ProjectType.PRODUCT_ORDERING;
    }

    /**
     * This is a helper method that binds a specific Jira Issue Type to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum that represents the Jira Issue Type for Product Orders
     */
    public CreateFields.IssueType fetchJiraIssueType() {
        return CreateFields.IssueType.PRODUCT_ORDER;
    }

    /**
     * This is used to help create or update a PDO's Jira ticket.
     */
    public enum JiraField implements CustomField.SubmissionField {
        PRODUCT_FAMILY("Product Family"),
        PRODUCT("Product"),
        QUOTE_ID("Quote ID"),
        MERCURY_URL("Mercury URL"),
        SAMPLE_IDS("Sample IDs"),
        REPORTER("Reporter"),
        FUNDING_DEADLINE("Funding Deadline"),
        PUBLICATION_DEADLINE("Publication Deadline"),
        STATUS("Status");

        private final String fieldName;

        private JiraField(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        @Nonnull @Override
        public String getFieldName() {
            return fieldName;
        }
    }

    public enum OrderStatus implements StatusType {
        Draft,
        Submitted,
        Abandoned,
        Completed;

        @Override
        public String getDisplayName() {
            return name();
        }

        /**
         * Get all status values using the name strings.
         *
         * @param statusStrings The desired list of statuses.
         * @return The statuses that are listed.
         */
        public static List<OrderStatus> getFromName(@Nonnull List<String> statusStrings) {
            if (CollectionUtils.isEmpty(statusStrings)) {
                return Collections.emptyList();
            }

            List<OrderStatus> statuses = new ArrayList<>();
            for (String statusString : statusStrings) {
                statuses.add(OrderStatus.valueOf(statusString));
            }

            return statuses;
        }
    }

    @Override
    public boolean equals(Object other) {

        if ((this == other)) {
            return true;
        }

        if (!(other instanceof ProductOrder)) {
            return false;
        }

        ProductOrder castOther = (ProductOrder) other;
        return new EqualsBuilder().append(getBusinessKey(), castOther.getBusinessKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getBusinessKey()).toHashCode();
    }

    public boolean hasJiraTicketKey() {
        return !StringUtils.isBlank(jiraTicketKey);
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    /**
     * Update this order's status based on the state of its samples.  The status is only changed if the current
     * status is Submitted or Completed.
     * <p/>
     * A order is complete if all samples are either billed or abandoned.
     *
     * @return true if the status was changed.
     */
    public boolean updateOrderStatus() {
        if (orderStatus != OrderStatus.Submitted && orderStatus != OrderStatus.Completed) {
            // We only support automatic status transitions from Submitted or Complete states.
            return false;
        }
        OrderStatus newStatus = OrderStatus.Completed;
        for (ProductOrderSample sample : samples) {
            if (sample.getDeliveryStatus() != ProductOrderSample.DeliveryStatus.ABANDONED
                && !sample.isCompletelyBilled()) {
                // Found an incomplete item.
                newStatus = OrderStatus.Submitted;
                break;
            }
        }

        if (newStatus != orderStatus) {
            orderStatus = newStatus;
            return true;
        }
        return false;
    }

    public enum LedgerStatus {
        READY_TO_BILL, READY_FOR_REVIEW, NOTHING_NEW
    }
}
