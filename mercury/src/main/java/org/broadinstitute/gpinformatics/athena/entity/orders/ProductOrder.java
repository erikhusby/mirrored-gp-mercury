package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;

import javax.inject.Inject;
import javax.persistence.Transient;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to model the concept of a Product ProductOrder that can be created
 * by the Program PM and subsequently submitted to a lims system.
 * Currently supports the concept associating a product with a set of samples withe a quote.
 * For more detail on the purpose of the ProductOrder, see the user stories listed on
 *
 * @see <a href="	https://confluence.broadinstitute.org/x/kwPGAg</a>
 *      <p/>
 *      Created by IntelliJ IDEA.
 *      User: mccrory
 *      Date: 8/28/12
 *      Time: 10:25 AM
 */
@Entity
public class ProductOrder implements Serializable {

    private static final String JIRA_SUBJECT_PREFIX = "Product order for ";

    @Inject
    @Transient
    private BSPSampleDataFetcher bspFetcher;

    @Inject
    @Transient
    private JiraService jiraService;

    @Id
    @SequenceGenerator(name="PRODUCT_ORDER_INDEX", sequenceName="PRODUCT_ORDER_INDEX", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="PRODUCT_ORDER_INDEX")
    private Long id;

    private Date createdDate;
    private Long createdBy;
    private Date modifiedDate;
    private Long modifiedBy;


    @Column(unique = true)
    private String title;                       // Unique title for the order

    @ManyToOne
    private ResearchProject researchProject;

    @OneToOne
    private Product product;
    private OrderStatus orderStatus = OrderStatus.Draft;
    private String quoteId;                     // Alphanumeric Id
    @Column(length = 2000)
    private String comments;                    // Additional comments of the order
    private String jiraTicketKey;               // Reference to the Jira Ticket created when the order is submitted
    @OneToMany(cascade = CascadeType.PERSIST)
    private List<ProductOrderSample> sampleProducts;


    /**
     * Default no-arg constructor
     */
    ProductOrder() {
    }

