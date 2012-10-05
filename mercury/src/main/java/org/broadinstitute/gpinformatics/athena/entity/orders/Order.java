package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

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
public class Order implements Serializable {

    private String title;                       // Unique title for the order
    private String researchProjectName;
    private Product product;
    private OrderStatus orderStatus = OrderStatus.Draft;
    private String quoteId;                     // Alphanumeric Id
    private String comments;                    // Additional comments of the order
    private List<ProductOrderSample> samples;


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
     * @param researchProjectName
     */
    public Order(String title, List<ProductOrderSample> samples, String quoteId, Product product, String researchProjectName) {
        this.title = title;
        this.samples = samples;
        this.quoteId = quoteId;
        this.product = product;
        this.researchProjectName = researchProjectName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResearchProjectName() {
        return researchProjectName;
    }

    public void setResearchProjectName(String researchProjectName) {
        this.researchProjectName = researchProjectName;
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

    public void addSample(ProductOrderSample sample) {
        samples.add(sample);
    }

    public int getUniqueParticipantCount() {
        Set<String> uniqueParticipants = new HashSet<String>();

        if (! isSheetEmpty() ) {
            if ( needsBspMetaData() ) {
                //TODO hmc fetch list of sample meta data from bsp via the fetcher
                throw new IllegalStateException("Not Yet Implemented");
            }

            for ( ProductOrderSample productOrderSample : samples ) {
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
        for ( ProductOrderSample productOrderSample : samples ) {
            String sampleName = productOrderSample.getSampleName();
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
            for ( ProductOrderSample productOrderSample : samples) {
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
        return (samples == null ) ||  samples.isEmpty();
    }

    private boolean needsBspMetaData() {
        boolean needed = false;
        if (! isSheetEmpty() ) {
            for ( ProductOrderSample productOrderSample : samples ) {
                if ( productOrderSample.isInBspFormat() &&
                     ! productOrderSample.hasBSPDTOBeenInitialized() ) {
                    needed = true;
                    break;
                }
            }
        }
        return needed;
    }

}
