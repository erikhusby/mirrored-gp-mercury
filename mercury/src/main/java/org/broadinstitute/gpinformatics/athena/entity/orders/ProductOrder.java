package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.IssueTransitionResponse;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

/**
 * Class to model the concept of a Product ProductOrder that can be created
 * by the Program PM and subsequently submitted to a lims system.
 * Currently supports the concept associating a product with a set of samples withe a quote.
 * For more detail on the purpose of the ProductOrder, see the user stories listed on
 *
 * @see <a href="https://confluence.broadinstitute.org/x/kwPGAg</a>
 *      <p/>
 *      Created by IntelliJ IDEA.
 *      User: mccrory
 *      Date: 8/28/12
 *      Time: 10:25 AM
 */
@Entity
@Audited
@Table(name = "PRODUCT_ORDER", schema = "athena")
public class ProductOrder implements Serializable {
    private static final String JIRA_SUBJECT_PREFIX = "Product order for ";

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_ORDER", schema = "athena", sequenceName = "SEQ_PRODUCT_ORDER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_ORDER")
    private Long productOrderId;

    private Date createdDate;

    private Long createdBy;

    private Date modifiedDate;

    private Long modifiedBy;

    /** Unique title for the order */
    @Column(unique = true)
    private String title;

    @ManyToOne
    private ResearchProject researchProject;

    @OneToOne
    private Product product;

    private OrderStatus orderStatus = OrderStatus.Draft;

    /** Alphanumeric Id */
    private String quoteId;

    /** Additional comments of the order */
    @Column(length = 2000)
    private String comments;

    /** Reference to the Jira Ticket created when the order is submitted */
    @Column(name = "JIRA_TICKET_KEY", nullable = false)
    private String jiraTicketKey;

    @Column(name = "count")
    /** counts the number of lanes; the default value is one lane */
    private int count = 1;

    /** Counts the number of rows in the one-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @NotAudited
    @Formula("(select count(*) from athena.product_order_sample pos where pos.product_order = product_order_id)")
    private Integer pdoSampleCount = 0;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product_order", nullable = false)
    @OrderColumn(name = "SAMPLE_POSITION", nullable = false)
    @AuditJoinTable(name = "product_order_sample_join_aud")
    private List<ProductOrderSample> samples = Collections.emptyList();

    @Transient
    private String sampleBillingSummary;

    @Transient
    private final SampleCounts counts = new SampleCounts();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "productOrder", orphanRemoval = true)
    private Set<ProductOrderAddOn> addOns = new HashSet<ProductOrderAddOn>();

    public String getBusinessKey() {
        return jiraTicketKey;
    }

    public boolean isInDB() {
        return productOrderId != null;
    }

    public String getAddOnList() {
        if (addOns.isEmpty()) {
            return "no Add-ons";
        }

        String[] addOnArray = new String[addOns.size()];
        int i=0;
        for (ProductOrderAddOn poAddOn : addOns) {
            addOnArray[i++] = poAddOn.getAddOn().getProductName();
        }

        return StringUtils.join(addOnArray, ", ");
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Class that encapsulates counting samples and storing the results.
     */
    private class SampleCounts implements Serializable {
        private static final long serialVersionUID = -6031146789417566007L;
        private boolean countsValid;
        private int totalSampleCount;
        private int bspSampleCount;
        private int receivedSampleCount;
        private int activeSampleCount;
        private int hasFPCount;
        private int missingBspMetaDataCount;
        private final Map<String, Integer> stockTypeCounts = new HashMap<String, Integer>();
        private final Map<String, Integer> primaryDiseaseCounts = new HashMap<String, Integer>();
        private final Map<String, Integer> genderCounts = new HashMap<String, Integer>();
        private final Map<String, Integer> sampleTypeCounts = new HashMap<String, Integer>();
        private int uniqueSampleCount;
        private int uniqueParticipantCount;

        private void incrementCount(Map<String, Integer> countMap, String key) {
            if (!StringUtils.isEmpty(key)) {
                Integer count = countMap.get(key);
                if (count == null) {
                    count = 0;
                }
                countMap.put(key, count + 1);
            }
        }

