package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

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
    private OrderStatus orderStatus;
    private String quoteId;                     // Alphanumeric Id
    private String comments;                    // Additional comments of the order
    private SampleSheet sampleSheet;

    public Order(final String title, final String researchProjectName, final String quoteId ) {
        this(title, researchProjectName, quoteId, new SampleSheet());
    }

    public Order(final String title, final String researchProjectName, final String quoteId,
                 final SampleSheet sampleSheet ) {
        this.title = title;
        this.researchProjectName = researchProjectName;
        this.orderStatus = OrderStatus.Draft;
        this.quoteId = quoteId;
        this.comments = "";
        this.sampleSheet = sampleSheet;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getResearchProjectName() {
        return researchProjectName;
    }

    public void setResearchProjectName(final String researchProjectName) {
        this.researchProjectName = researchProjectName;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(final String quoteId) {
        this.quoteId = quoteId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(final OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }

    public SampleSheet getSampleSheet() {
        return sampleSheet;
    }

    public void setSampleSheet(final SampleSheet sampleSheet) {
        this.sampleSheet = sampleSheet;
    }

    public int getUniqueParticipantCount() {
        return sampleSheet.getUniqueParticipantCount();
    }

    public int getUniqueSampleCount() {
        return sampleSheet.getUniqueSampleCount();
    }

    public int getTotalSampleCount() {
        return sampleSheet.getTotalSampleCount();
    }

    public int getDuplicateCount() {
        return sampleSheet.getDuplicateCount();
    }

    public ImmutablePair getTumorNormalCounts() {
        return sampleSheet.getTumorNormalCounts();
    }

    public ImmutablePair getMaleFemaleCount() {
        return sampleSheet.getMaleFemaleCount();
    }

}
