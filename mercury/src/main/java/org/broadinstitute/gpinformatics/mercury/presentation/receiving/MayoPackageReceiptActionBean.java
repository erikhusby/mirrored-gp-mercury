package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles receipt of a Mayo package.
 */
@UrlBinding(MayoPackageReceiptActionBean.ACTION_BEAN_URL)
public class MayoPackageReceiptActionBean extends CoreActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/mayo_package_receipt.action";
    private static final String PAGE1 = "mayo_package_receipt1.jsp";
    private static final String PAGE2 = "mayo_package_receipt2.jsp";
    private static final String START_PACAKGE_BTN = "startPackageBtn";
    private static final String SAVE_BTN = "saveBtn";
    private static final String CANCEL_BTN = "cancelBtn";

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    private MessageCollection messageCollection = new MessageCollection();
    private List<List<String>> manifestCellGrid = new ArrayList<>();

    @Validate(required = true, on = {START_PACAKGE_BTN, SAVE_BTN})
    private String packageBarcode;

    @Validate(required = true, on = {START_PACAKGE_BTN, SAVE_BTN})
    private String rackCount;

    @Validate(required = true, on = {START_PACAKGE_BTN, SAVE_BTN})
    private String rackBarcodeString;

    @Validate(required = true, on = {SAVE_BTN})
    private String shipmentCondition;

    @Validate(required = true, on = {SAVE_BTN})
    private String deliveryMethod;

    @Validate(required = true, on = {SAVE_BTN})
    private String trackingNumber;

    @Validate(required = true, on = {SAVE_BTN})
    private Long manifestSessionId;

    @Validate(required = true, on = {SAVE_BTN})
    private String filename;

    private String quarantineReason;
    private String rctUrl;

    @DefaultHandler
    @HandlesEvent(CANCEL_BTN)
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(START_PACAKGE_BTN)
    public Resolution startPackageEvent() {
        mayoManifestEjb.lookupManifestSession(this);
        // Generates a cell grid for the contents of the manifest file.
        if (StringUtils.isNotBlank(filename)) {
            manifestCellGrid = mayoManifestEjb.readManifestFileCellGrid(filename, messageCollection);
        }
        addMessages(messageCollection);
        return new ForwardResolution(PAGE2);
    }

    @HandlesEvent(SAVE_BTN)
    public Resolution saveEvent() {
        mayoManifestEjb.receive(this);
        addMessages(messageCollection);
        if (!messageCollection.hasErrors() && StringUtils.isNotBlank(rctUrl)) {
            return new ForwardResolution(rctUrl);
        }
        return new ForwardResolution(PAGE2);
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public void setMessageCollection(MessageCollection messageCollection) {
        this.messageCollection = messageCollection;
    }

    public List<List<String>> getManifestCellGrid() {
        return manifestCellGrid;
    }

    public void setManifestCellGrid(List<List<String>> manifestCellGrid) {
        this.manifestCellGrid = manifestCellGrid;
    }

    public Long getManifestSessionId() {
        return manifestSessionId;
    }

    public void setManifestSessionId(Long manifestSessionId) {
        this.manifestSessionId = manifestSessionId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPackageBarcode() {
        return packageBarcode;
    }

    public void setPackageBarcode(String packageBarcode) {
        this.packageBarcode = packageBarcode;
    }

    public String getRackCount() {
        return rackCount;
    }

    public void setRackCount(String rackCount) {
        this.rackCount = rackCount;
    }

    public String getRackBarcodeString() {
        return rackBarcodeString;
    }

    public void setRackBarcodeString(String rackBarcodeString) {
        this.rackBarcodeString = rackBarcodeString;
    }

    public String getShipmentCondition() {
        return shipmentCondition;
    }

    public void setShipmentCondition(String shipmentCondition) {
        this.shipmentCondition = shipmentCondition;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getQuarantineReason() {
        return quarantineReason;
    }

    public void setQuarantineReason(String quarantineReason) {
        this.quarantineReason = quarantineReason;
    }

    public String getRctUrl() {
        return rctUrl;
    }

    public void setRctUrl(String rctUrl) {
        this.rctUrl = rctUrl;
    }
}