        private void generateCounts() {
            if (countsValid) {
                return;
            }

            loadBspData();

            totalSampleCount = samples.size();
            bspSampleCount = 0;
            receivedSampleCount = 0;
            activeSampleCount = 0;
            hasFPCount = 0;
            missingBspMetaDataCount = 0;
            stockTypeCounts.clear();
            primaryDiseaseCounts.clear();
            genderCounts.clear();
            Set<String> sampleSet = new HashSet<String>(samples.size());
            Set<String> participantSet = new HashSet<String>();

            for (ProductOrderSample sample : samples) {
                if (sampleSet.add(sample.getSampleName())) {
                    if (sample.isInBspFormat()) {
                        bspSampleCount++;

                        if (sample.bspMetaDataMissing()) {
                            missingBspMetaDataCount++;
                        }

                        BSPSampleDTO bspDTO = sample.getBspDTO();
                        if (bspDTO.isSampleReceived()) {
                            receivedSampleCount++;
                        }
                        if (bspDTO.isActiveStock()) {
                            activeSampleCount++;
                        }

                        incrementCount(stockTypeCounts, bspDTO.getStockType());

                        String participantId = bspDTO.getPatientId();
                        if (StringUtils.isNotBlank(participantId)) {
                            participantSet.add(participantId);
                        }

                        incrementCount(primaryDiseaseCounts, bspDTO.getPrimaryDisease());
                        incrementCount(genderCounts, bspDTO.getGender());
                        if (bspDTO.getHasFingerprint()) {
                            hasFPCount++;
                        }

                        incrementCount(sampleTypeCounts, bspDTO.getSampleType());
                    }
                }
            }
            uniqueSampleCount = sampleSet.size();
            uniqueParticipantCount = participantSet.size();
            countsValid = true;
        }