    /**
     * Constructor with mandatory fields
     * @param title
     * @param sampleProducts
     * @param quoteId
     * @param product
     * @param researchProject
     */
    public ProductOrder(String title, List<ProductOrderSample> sampleProducts, String quoteId, Product product, ResearchProject researchProject) {
        this.title = title;
        this.sampleProducts = sampleProducts;
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

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProjectName(ResearchProject researchProject) {
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

    public List<ProductOrderSample> getSampleProducts() {
        return sampleProducts;
    }

    public void addSample(ProductOrderSample sampleProduct) {
        sampleProducts.add(sampleProduct);
    }

    /**
     * getJiraTicketKey allows a user of this class to gain access to the Unique key representing the Jira Ticket for
     * which this Product ProductOrder is associated
     *
     * @return a {@link String} that represents the unique Jira Ticket key
     */
    public String getJiraTicketKey() {
        return this.jiraTicketKey;
    }

    /**
     * setJiraTicketKey allows a user of this class to associate the key for the Jira Ticket which was created when the
     * related ProductOrder was officially submitted
     *
     * @param jiraTicketKeyIn a {@link String} that represents the unique key to the Jira Ticket to which the current
     *                        Product Order is associated
     */
    public void setJiraTicketKey(String jiraTicketKeyIn) {
        if(jiraTicketKeyIn == null) {
            throw new NullPointerException("Jira Ticket Key cannot be null");
        }
        this.jiraTicketKey = jiraTicketKeyIn;
    }

    /**
     * getUniqueParticipantCount provides the summation of all unique participants represented in the list of samples
     * registered to this product order
     * @return a count of every participant that is represented by at least one sample in the list of product order
     * samples
     */
    public int getUniqueParticipantCount() {
        Set<String> uniqueParticipants = new HashSet<String>();

        if (! isSheetEmpty() ) {
            if ( needsBspMetaData() ) {

                Map<String, BSPSampleDTO> bspSampleMetaData = bspFetcher.fetchSamplesFromBSP(getUniqueSampleNames());

                //todo SGM Test cases
                updateBspMetaData(bspSampleMetaData);
            }

            for ( ProductOrderSample productOrderSample : sampleProducts) {
                String participantId = productOrderSample.getParticipantId();
                if (StringUtils.isNotBlank(participantId)) {
                    uniqueParticipants.add(participantId);
                }
            }
        }
        return uniqueParticipants.size();
    }

    /**
     * returns the number of unique participants
     * @return
     */
    public int getUniqueSampleCount() {
        int result = 0;
        Set<String> uniqueSamples = getUniqueSampleNames();
        return uniqueSamples.size();
    }

    /**
     * getUniqueSampleNames provides a collection that contains the names of each sample that is represented by at least
     * one sample that is registered to this product order
     *
     * @return a Set of unique Sample name which are represented in the list of samples registered to this product
     * order
     */
    private Set<String> getUniqueSampleNames() {
        Set<String> uniqueSamples = new HashSet<String>();
        for ( ProductOrderSample productOrderSample : sampleProducts) {
            String sampleName = productOrderSample.getSampleName();
            if (StringUtils.isNotBlank(sampleName)) {
                uniqueSamples.add(sampleName);
            }
        }
        return uniqueSamples;
    }

    /**
     * getTotalSampleCount exposes how many samples are registered to this product order
     *
     * @return a count of all samples registered to this product order
     */
    public int getTotalSampleCount() {
        return sampleProducts.size();
    }

    /**
     * getDuplicateCount exposes how many samples registered to this product order are represented by more than one
     * sample in the list
     *
     * @return a count of all samples that have more than one entry in the registered sample list
     */
    public int getDuplicateCount() {
        return ( getTotalSampleCount() - getUniqueSampleCount());
    }

    /**
     * getBspSampleCount exposes how many of the samples, which are registered to this product order, are from BSP
     *
     * @return a count of all product order samples that come from bsp
     */
    public int getBspSampleCount() {
        int count = 0;

        for(ProductOrderSample sample : sampleProducts) {
            if(sample.isInBspFormat()) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     *
     * @return
     */
    public TumorNormalCount getTumorNormalCounts ( ) {

        TumorNormalCount counts =
                new TumorNormalCount(
                        getSampleTypeCount(BSPSampleDTO.TUMOR_IND),
                        getSampleTypeCount(BSPSampleDTO.NORMAL_IND)
                );
        return counts;
    }

    public MaleFemaleCount getMaleFemaleCounts ( ) {

        MaleFemaleCount counts =
                new MaleFemaleCount(
                        getGenderCount(BSPSampleDTO.MALE_IND),
                        getGenderCount(BSPSampleDTO.FEMALE_IND)
                );
        return counts;
    }

    public BspNonBspSampleCount getBspNonBspSampleCounts ( ) {
        BspNonBspSampleCount counts =
                new BspNonBspSampleCount(
                        getBspSampleCount(),
                        getTotalSampleCount() - getBspSampleCount()
                );
        return counts;
    }

    public BilledNotBilledCounts getBilledNotBilledCounts ( ) {
        int billedCount = 0;
        int notBilledCount = 0;



        return new BilledNotBilledCounts(getBillingStatusCount(BillingStatus.Billed),
                                         getBillingStatusCount(BillingStatus.NotYetBilled));
    }

    public int getElligibleForBillingCounts ( ) {
        return getBillingStatusCount(BillingStatus.EligibleForBilling);
    }

    public int getNotBIllableCounts ( ) {
        return getBillingStatusCount(BillingStatus.NotBillable);
    }

    private int getBillingStatusCount (BillingStatus targetStatus) {
        int statusCount = 0;

        for(ProductOrderSample sample:sampleProducts) {
            if(targetStatus.equals ( sample.getBillingStatus ( ) )) {
                statusCount++;
            }
        }

        return statusCount;
    }

    public int getFingerprintCount ( ) {

        int fpCount = 0;

        for ( ProductOrderSample productOrderSample : sampleProducts ) {
            if ( productOrderSample.isInBspFormat () &&
                    productOrderSample.hasFootprint()) {
                fpCount++;
            }
        }
        return fpCount;
    }

    /**
     * getCountsByStockType exposes the summation of each unique stock type found in the list of samples registered to
     * this product order
     *
     * @return a Map, indexed by the unique stock type found, which gives a count of how many samples in the list of
     * product order samples, are related to that stock type
     */
    public Map<String, Integer> getCountsByStockType () {

        Map<String, Integer> stockTypeCounts = new HashMap<String, Integer>();

        for(ProductOrderSample sample : sampleProducts) {
            if(sample.isInBspFormat () ) {
                if(!stockTypeCounts.containsKey(sample.getStockType())) {
                    stockTypeCounts.put(sample.getStockType(), 0);
                }
                stockTypeCounts.put(sample.getStockType(), stockTypeCounts.get(sample.getStockType() + 1));
            }
        }

        return stockTypeCounts;
    }

    /**
     * getPrimaryDiseaseCount exposes the summation of each unique disease found in the list of samples registered to
     * this product order
     *
     * @return a Map, indexed by the unique disease found, which gives a count of how many samples in the list of
     * product order samples, are related to that disease.
     */
    public Map<String, Integer> getPrimaryDiseaseCount() {
        Map<String, Integer> uniqueDiseases = new HashMap<String, Integer>();

        for(ProductOrderSample sample: sampleProducts) {
            if(sample.isInBspFormat ()) {
                if(!uniqueDiseases.containsKey(sample.getDisease())) {
                    uniqueDiseases.put(sample.getDisease(),0);
                }
                uniqueDiseases.put(sample.getDisease(), uniqueDiseases.get(sample.getDisease()) +1);
            }
        }

        return uniqueDiseases;
    }

    /**
     * getGenderCount is a helper method to exposed the sum of all samples, registered to this product order, based on
     * a given gender
     *
     * @param gender A string that represents the gender for which we wish to get a count
     * @return a count of all samples for whom the participant's gener matches the one given
     */
    private int getGenderCount ( String gender ) {

        int counter = 0;
        for (ProductOrderSample sample:sampleProducts) {
            if (sample.isInBspFormat () && gender.equals (sample.getGender ())) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * getSampleTypeCount is a helper method to expose the sum of all samples, registered to this product order,
     * based on a given sample type
     *
     *
     * @param sampleTypeInd a String representing the type of sample for which we wish to get a count
     * @return a count of all samples that have a sample type matching the value passed in.
     */
    private int getSampleTypeCount ( String sampleTypeInd ) {
        int counter = 0;
        for (ProductOrderSample sample:sampleProducts) {
            if (sample.isInBspFormat () && sampleTypeInd.equals (sample.getSampleType ())) {
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
    public int getReceivedSampleCount ( ) {
        int counter = 0;

        for (ProductOrderSample sample:sampleProducts) {
            if (sample.isInBspFormat () && sample.isSampleReceived ()) {
                counter++;
            }
        }

        return counter;
    }

    /**
     * getActiveSampleCount is a helper method that determines how many BSP samples registered to the product order are
     * in an Active state
     *
     * @return a count of all samples in this product order that are in an ACTIVE state
     */
    public int getActiveSampleCount ( ) {
        int counter = 0;
        for (ProductOrderSample sample:sampleProducts) {
            if (sample.isInBspFormat() && sample.isActiveStock()) {
                counter++;
            }
        }

        return counter;
    }

    /**
     * submitProductOrder encapsulates the set of steps necessary to finalize the submission of a product order.
     * This mainly deals with jira ticket creation.  This method will:
     * <ul>
     *     <li>Create a new jira ticket and persist the reference to the ticket key</li>
     *     <li>assign the submitter as a watcher to the ticket</li>
     *     <li>Add a new comment listing all Samples contained within the order</li>
     *     <li>Add any validation comments regarding the Samples contained within the order</li>
     * </ul>
     * @throws IOException
     */
    public void submitProductOrder() throws IOException{

        Map<String, CustomFieldDefinition> submissionFields =
                jiraService.getCustomFields(new CreateIssueRequest.Fields.Project(fetchJiraProject().getKeyPrefix()),
                                            fetchJiraIssueType());

        List<CustomField> listOfFields = new ArrayList<CustomField>();

        listOfFields.add(
                new CustomField(submissionFields.get(RequiredSubmissionFields.PRODUCT_FAMILY.getFieldName()),
                                this.product.getProductFamily()));

        if(quoteId != null && !quoteId.isEmpty()) {
            listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.QUOTE_ID.getFieldName()),
                            this.quoteId));
        }

        CreateIssueResponse issueResponse =
                jiraService.createIssue(fetchJiraProject().getKeyPrefix(), fetchJiraIssueType(), title,
                                                    comments, listOfFields);

        setJiraTicketKey(issueResponse.getKey());

        addPublicComment("Sample List: "+StringUtils.join(getUniqueSampleNames(), ','));

        /**
         * TODO SGM --  When the service to retrieve BSP People is implemented, add current user ID here.
         */
//        addWatcher(createdBy.toString());

        sampleValidationComments();
    }

    /**
     * sampleValidationComments is a helper method encapsulating the validations run against the samples contained
     * within this product order.  The results of these validation checks are then added to the existing Jira Ticket
     *
     * @throws IOException
     */
    public void sampleValidationComments() throws IOException {
        StringBuilder buildValidationCommentsIn = new StringBuilder();
        if(getBspNonBspSampleCounts().getBspSampleCount() == getTotalSampleCount()) {
            buildValidationCommentsIn.append("All Samples are BSP Samples");
            buildValidationCommentsIn.append("\n");
            buildValidationCommentsIn.append(String.format("%i of %i Samples are in RECEIVED state",
                                                         getReceivedSampleCount(), getTotalSampleCount()));
            buildValidationCommentsIn.append("\n");
            buildValidationCommentsIn.append(String.format("%i of %i Samples are Active stock",
                                                         getActiveSampleCount(), getTotalSampleCount()));
        } else if(getBspNonBspSampleCounts().getBspSampleCount() != 0 &&
                getBspNonBspSampleCounts().getNonBspSampleCount() != 0) {
            buildValidationCommentsIn.append(String.format("Of %i Samples, %i are BSP samples and %i are non-BSP",
                                                         getTotalSampleCount(),getBspNonBspSampleCounts().getBspSampleCount(),
                                                         getBspNonBspSampleCounts().getNonBspSampleCount()));
        } else {
            buildValidationCommentsIn.append("None of the samples come from BSP") ;
        }

        addPublicComment(buildValidationCommentsIn.toString());
    }

    public void addPublicComment(String comment) throws IOException{
        jiraService.addComment(this.jiraTicketKey, comment);
    }

    public void addWatcher(String personLoginId) throws IOException {
        jiraService.addWatcher(this.jiraTicketKey, personLoginId);
    }

    /**
     * Returns true if any and all samples are of BSP Format.
     * Note will return false if there are no samples on the sheet.
     * @return
     */
    public boolean areAllSampleBSPFormat() {
        boolean result = true;
        if (! isSheetEmpty() ) {
            for ( ProductOrderSample productOrderSample : sampleProducts) {
                if (! productOrderSample.isInBspFormat() ) {
                    result = false;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * isSheetEmpty validates the existence of samples in the product order
     * @return true if there are no samples currently assigned to this product order
     */
    private boolean isSheetEmpty() {
        return (sampleProducts == null ) ||  sampleProducts.isEmpty();
    }

    /**
     * needsBspMetaData validates the State of all samples registered to this project order.
     * @return true in the case that at least one sample in the product order list is deemed a BSP sample and does not
     * have the necessary BSP meta data associated with it.
     */
    private boolean needsBspMetaData() {
        boolean needed = false;
        if (! isSheetEmpty() ) {
            for ( ProductOrderSample productOrderSample : sampleProducts) {
                if ( productOrderSample.isInBspFormat() &&
                     ! productOrderSample.hasBSPDTOBeenInitialized() ) {
                    needed = true;
                    break;
                }
            }
        }
        return needed;
    }

    /**
     * updateBspMetaData is a helper method that will update the BSP eta data of all samples registered to this product
     * order that are represented in the given Map
     * @param derivedMetaData a map of BSP metadata indexed by the sample name
     */
    private void updateBspMetaData(Map<String, BSPSampleDTO> derivedMetaData) {
        for(ProductOrderSample sample:getSampleProducts()) {
            if(derivedMetaData.containsKey(sample.getSampleName())) {
                sample.setBspDTO(derivedMetaData.get(sample.getSampleName()));
            }
        }
    }



    /**
     * fetchJiraProject is a helper method that binds a specific Jira project to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     * {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.ProjectType} that
     * represents the Jira Project for Product Orders
     */
    @Transient
    public CreateIssueRequest.Fields.ProjectType fetchJiraProject() {
        return CreateIssueRequest.Fields.ProjectType.Product_Ordering;
    }

    /**
     *
     * fetchJiraIssueType is a helper method that binds a specific Jira Issue Type to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     * {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.Issuetype} that
     * represents the Jira Issue Type for Product Orders
     */
    @Transient
    public CreateIssueRequest.Fields.Issuetype fetchJiraIssueType() {
        return CreateIssueRequest.Fields.Issuetype.Product_Order;
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     */
    public enum RequiredSubmissionFields {

        PRODUCT_FAMILY("Product Family"),
        QUOTE_ID("Quote ID");

        private String fieldName;

        private RequiredSubmissionFields(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    /**
     * Created by IntelliJ IDEA.
     * User: mccrory
     * Date: 9/26/12
     * Time: 11:56 AM
     */
    public static enum OrderStatus {
        Draft,
        Submitted,
        Closed
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductOrder)) return false;

        final ProductOrder productOrder = (ProductOrder) o;

        if (!researchProject.equals(productOrder.researchProject)) return false;
        if (!title.equals(productOrder.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + researchProject.hashCode();
        return result;
    }
}
