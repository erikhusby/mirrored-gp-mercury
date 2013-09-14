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
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UrlBinding(ReceiveSamplesActionBean.URL)
public class ReceiveSamplesActionBean extends RackScanActionBean {

    public static final String URL = "/sample/receiveSamples.action";
    public static final String SHOW_RECEIPT_JSP = "/sample/sample_receipt.jsp";
    public static final String RACKSCAN_RECEIPT_JSP = "/samples/sample_receipt_rackscan_results.jsp";
    public static final String SAMPLE_ID_RECEIPT_JSP = "/samples/sample_receipt_results.jsp";
    public static final String SHOW_RECEIPT_ACTION = "showRecept";
    public static final String RECEIVE_SAMPLES_EVENT = "receiveSamplesById";

    @Inject
    private ReceiveSamplesEjb receiveSamplesEjb;

    private List<String> samplesToReceive;
    private SampleKitReceiptResponse response;

    @DefaultHandler
    @HandlesEvent(SHOW_RECEIPT_ACTION)
    public Resolution showReceipt() {
        return new ForwardResolution(SHOW_RECEIPT_JSP);
    }

    @Override
    @HandlesEvent(SCAN_EVENT)
    public Resolution scan() throws ScannerException {
        // Run the rack scanner, ignore the returned resolution.
        super.scan();

        // Prep the Ids to receive.
        samplesToReceive = new ArrayList<>(rackScan.values());

        return receiveSamples(RACKSCAN_RECEIPT_JSP);
    }

    @HandlesEvent(RECEIVE_SAMPLES_EVENT)
    public Resolution receiveSamplesById() {
        return receiveSamples(SAMPLE_ID_RECEIPT_JSP);
    }

    private Resolution receiveSamples(String receiptJsp) {
        MessageCollection messageCollection = new MessageCollection();

        response = receiveSamplesEjb.receiveSamples(samplesToReceive,
                        getUserBean().getBspUser().getUsername(), messageCollection);

        for (String error : response.getMessages()) {
            addGlobalValidationError(error);
        }

        addMessages(messageCollection);

        return new ForwardResolution(receiptJsp);
    }

    public List<String> getSamplesToReceive() {
        return samplesToReceive;
    }

    public void setSamplesToReceive(List<String> samplesToReceive) {
        this.samplesToReceive = samplesToReceive;
    }

    public SampleKitReceiptResponse getResponse() {
        return response;
    }

    public void setResponse(SampleKitReceiptResponse response) {
        this.response = response;
    }
}
