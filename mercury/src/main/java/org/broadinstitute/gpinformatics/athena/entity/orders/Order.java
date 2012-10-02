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
@XmlType(namespace = Namespaces.ORDER_NS)
public class Order implements Serializable {

    private String title;                       // Unique title for the order
    private String researchProjectName;
    private String barcode;                     // Unique barcode for the order. Eg. PDO-ABDR
    private OrderStatus orderStatus;
    private String quoteId;                     // Alphanumeric Id
    private String comments;                    // Additional comments of the order
    private SampleSheet sampleSheet;


    public Order() {
    }

    public Order(final String title, final String researchProjectName, final String barcode,
                 final OrderStatus orderStatus, final String quoteId, final String comments,
                 final SampleSheet sampleSheet) {
        this.title = title;
        this.researchProjectName = researchProjectName;
        this.barcode = barcode;
        this.orderStatus = orderStatus;
        this.quoteId = quoteId;
        this.comments = comments;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
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
        //TODO
        //        return 0;
        return sampleSheet.getUniqueParticipantCount();
    }

    public int getUniqueSampleCount() {
        //TODO
        //        return 0;
        return sampleSheet.getUniqueSampleCount();
    }

    public int getTotalSampleCount() {
        //TODO
        //        return 0;
        return sampleSheet.getTotalSampleCount();
    }

    public int getDuplicateCount() {
         //TODO
        //         return 0;
        return sampleSheet.getDuplicateCount();
    }

    public ImmutablePair getDiseaseNormalCounts() {
            //TODO
        //            return 0;
        return sampleSheet.getDiseaseNormalCounts();
    }

    public ImmutablePair getGenderCount() {
         //TODO
        //         return 0;
        return sampleSheet.getGenderCount();
    }

}
