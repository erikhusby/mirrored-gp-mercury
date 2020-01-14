package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Handles accessioning of samples in a Mayo rack.
 */
@UrlBinding(MayoSampleReceiptActionBean.ACTION_BEAN_URL)
public class MayoSampleReceiptActionBean extends RackScanActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/mayo_sample_receipt.action";
    // Events from Sample Receipt page 1
    private static final String PAGE1 = "mayo_sample_receipt1.jsp";
    private static final String SCAN_BTN = "scanBtn";
    // Events from Sample Receipt page 2
    private static final String PAGE2 = "mayo_sample_receipt2.jsp";
    private static final String SAVE_BTN = "saveBtn";

    private MessageCollection messageCollection = new MessageCollection();
    private List<String> rackScanEntries = new ArrayList<>();

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Validate(required = true, on = {SCAN_BTN, SAVE_BTN})
    private String rackBarcode;

    @DefaultHandler
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(SCAN_BTN)
    public Resolution scanEvent() throws ScannerException {
        // Performs the rack scan and validates it for accessioning.
        runRackScan(false);
        if (rackScan == null || rackScan.isEmpty()) {
            messageCollection.addError("The rack scan is empty.");
        } else {
            mayoManifestEjb.validateForAccessioning(this);
        }
        addMessages(messageCollection);
        return new ForwardResolution(messageCollection.hasErrors() ? PAGE1 : PAGE2);
    }

    /**
     * Makes receipt artifacts for the rack and tubes and accessions
     * samples if a ManifestSession is found and the data matches up.
     */
    @HandlesEvent(SAVE_BTN)
    public Resolution accessionEvent() {
        reconstructScan();
        mayoManifestEjb.accession(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return "Mayo Sample Receipt";
    }

    /** Reconstructs the rackScan from the jsp's hidden variables. */
    private void reconstructScan() {
        rackScan = new LinkedHashMap<>();
        for (String rackScanEntry : rackScanEntries) {
            String[] tokens = rackScanEntry.split(" ");
            String value = (tokens.length > 1) ? tokens[1] : "";
            rackScan.put(tokens[0], value);
        }
    }

    /** Return an ordered list of the rack column names. */
    public List<String> getRackColumns() {
        return Arrays.asList(MayoManifestEjb.DEFAULT_RACK_TYPE.getVesselGeometry().getColumnNames());
	}

    /** Return an ordered list of the rack row names. */
	public List<String> getRackRows() {
        return Arrays.asList(MayoManifestEjb.DEFAULT_RACK_TYPE.getVesselGeometry().getRowNames());
	}

    /** Returns the SampleName or empty string for the given position name. */
	public String getSampleAt(String rowName, String columnName) {
        return StringUtils.trimToEmpty(rackScan.get(rowName + columnName));
    }

    public List<String> getRackScanEntries() {
        return rackScanEntries;
    }

    public void setRackScanEntries(List<String> rackScanEntries) {
        this.rackScanEntries = rackScanEntries;
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public void setMessageCollection(MessageCollection messageCollection) {
        this.messageCollection = messageCollection;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

}