        private void outputCounts(List<String> output, Map<String, Integer> counts, String label) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                output.add(MessageFormat.format("{0} ''{1}'': {2}", label, entry.getKey(), entry.getValue()));
            }
        }

        public List<String> sampleSummary() {
            generateCounts();

            List<String> output = new ArrayList<String>();
            if (totalSampleCount == 0) {
                output.add("Total: None");
            } else {
                output.add(MessageFormat.format("Total: {0}", totalSampleCount));
                if (uniqueSampleCount != totalSampleCount) {
                    output.add(MessageFormat.format("Unique: {0}", uniqueSampleCount));
                    output.add(MessageFormat.format("Duplicate: {0}", (totalSampleCount - uniqueSampleCount)));
                }

                if (bspSampleCount == uniqueSampleCount) {
                    output.add("From BSP: All");
                } else if (bspSampleCount != 0) {
                    output.add(MessageFormat.format("Unique BSP: {0}", bspSampleCount));
                    output.add(MessageFormat.format("Unique Not BSP: {0}", uniqueSampleCount - bspSampleCount));
                } else {
                    output.add("From BSP: None");
                }
            }

            if (uniqueParticipantCount != 0) {
                output.add(MessageFormat.format("Unique Participants: {0}", uniqueParticipantCount));
            }

            if (receivedSampleCount != 0) {
                output.add(MessageFormat.format("RECEIVED: {0}", receivedSampleCount));
            }

            outputCounts(output, stockTypeCounts, "Stock Type");
            outputCounts(output, primaryDiseaseCounts, "Disease");
            outputCounts(output, genderCounts, "Gender");
            outputCounts(output, sampleTypeCounts, "Sample Type");

            if (hasFPCount != 0) {
                output.add(MessageFormat.format("Fingerprint Data: {0}", hasFPCount));
            }

            return output;
        }

        private void checkCount(int count, String message, List<String> output) {
            if (count != 0) {
                output.add(MessageFormat.format(message, count));
            }
        }

        // TODO: Will need to implement more extensive validations vs specific products; will need to move these
        // checks elsewhere.
        /**
         * Check to see if the samples are valid.
         * - for all BSP formatted sample IDs, do we have BSP data?
         * - all BSP samples are RECEIVED
         * - all BSP samples' stock is ACTIVE
         */
        public List<String> sampleValidation() {
            List<String> output = new ArrayList<String>();
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
     * Default no-arg constructor
     */
    ProductOrder() {
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
    public ProductOrder(@Nonnull Long creatorId, @Nonnull String title, List<ProductOrderSample> samples, String quoteId,
                        Product product, ResearchProject researchProject) {
        createdBy = creatorId;
        createdDate = new Date();
        modifiedBy = createdBy;
        modifiedDate = createdDate;
        this.title = title;
        setSamples(samples);
        this.quoteId = quoteId;
        this.product = product;
        this.researchProject = researchProject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ProductOrderAddOn> getAddOns() {
        List<ProductOrderAddOn> addOnList = new ArrayList<ProductOrderAddOn>();
        if ((addOns != null) && (!addOns.isEmpty())) {
            addOnList.addAll(addOns);
        }

        return addOnList;
    }

    public void updateAddOnProducts(List<Product> addOnList) {
        addOns.clear();
        for (Product addOn : addOnList) {
            addOns.add(new ProductOrderAddOn(addOn, this));
        }
    }

    public void setAddOns(Set<ProductOrderAddOn> addOns) {
        this.addOns = addOns;
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

    public List<ProductOrderSample> getSamples() {
        return samples;
    }

    public void setSamples(List<ProductOrderSample> samples) {
        this.samples = samples;
        if (samples != null) {
            for (ProductOrderSample sample : samples) {
                sample.setProductOrder(this);
            }
        }
        counts.invalidate();
        sampleBillingSummary = null;
    }

    /**
     * getJiraTicketKey allows a user of this class to gain access to the Unique key representing the Jira Ticket for
     * which this Product ProductOrder is associated
     *
     * @return a {@link String} that represents the unique Jira Ticket key
     */
    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    /**
     * Used for test purposes only.
     */
    public void setJiraTicketKey(@Nonnull String jiraTicketKey) {
        if (jiraTicketKey == null) {
            throw new NullPointerException("Jira Ticket Key cannot be null");
        }
        this.jiraTicketKey = jiraTicketKey;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * Use the BSP Manager to load the bsp data for every sample in this product order.
     */
    public void loadBspData() {
        // Can't use SampleCounts here, it calls this recursively.
        Set<String> uniqueNames = new HashSet<String>(samples.size());
        for (ProductOrderSample sample : samples) {
            if (sample.needsBspMetaData()) {
                uniqueNames.add(sample.getSampleName());
            }
        }

        if (uniqueNames.isEmpty()) {
            // No BSP samples, nothing to do.
            return;
        }

        BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
        Map<String, BSPSampleDTO> bspSampleMetaData = bspSampleDataFetcher.fetchSamplesFromBSP(uniqueNames);
        for (ProductOrderSample sample : getSamples()) {
            BSPSampleDTO bspSampleDTO = bspSampleMetaData.get(sample.getSampleName());
            if (bspSampleDTO == null) {
                bspSampleDTO = BSPSampleDTO.DUMMY;
            }
            sample.setBspDTO(bspSampleDTO);
        }
    }

    /**
     * getUniqueParticipantCount provides the summation of all unique participants represented in the list of samples
     * registered to this product order
     *
     * @return a count of every participant that is represented by at least one sample in the list of product order
     *         samples
     */
    public int getUniqueParticipantCount() {
        counts.generateCounts();
        return counts.uniqueParticipantCount;
    }

    /**
     * @return the number of unique samples, as determined by the sample name
     */
    public int getUniqueSampleCount() {
        counts.generateCounts();
        return counts.uniqueSampleCount;
    }

    /**
     * @return the list of sample names for this order, including duplicates. in the same sequence that
     * they were entered.
     */
    private List<String> getSampleNames() {
        List<String> names = new ArrayList<String>(samples.size());
        for (ProductOrderSample productOrderSample : samples) {
            names.add(productOrderSample.getSampleName());
        }
        return names;
    }

    /**
     * getTotalSampleCount exposes how many samples are registered to this product order
     *
     * @return a count of all samples registered to this product order
     */
    public int getTotalSampleCount() {
        if (samples == null) {
            return 0;
        }
        return samples.size();
    }

    /**
     * getDuplicateCount exposes how many samples registered to this product order are represented by more than one
     * sample in the list
     *
     * @return a count of all samples that have more than one entry in the registered sample list
     */
    public int getDuplicateCount() {
        counts.generateCounts();
        return counts.totalSampleCount - counts.uniqueSampleCount;
    }

    /**
     * getBspSampleCount exposes how many of the samples, which are registered to this product order, are from BSP
     *
     * @return a count of all product order samples that come from bsp
     */
    public int getBspSampleCount() {
        counts.generateCounts();
        return counts.bspSampleCount;
    }

    public int getTumorCount() {
        counts.generateCounts();
        Integer count = counts.sampleTypeCounts.get(BSPSampleDTO.TUMOR_IND);
        return count == null ? 0 : count;
    }

    public int getNormalCount() {
        counts.generateCounts();
        Integer count = counts.sampleTypeCounts.get(BSPSampleDTO.NORMAL_IND);
        return count == null ? 0 : count;
    }

    public int getFemaleCount() {
        counts.generateCounts();
        Integer count = counts.genderCounts.get(BSPSampleDTO.FEMALE_IND);
        return count == null ? 0 : count;
    }

    public int getMaleCount() {
        counts.generateCounts();
        Integer count = counts.genderCounts.get(BSPSampleDTO.MALE_IND);
        return count == null ? 0 : count;
    }

    /**
     * getBilledNotBilledCounts calculates both how many samples registered to this product have been billed and how
     * many samples have not been billed
     *
     * @return an instance of a BilledNotBilledCounts object which exposes both the Billed counts and the not billed
     *         counts
     */
    public BilledNotBilledCounts getBilledNotBilledCounts() {
        return new BilledNotBilledCounts(getBillingStatusCount(BillingStatus.Billed),
                getBillingStatusCount(BillingStatus.NotYetBilled));
    }

    /**
     * getElligibleForBillingCounts calculates how many samples are eligible to be billed
     *
     * @return a count of all samples registered to this product order that are eligible for billing
     */
    public int getElligibleForBillingCounts() {
        return getBillingStatusCount(BillingStatus.EligibleForBilling);
    }

    /**
     * getNotBillableCounts calculates how many samples have not been billed
     *
     * @return a count of all samples registered to this product order that have not been billed
     */
    public int getNotBillableCounts() {
        return getBillingStatusCount(BillingStatus.NotBillable);
    }

    /**
     * getBillingStatusCount calculates how many samples registered to this product order have a billing status that
     * matches targetStatus
     *
     * @param targetStatus an instance of a BillingStatus enum for which the user wishes to compare against the list
     *                     of Product Order Samples registered to this Product Order
     * @return a count of all samples that have a billing status that matches the given billing status
     */
    private int getBillingStatusCount(BillingStatus targetStatus) {
        int statusCount = 0;

        for (ProductOrderSample sample : samples) {
            if (targetStatus == sample.getBillingStatus()) {
                statusCount++;
            }
        }

        return statusCount;
    }

    /**
     * getFingerprintCount calculates how many samples registered to this product order have a fingerprint associated
     * with it
     *
     * @return a count of the samples that have a fingerprint
     */
    public int getFingerprintCount() {
        counts.generateCounts();
        return counts.hasFPCount;
    }

    /**
     * getCountsByStockType exposes the summation of each unique stock type found in the list of samples registered to
     * this product order
     *
     * @return a Map, indexed by the unique stock type found, which gives a count of how many samples in the list of
     *         product order samples, are related to that stock type
     */
    public Map<String, Integer> getCountsByStockType() {
        counts.generateCounts();
        return counts.stockTypeCounts;
    }

    /**
     * getSampleTypeCount is a helper method to expose the sum of all samples, registered to this product order,
     * based on a given sample type
     *
     * @param sampleTypeInd a String representing the type of sample for which we wish to get a count
     * @return a count of all samples that have a sample type matching the value passed in.
     */
    private int getSampleTypeCount(String sampleTypeInd) {
        int counter = 0;
        for (ProductOrderSample sample : samples) {
            if (sample.isInBspFormat() && sampleTypeInd.equals(sample.getBspDTO().getSampleType())) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * getReceivedSampleCount is a helper method that determines how many BSP samples registered to this product order
     * are marked as Received
     *
     * @return a count of all samples in this product order that are in a RECEIVED state
     */
    public int getReceivedSampleCount() {
        counts.generateCounts();
        return counts.receivedSampleCount;
    }

    /**
     * getActiveSampleCount is a helper method that determines how many BSP samples registered to the product order are
     * in an Active state
     *
     * @return a count of all samples in this product order that are in an ACTIVE state
     */
    public int getActiveSampleCount() {
        counts.generateCounts();
        return counts.activeSampleCount;
    }

    public int getMissingBspMetaDataCount() {
        counts.generateCounts();
        return counts.missingBspMetaDataCount;
    }

    public Long getProductOrderId() {
        return productOrderId;
    }

    /**
     * submitProductOrder encapsulates the set of steps necessary to finalize the submission of a product order.
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
    public void submitProductOrder() throws IOException {
        JiraService jiraService = ServiceAccessUtility.getBean(JiraService.class);
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

        List<CustomField> listOfFields = new ArrayList<CustomField>();

        listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.PRODUCT_FAMILY,
                product.getProductFamily() == null ? "" : product.getProductFamily().getName()));

        listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.PRODUCT,
                product.getProductName() == null ? "" : product.getProductName()));

        if (quoteId != null && !quoteId.isEmpty()) {
            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.QUOTE_ID, quoteId));
        }
        listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.SAMPLE_IDS,
                StringUtils.join(getSampleNames(), '\n')));

        BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);

        JiraIssue issue = jiraService.createIssue(
                fetchJiraProject().getKeyPrefix(), bspUserList.getById(createdBy).getUsername(),
                fetchJiraIssueType(), title, comments == null ? "" : comments, listOfFields);

        jiraTicketKey = issue.getKey();
        issue.addLink(researchProject.getJiraTicketKey());

        issue.addWatcher(bspUserList.getById(createdBy).getUsername());

        issue.addComment(StringUtils.join(getSampleSummaryComments(), "\n"));
        issue.addComment(StringUtils.join(getSampleValidationComments(), "\n"));
    }

    /**
     * This is a helper method encapsulating the validations run against the samples contained
     * within this product order.  The results of these validation checks are then added to the existing Jira Ticket.
     */
    public List<String> getSampleSummaryComments() {
        counts.generateCounts();
        return counts.sampleSummary();
    }

    public List<String> getSampleValidationComments() {
        counts.generateCounts();
        return counts.sampleValidation();
    }

    /**
     * closeProductOrder allows a user to set the Jira ticket associated with this product order into a "Billed" state
     *
     * @throws IOException
     */
    public void closeProductOrder() throws IOException {
        if (StringUtils.isEmpty(jiraTicketKey)) {
            throw new IllegalStateException("A jira Ticket has not been created.");
        }
        JiraService jiraService = ServiceAccessUtility.getBean(JiraService.class);
        JiraIssue issue = jiraService.getIssue(jiraTicketKey);
        IssueTransitionResponse transitions = issue.findAvailableTransitions();

        String transitionId = transitions.getTransitionId(TransitionStates.Complete.getStateName());

        issue.postNewTransition(transitionId);
    }

    /**
     * @return true if all samples are of BSP Format. Note:
     * will return false if there are no samples on the sheet.
     */
    public boolean areAllSampleBSPFormat() {
        counts.generateCounts();
        return counts.bspSampleCount == counts.uniqueSampleCount;
    }

    /**
     * isSheetEmpty validates the existence of samples in the product order
     *
     * @return true if there are no samples currently assigned to this product order
     */
    private boolean isSheetEmpty() {
        return (samples == null) || samples.isEmpty();
    }


    /**
     * This is a helper method that binds a specific Jira project to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType} that
     *         represents the Jira Project for Product Orders
     */
    @Transient
    public CreateFields.ProjectType fetchJiraProject() {
        return CreateFields.ProjectType.Product_Ordering;
    }

    /**
     * This is a helper method that binds a specific Jira Issue Type to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.IssueType} that
     *         represents the Jira Issue Type for Product Orders
     */
    @Transient
    public CreateFields.IssueType fetchJiraIssueType() {
        return CreateFields.IssueType.PRODUCT_ORDER;
    }

    public String getSampleBillingSummary() {
        if (sampleBillingSummary == null) {
            if (samples != null) {
                Map<BillingStatus, Integer> totals = new EnumMap<BillingStatus, Integer>(BillingStatus.class);
                for (ProductOrderSample sample : samples) {
                    Integer total = totals.get(sample.getBillingStatus());
                    if (total == null) {
                        total = 0;
                    }
                    totals.put(sample.getBillingStatus(), ++total);
                }
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<BillingStatus, Integer> entry : totals.entrySet()) {
                    sb.append(MessageFormat.format("{0}: {1} ", entry.getKey().getDisplayName(), entry.getValue()));
                }
                sampleBillingSummary = sb.toString();
            }
        }
        return sampleBillingSummary;
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {
        PRODUCT_FAMILY("Product Family"),
        PRODUCT("Product"),
        QUOTE_ID("Quote ID"),
        MERCURY_URL("Mercury URL"),
        SAMPLE_IDS("Sample IDs");

        private final String fieldName;

        private RequiredSubmissionFields(String fieldNameIn) {
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
        Closed;

        @Override
        public String getDisplayName() {
            return name();
        }
    }

    private enum TransitionStates {
        Complete("Complete"),
        Cancel("Cancel"),
        StartProgress("Start Progress"),
        PutOnHold("Put On Hold");

        private final String stateName;

        private TransitionStates(String stateName) {
            this.stateName = stateName;
        }

        public String getStateName() {
            return stateName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProductOrder that = (ProductOrder) o;

        if (researchProject != null ? !researchProject.equals(that.getResearchProject()) : that.getResearchProject() != null)
            return false;
        if (title != null ? !title.equals(that.getTitle()) : that.getTitle() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (researchProject != null ? researchProject.hashCode() : 0);
        return result;
    }

    public boolean hasJiraTicketKey() {
        return !StringUtils.isBlank(jiraTicketKey);
    }

    public Integer getPdoSampleCount() {
        return pdoSampleCount;
    }
}
