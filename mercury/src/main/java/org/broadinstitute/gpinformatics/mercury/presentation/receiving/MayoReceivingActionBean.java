package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestImportProcessor;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles receipt of Mayo samples. There are two receivables from Mayo - a manifest file that describes the
 * samples, and a rack or box of tubes. Mayo puts the manifest file in a Google storage bucket where it can be
 * read by Mercury. At a different time the rack of tubes arrives and a lab tech scans its barcodes in using
 * the Mercury UI.
 *
 * This class queries the lab tech for the package barcode and rack barcode, takes a rack scan of tube barcodes,
 * then looks up the manifest file using the package barcode. Mercury makes vessels, samples, a manifest session,
 * and an RCT jira ticket. If there is no manifest found the vessels and RCT ticket are still made, but not the
 * samples since they require data from the manifest. The lab tech can redo the rack scan after the manifest is
 * present in Google bucket, and this class will then create samples and manifest session and update the tubes.
 *
 * For user friendly do-overs, a rack can be rescanned provided the Overwrite checkbox is selected, and this
 * will update the existing rack to match the new scan. When this is done the manifest is re-read from Google
 * storage, and if found to have different sample metadata content, a new manifest session is created, and the
 * old sample metadata is replaced with the new. The existing RCT ticket gets updated too.
 */
