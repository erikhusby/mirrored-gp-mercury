package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ColorCovidManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;

/**
 * Handles receipt of a Color Genomics rack of tubes.
 */
@UrlBinding(ColorCovidReceiptActionBean.ACTION_BEAN_URL)
public class ColorCovidReceiptActionBean extends RackScanActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/color_covid_receipt.action";
    private static final String PAGE1 = "color_covid_sample_receipt1.jsp";
    private static final String SCAN_BTN = "scanBtn";
    private static final String SAVE_BTN = "saveBtn";
    private static final String CANCEL_BTN = "cancelBtn";

    @Inject
    private ColorCovidManifestEjb manifestEjb;

    @Validate(required = true, on = {SCAN_BTN, SAVE_BTN})
    private String rackBarcode;

    private MessageCollection messageCollection = new MessageCollection();
    private String dtoString;
    private String filename;
    private String manifestContent;
    private boolean clearFields = false;

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return "Color Covid Sample Receipt";
    }

    @DefaultHandler
    @HandlesEvent(CANCEL_BTN)
    public Resolution page1() {
        clearFields = true;
        rackBarcode = null;
        dtoString = null;
        messageCollection.clearAll();
        return new ForwardResolution(PAGE1);
    }

    /** Performs the rack scan and validates it against the manifest. */
    @HandlesEvent(SCAN_BTN)
    public Resolution scanEvent() {
        try {
            runRackScan(false);
            if (rackScan == null || rackScan.isEmpty()) {
                messageCollection.addError("The rack scan is empty.");
            } else {
                // If data validates, a string containing all dtos is written to this bean.
                manifestEjb.validate(this);
            }
        } catch (ScannerException e) {
            messageCollection.addError(e);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    /** Does the sample receipt using the manifest dtos. */
    @HandlesEvent(SAVE_BTN)
    public Resolution saveEvent() {
        manifestEjb.accession(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

    /** Indicates whether the UI should clear the text fields. */
    public boolean getClearFields() {
        return clearFields;
    }

    public String getDtoString() {
        return dtoString;
    }

    public void setDtoString(String dtoString) {
        this.dtoString = dtoString;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getManifestContent() {
        return manifestContent;
    }

    public void setManifestContent(String manifestContent) {
        this.manifestContent = manifestContent;
    }
}
