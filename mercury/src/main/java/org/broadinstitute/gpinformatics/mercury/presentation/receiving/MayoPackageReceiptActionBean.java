package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor;
import org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure.QuarantinedDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles receipt of a Mayo package. Also handles the Mayo Manifest Admin functionality.
 */
@UrlBinding(MayoPackageReceiptActionBean.ACTION_BEAN_URL)
public class MayoPackageReceiptActionBean extends CoreActionBean {
    public static final String ACTION_BEAN_URL = "/receiving/mayo_package_receipt.action";
    private static final String PAGE1 = "mayo_package_receipt1.jsp";
    private static final String PAGE2 = "mayo_package_receipt2.jsp";
    private static final String QUARANTINED_PAGE = "mayo_quarantines.jsp";
    private static final String CONTINUE_BTN = "continueBtn";
    private static final String LINK_PACKAGE_BTN = "linkPkgBtn";
    private static final String UPDATE_METADATA_BTN = "updateMetadataBtn";
    private static final String SAVE_BTN = "saveBtn";
    private static final String CANCEL_BTN = "cancelBtn";
    private static final String VIEW_QUARANTINES = "viewQuarantines";

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Inject
    private QuarantinedDao quarantinedDao;

    private MessageCollection messageCollection = new MessageCollection();
    private List<List<String>> manifestCellGrid = new ArrayList<>();

    @Validate(required = true, on = {CONTINUE_BTN, LINK_PACKAGE_BTN, SAVE_BTN})
    private String packageBarcode;

    @Validate(required = true, on = {LINK_PACKAGE_BTN, UPDATE_METADATA_BTN})
    private String filename;

    @Validate(required = true, on = {SAVE_BTN})
    private String shipmentCondition;

    @Validate(required = true, on = {SAVE_BTN})
    private String deliveryMethod;

    @Validate(required = true, on = {SAVE_BTN})
    private String trackingNumber;

    @Validate(required = true, on = {CONTINUE_BTN, SAVE_BTN})
    private String rackBarcodeString;

    @Validate(required = true, on = {CONTINUE_BTN})
    private String rackCount;

    private Long manifestSessionId;
    private String rctUrl;
    private List<String> rackBarcodes = Collections.emptyList();
    private List<String> quarantineBarcodes = new ArrayList<>();
    private List<String> quarantineReasons = new ArrayList<>();
    private List<Quarantined> quarantined = new ArrayList<>();

    @DefaultHandler
    @HandlesEvent(CANCEL_BTN)
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(CONTINUE_BTN)
    public Resolution receiptContinue() {
        // Parses, then rewrites the barcode string to normalize the whitespace.
        parseBarcodeString();
        rackBarcodeString = StringUtils.join(rackBarcodes, "\n");
        if (StringUtils.isBlank(rackBarcodeString)) {
            addValidationError(rackBarcodeString, "One or more rack barcodes are required.");
        } else if (CollectionUtils.isEmpty(rackBarcodes)) {
            addValidationError(rackBarcodeString, "Cannot parse rack barcode(s)");
        } else if (StringUtils.isBlank(rackCount) || !NumberUtils.isDigits(rackCount)) {
            addValidationError(rackBarcodeString, "Number of racks is required");
        } else if (rackBarcodes.size() != Integer.parseInt(rackCount)) {
            addValidationError(rackBarcodeString, "Number of rack barcodes does not match the number of racks.");
        }
        // Errors any duplicate rack barcodes.
        Map<String, Long> countingMap = rackBarcodes.stream().
                collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        String duplicates = countingMap.entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() > 1).
                map(Map.Entry::getKey).
                collect(Collectors.joining(", "));
        if (!duplicates.isEmpty()) {
            addValidationError(rackBarcodeString, "Found duplicate rack barcodes: " + duplicates);
        }
        if (!getValidationErrors().isEmpty()) {
            return new ForwardResolution(PAGE1);
        }

        // Looks up a manifest for the package id and does validation.
        boolean canContinue = mayoManifestEjb.packageReceiptLookup(this);
        addMessages(messageCollection);
        return canContinue ? new ForwardResolution(PAGE2) : new ForwardResolution(PAGE1);
    }

    @HandlesEvent(LINK_PACKAGE_BTN)
    public Resolution linkPackageBtn() {
        if (StringUtils.isBlank(filename)) {
            addValidationError(filename, "Manifest filename is required.");
            return new ForwardResolution(PAGE1);
        }
        // Updates the ManifestSession using the specified manifest file and does validation.
        mayoManifestEjb.linkPackageToManifest(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(UPDATE_METADATA_BTN)
    public Resolution updateMetadataBtn() {
        if (StringUtils.isBlank(filename)) {
            addValidationError(filename, "Manifest filename is required.");
            return new ForwardResolution(PAGE1);
        }
        mayoManifestEjb.updateMetadata(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(SAVE_BTN)
    public Resolution saveEvent() {
        parseBarcodeString();
        mayoManifestEjb.packageReceipt(this);
        addMessages(messageCollection);
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(VIEW_QUARANTINES)
    public Resolution showQuarantined() {
        quarantined.addAll(quarantinedDao.findItems(Quarantined.ItemSource.MAYO));
        quarantined.sort((q1, q2) -> {
            int comparison = q1.getItemType().compareTo(q2.getItemType());
            return (comparison == 0) ?  q1.getQuarantinedId().compareTo(q2.getQuarantinedId()) : comparison;
        });
        return new ForwardResolution(QUARANTINED_PAGE);
    }

    /** Parses the rack barcode entry string into individual rack barcodes. */
    public void parseBarcodeString() {
        rackBarcodes = Arrays.asList(StringUtils.split(
                MayoManifestImportProcessor.cleanupValue(rackBarcodeString, " ")));
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

    public String getRctUrl() {
        return rctUrl;
    }

    public void setRctUrl(String rctUrl) {
        this.rctUrl = rctUrl;
    }

    public List<String> getRackBarcodes() {
        return rackBarcodes;
    }

    public void setRackBarcodes(List<String> rackBarcodes) {
        this.rackBarcodes = rackBarcodes;
    }

    public List<String> getQuarantineBarcodes() {
        return quarantineBarcodes;
    }

    public void setQuarantineBarcodes(List<String> quarantineBarcodes) {
        this.quarantineBarcodes = quarantineBarcodes;
    }

    public List<String> getQuarantineReasons() {
        return quarantineReasons;
    }

    public void setQuarantineReasons(List<String> quarantineReasons) {
        this.quarantineReasons = quarantineReasons;
    }

    public List<Quarantined> getQuarantined() {
        return quarantined;
    }

    public void setQuarantined(List<Quarantined> quarantined) {
        this.quarantined = quarantined;
    }

    public List<String> getRackReasons() {
        return Quarantined.getRackReasons();
    }
}
