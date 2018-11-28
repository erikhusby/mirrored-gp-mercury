package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ReceiveSamplesEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.List;

/**
 * Action bean for receiving samples. This can be done either via a rackscan or via a list of sample ids.
 */
@UrlBinding(ReceiveSamplesActionBean.URL)
public class ReceiveSamplesActionBean extends RackScanActionBean {

    public static final String URL = "/sample/receiveSamples.action";
    public static final String SHOW_RECEIPT_JSP = "/sample/sample_receipt.jsp";
    public static final String RACKSCAN_RECEIPT_JSP = "/samples/sample_receipt_rackscan_results.jsp";
    public static final String SHOW_RECEIPT_ACTION = "showRecept";
    public static final String RECEIVE_SAMPLES_EVENT = "receiveSamplesById";
    public static final String PAGE_TITLE = "Receive Samples";

    @Inject
    private ReceiveSamplesEjb receiveSamplesEjb;

    /** List of sample ids from the page to pull out and return. */
    private String sampleIds;

    /** List of sample ids to receive. This is algorithmically filled in. */
    private List<String> samplesToReceive;

    /** Response from Bsp for the sample kit receipt. */
    private SampleKitReceiptResponse response;

    /**
     * Shows the receipt page.
     */
    @DefaultHandler
    @HandlesEvent(SHOW_RECEIPT_ACTION)
    public Resolution showReceipt() {
        return new ForwardResolution(SHOW_RECEIPT_JSP);
    }

    /**
     * Utilizes a rack scanner to preform the receipt and find out which samples are being received.
     * @throws ScannerException
     */
    @Override
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {
        // Run the rack scanner, ignore the returned resolution.
        super.scan();

        // Prep the Ids to receive.
        samplesToReceive = rackScannerEjb.obtainSampleIdsFromRackscan(rackScan);

        try {
            return receiveSamples(RACKSCAN_RECEIPT_JSP);
        } catch (JAXBException e) {
            throw new ScannerException(e);
        }
    }

    @Override
    public String getRackScanPageUrl() {
        return URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    /**
     * Receives sample by the sample Ids passed in.
     */
    @HandlesEvent(RECEIVE_SAMPLES_EVENT)
    public Resolution receiveSamplesById() throws JAXBException {

        samplesToReceive = SearchActionBean.cleanInputStringForSamples(sampleIds);

        Resolution resolution = receiveSamples(SHOW_RECEIPT_JSP);

        if (!hasErrors()) {
            addMessage("{0} sample(s) have been received.", response.getResult().size());
        }

        return resolution;
    }

    /**
     * Actually performs the receipt based on the sample ids passed in and pushed into the samplesToReceive variable.
     *
     * @param receiptJsp JSP to pass the resolution to.
     */
    private Resolution receiveSamples(String receiptJsp) throws JAXBException {
        MessageCollection messageCollection = new MessageCollection();

        response = receiveSamplesEjb.receiveSamples(new HashMap<>(), samplesToReceive,
                getUserBean().getBspUser(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);

        return new ForwardResolution(receiptJsp);
    }

    public String getReceiveSamplesEvent() {
        return RECEIVE_SAMPLES_EVENT;
    }

    public String getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(String sampleIds) {
        this.sampleIds = sampleIds;
    }

    public SampleKitReceiptResponse getResponse() {
        return response;
    }

    public void setResponse(SampleKitReceiptResponse response) {
        this.response = response;
    }
}
