package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to model the concept of a Product Order that can be created
 * by the Program PM and subsequently submitted to a lims system.
 * Currently supports the concept associating a product with a set of samples withe a quote.
 * For more detail on the purpose of the Order, see the user stories listed on
 *
 * @see <a href="	https://confluence.broadinstitute.org/x/kwPGAg</a>
 *      <p/>
 *      Created by IntelliJ IDEA.
 *      User: mccrory
 *      Date: 8/28/12
 *      Time: 10:25 AM
 */
@Entity
public class Order implements Serializable {

    @Id
    @SequenceGenerator(name="PRODUCT_ORDER_INDEX", sequenceName="PRODUCT_ORDER_INDEX", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="PRODUCT_ORDER_INDEX")
    private Long id;

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
    private List<OrderSample> samples;


    /**
     * Default no-arg constructor
     */
    Order() {
    }

    /**
     * Constructor with mandatory fields
     * @param title
     * @param samples
     * @param quoteId
     * @param product
     * @param researchProject
     */
    public Order(String title, List<OrderSample> samples, String quoteId, Product product, ResearchProject researchProject) {
        this.title = title;
        this.samples = samples;
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

    public List<OrderSample> getSamples() {
        return samples;
    }

    public void addSample(OrderSample sample) {
        samples.add(sample);
    }

    /**
     * getJiraTicketKey allows a user of this class to gain access to the Unique key representing the Jira Ticket for
     * which this Product Order is associated
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

    public int getUniqueParticipantCount() {
        Set<String> uniqueParticipants = new HashSet<String>();

        if (! isSheetEmpty() ) {
            if ( needsBspMetaData() ) {
                //TODO hmc fetch list of sample meta data from bsp via the fetcher
                throw new IllegalStateException("Not Yet Implemented");
            }

            for ( OrderSample orderSample : samples ) {
                String participantId = orderSample.getParticipantId();
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

    private Set<String> getUniqueSampleNames() {
        Set<String> uniqueSamples = new HashSet<String>();
        for ( OrderSample orderSample : samples ) {
            String sampleName = orderSample.getSampleName();
            if (StringUtils.isNotBlank(sampleName)) {
                uniqueSamples.add(sampleName);
            }
        }
        return uniqueSamples;
    }

    public int getTotalSampleCount() {
        return samples.size();
    }

    public int getDuplicateCount() {
        return ( getTotalSampleCount() - getUniqueSampleCount());
    }

    public TumorNormalCount getTumorNormalCounts() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
    }

    public MaleFemaleCount getMaleFemaleCounts() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
    }

    /**
     * Returns true is any and all samples are of BSP Format.
     * Note will return false if there are no samples on the sheet.
     * @return
     */
    public boolean areAllSampleBSPFormat() {
        boolean result = true;
        if (! isSheetEmpty() ) {
            for ( OrderSample orderSample : samples) {
                if (! orderSample.isInBspFormat() ) {
                    result = false;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    private boolean isSheetEmpty() {
        return (samples == null ) ||  samples.isEmpty();
    }

    private boolean needsBspMetaData() {
        boolean needed = false;
        if (! isSheetEmpty() ) {
            for ( OrderSample orderSample : samples ) {
                if ( orderSample.isInBspFormat() &&
                     ! orderSample.hasBSPDTOBeenInitialized() ) {
                    needed = true;
                    break;
                }
            }
        }
        return needed;
    }

    /**
     * fetchJiraProject is a helper method that binds a specific Jira project to an Order entity.  This
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
     * fetchJiraIssueType is a helper method that binds a specific Jira Issue Type to an Order entity.  This
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

        PRODUCT_FAMILY("Product Family");

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
        if (!(o instanceof Order)) return false;

        final Order order = (Order) o;

        if (!researchProject.equals(order.researchProject)) return false;
        if (!title.equals(order.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + researchProject.hashCode();
        return result;
    }
}