@UrlBinding(MayoReceivingActionBean.ACTION_BEAN_URL)
public class MayoReceivingActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(MayoReceivingActionBean.class);
    public static final String ACTION_BEAN_URL = "/receiving/mayo_receiving.action";
    public static final String PAGE1 = "mayo_sample_receipt1.jsp";
    public static final String PAGE2 = "mayo_sample_receipt2.jsp";
    public static final String PAGE3 = "mayo_manifest_display.jsp";
    public static final String SHOW_MANIFEST_EVENT = "showManifestBtn";
    public static final String SCAN_EVENT = "scanBtn";
    public static final String SAVE_EVENT = "saveBtn";

    private MessageCollection messageCollection = new MessageCollection();
    private boolean canOverwrite = false;
    private boolean overwriteFlag = false;
    private VesselGeometry vesselGeometry = null;
    private List<String> rackScanEntries = new ArrayList<>();
    private String filename;
    private List<String> manifestSheetnames;
    private Map<String, String[][]> manifestArray = new HashMap<>();

    @Inject
    private MayoManifestEjb mayoManifestEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Validate(required = true, on = {SCAN_EVENT, SHOW_MANIFEST_EVENT, SAVE_EVENT})
    private String packageBarcode;

    @Validate(required = true, on = {SCAN_EVENT, SAVE_EVENT})
    private String rackBarcode;

    @DefaultHandler
    public Resolution page1() {
        return new ForwardResolution(PAGE1);
    }

    @HandlesEvent(SCAN_EVENT)
    public Resolution scanEvent() throws ScannerException {
        runRackScan( false );
        if (rackScan == null || rackScan.isEmpty()) {
            messageCollection.addError("The rack scan is empty.");
        } else {
            String invalidPositions = rackScan.keySet().stream().
                    filter(position -> VesselPosition.getByName(position) == null).
                    collect(Collectors.joining(", "));
            if (StringUtils.isNotBlank(invalidPositions)) {
                messageCollection.addError("Rack scan has unknown position: " + invalidPositions);
            } else {
                // Turns the rack scan into a list of position & sample to pass to and from jsp.
                rackScanEntries = rackScan.entrySet().stream().
                        map(mapEntry -> StringUtils.join(mapEntry.getKey(), " ", mapEntry.getValue())).
                        sorted().collect(Collectors.toList());
                // Figures out the minimal rack that will accommodate all vessel positions.
                RackOfTubes.RackType rackType = mayoManifestEjb.inferRackType(rackScan.keySet());
                if (rackType != null) {
                    vesselGeometry = rackType.getVesselGeometry();
                } else {
                    messageCollection.addError("Cannot find a rack type for scanned positions " +
                            rackScan.keySet().stream().sorted().collect(Collectors.joining(", ")));
                }
            }

            // Finds an existing rack that matches the rack barcode and checks if it is a Mayo rack,
            // which will have a linked RCT- ticket.
            LabVessel existingRack = labVesselDao.findByIdentifier(rackBarcode);
            if (existingRack != null) {
                if (OrmUtil.proxySafeIsInstance(existingRack, RackOfTubes.class)) {
                    if (OrmUtil.proxySafeCast(existingRack, RackOfTubes.class).getJiraTickets().stream().
                            filter(ticket -> ticket.getTicketName().
                                    startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                            findFirst().isPresent()) {
                        canOverwrite = true;
                    } else {
                        messageCollection.addError("Another rack having barcode " + rackBarcode +
                                " already exists but it isn't an uploaded rack.");
                    }
                } else {
                    messageCollection.addError("Another vessel having barcode " + rackBarcode +
                            " already exists but is not a rack of tubes.");
                }
            }
        }
        addMessages(messageCollection);
        return new ForwardResolution(messageCollection.hasErrors() ? PAGE1 : PAGE2);
    }

    /**
     * Retrieves and displays the manifest cell data from Google Storage file.
     */
    @HandlesEvent(SHOW_MANIFEST_EVENT)
    public Resolution showManifest() {
        Map<String, List<List<String>>> manifestCells = mayoManifestEjb.readFileAsCellGrid(filename, messageCollection);
        if (!manifestCells.values().isEmpty()) {
            // Formats the manifest upload spreadsheet cells into an array of strings indexed by
            // sheet, row, and column for display purposes.
            manifestSheetnames = manifestCells.keySet().stream().
                    sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (String sheetname : manifestSheetnames) {
                List<List<String>> cellGrid = manifestCells.get(sheetname);
                int numRows = cellGrid.size();
                int maxColumns = cellGrid.stream().mapToInt(List::size).max().orElse(0);
                String[][] dataArray = new String[numRows][maxColumns];
                manifestArray.put(sheetname, dataArray);
                if (!cellGrid.isEmpty()) {
                    MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
                    processor.initHeaders(cellGrid.get(0), null);
                    for (int rowIdx = 0; rowIdx < numRows; ++rowIdx) {
                        List<String> columns = manifestCells.get(sheetname).get(rowIdx);
                        for (int columnIdx = 0; columnIdx < columns.size(); ++columnIdx) {
                            String value = columns.get(columnIdx);
                            // If there is a date column, converts it.
                            if (rowIdx > 0 && processor.isDateColumn(columnIdx)) {
                                value = PoiSpreadsheetParser.convertDoubleStringToDateString(value);
                            }
                            dataArray[rowIdx][columnIdx] = value;
                        }
                    }
                }
            }
            addMessages(messageCollection);
            return new ForwardResolution(PAGE3);

        } else {
            messageCollection.addWarning("Cannot find %s in Google Storage.", filename);
            addMessages(messageCollection);
            return new ForwardResolution(PAGE1);
        }
    }

    /**
     * Makes samples and tubes for the rack whose barcode has been scanned. If there is also a
     * ManifestSession for the scanned package barcode, finds the correct ManifestRecords for this
     * rack and links Metadata to the samples and tubes.
     */
    @HandlesEvent(SAVE_EVENT)
    public Resolution saveEvent() {
        if (!messageCollection.hasErrors()) {
            // Errors if the package barcode, rack barcode, or rackScan is blank.
            if (StringUtils.isBlank(packageBarcode) && StringUtils.isBlank(rackBarcode)) {
                messageCollection.addError("Package or rack barcode is blank.");
            }
            rackScan = new LinkedHashMap<>();
            for (String rackScanEntry : rackScanEntries) {
                String[] tokens = rackScanEntry.split(" ");
                rackScan.put(tokens[0], (tokens.length > 1) ? tokens[1] : "");
            }
            if (!rackScan.values().stream().filter(StringUtils::isNotBlank).findFirst().isPresent()) {
                messageCollection.addError("Rack scan has no tube barcodes.");
            }
        }
        LabVessel existingRack = null;
        if (!messageCollection.hasErrors()) {
            existingRack = labVesselDao.findByIdentifier(rackBarcode);
            // Finds an existing rack that matches the rack barcode, and makes an error message if
            // overwrite is not set. Also errors if the rack is not a previously uploaded rack.
            if (canOverwrite) {
                if (overwriteFlag) {
                    messageCollection.addInfo("Rack " + rackBarcode + " already exists and will be overwritten.");
                } else {
                    messageCollection.addError("Rack " + rackBarcode + " already exists and overwrite isn't selected.");
                }
            } else if (existingRack != null) {
                if (OrmUtil.proxySafeIsInstance(existingRack, RackOfTubes.class)) {
                    if (!OrmUtil.proxySafeCast(existingRack, RackOfTubes.class).getJiraTickets().stream().
                            filter(ticket -> ticket.getTicketName().startsWith(
                                    CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                            findFirst().isPresent()) {
                        messageCollection.addError("Rack " + rackBarcode +
                                " already exists but isn't an uploaded rack.");
                    }
                }
            }
        }

        if (!messageCollection.hasErrors()) {
            if (existingRack == null) {
                // If the rack is new but one of the tube barcodes matches an existing vessel,
                // error if it is not a Mayo upload tube, and error if overwrite is not set.
                List<LabVessel> existingTubes = labVesselDao.findByListIdentifiers(new ArrayList<>(rackScan.values()));
                if (!existingTubes.isEmpty()) {
                    // Checks existing tubes for any non-Mayo samples, indicated by the absence of Biobank Id.
                    String nonMayoSamples = existingTubes.stream().
                            filter(labVessel -> labVessel.getMercurySamples() != null).
                            flatMap(labVessel -> labVessel.getMercurySamples().stream()).
                            filter(mercurySample -> !mercurySample.getMetadata().stream().
                                    filter(metadata -> metadata.getKey() == Metadata.Key.BIOBANK_ID).
                                    findFirst().isPresent()).
                            map(MercurySample::getSampleKey).sorted().distinct().collect(Collectors.joining(", "));
                    if (StringUtils.isNotBlank(nonMayoSamples)) {
                        messageCollection.addError("Cannot save samples that match existing non-Mayo samples: " +
                                nonMayoSamples);
                    } else {
                        String tubeBarcodes = StringUtils.join(existingTubes, ", ");
                        if (!overwriteFlag) {
                            messageCollection.addError(
                                    "Overwrite must be selected in order to re-save existing tubes: " + tubeBarcodes);
                        } else {
                            messageCollection.addInfo("Existing tubes will be overwritten: " + tubeBarcodes);
                        }
                    }
                }
            }
        }
        if (!messageCollection.hasErrors()) {
            // Looks up or makes tubes, rack, and RCT ticket, and links them. If the manifest for the package is
            // found, makes samples and links them to the tubes, and links the sample metadata from the manifest.
            mayoManifestEjb.lookupOrMakeVesselsAndSamples(packageBarcode, rackBarcode, rackScan, filename,
                    overwriteFlag, messageCollection);
        }
        if (!messageCollection.hasErrors()) {
            addMessage("Successfully received samples in Mercury.");
        }
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


    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    /** Return an ordered list of the rack column names. */
    public List<String> getRackColumns() {
        return vesselGeometry != null ? Arrays.asList(vesselGeometry.getColumnNames()) : Collections.emptyList();
	}

    /** Return an ordered list of the rack row names. */
	public List<String> getRackRows() {
        return vesselGeometry != null ? Arrays.asList(vesselGeometry.getRowNames()) : Collections.emptyList();
	}

    /** Returns the SampleName or empty string for the given position name. */
	public String getSampleAt(String rowName, String columnName) {
        return StringUtils.trimToEmpty(rackScan.get(rowName + columnName));
    }

    /** Returns true if the rack identified by rackBarcode can be overwritten. */
	public boolean getCanOverwrite() {
        return canOverwrite;
	}

    public void setCanOverwrite(boolean canOverwrite) {
        this.canOverwrite = canOverwrite;
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

    public boolean isOverwriteFlag() {
        return overwriteFlag;
    }

    public void setOverwriteFlag(boolean overwriteFlag) {
        this.overwriteFlag = overwriteFlag;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) {
        this.vesselGeometry = vesselGeometry;
    }

    public String[][] getManifestArray(String sheetname) {
        return manifestArray.get(sheetname);
    }

    public List<String> getManifestSheetnames() {
        return manifestSheetnames;
    }

    public String getPackageBarcode() {
        return packageBarcode;
    }

    public void setPackageBarcode(String packageBarcode) {
        this.packageBarcode = packageBarcode;
        // Sets the filename too.
        filename = packageBarcode + ".xlsx";
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public void setRackBarcode(String rackBarcode) {
        this.rackBarcode = rackBarcode;
    }

    public String getFilename() {
        return filename;
    }
}
