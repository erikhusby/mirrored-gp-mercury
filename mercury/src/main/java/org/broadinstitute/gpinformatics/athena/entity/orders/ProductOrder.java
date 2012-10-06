package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
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
@Audited
@Table(schema = "athena")
public class ProductOrder implements Serializable {

    @Id
    @SequenceGenerator(name="SEQ_PRODUCT_ORDER", schema = "athena", sequenceName="SEQ_PRODUCT_ORDER")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="SEQ_PRODUCT_ORDER")
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
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "productOrder")
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

    public int getUniqueParticipantCount() {
        Set<String> uniqueParticipants = new HashSet<String>();

        if (! isSheetEmpty() ) {
            if ( needsBspMetaData() ) {
                //TODO hmc fetch list of sample meta data from bsp via the fetcher
                throw new IllegalStateException("Not Yet Implemented");
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

    public int getTotalSampleCount() {
        return sampleProducts.size();
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

    private boolean isSheetEmpty() {
        return (sampleProducts == null ) ||  sampleProducts.isEmpty();
    }

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
