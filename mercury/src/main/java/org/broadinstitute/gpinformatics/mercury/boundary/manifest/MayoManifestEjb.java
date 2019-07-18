package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.infrastructure.QuarantinedDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.Quarantined;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoAdminActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoPackageReceiptActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoSampleReceiptActionBean;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
@Stateful
public class MayoManifestEjb {
    private static Log logger = LogFactory.getLog(MayoManifestEjb.class);
    public static final String ACCESSIONED = "Accessioned rack %s with %s samples %s.";
    public static final String ALREADY_ACCESSIONED = "Rack %s is already accessioned.";
    public static final String ALREADY_ACCESSIONED_SAMPLES = "Samples %s are already accessioned.";
    public static final String CANNOT_ADD_ACCESSIONED = "Package receipt cannot add existing rack %s.";
    public static final String CANNOT_ADD_RECEIVED = "Package receipt cannot add rack %s received by %s.";
    public static final String CANNOT_REMOVE_ACCESSIONED = "Package re-receipt cannot remove accessioned rack %s.";
    public static final String EXTRA_IN_MANIFEST = "Manifest contains additional rack barcode %s.";
    public static final String INVALID_MANIFEST = "File %s does not contain a manifest.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String MULTIPLE_FILES = "Found multiple files for %s : %s.";
    public static final String NEEDS_ALLOW_UPDATE =
            "%s has already been received. Allow Update must be checked to update it.";
    public static final String NEEDS_MANUAL_LINKING = "You'll need to link a manifest file to this package.";
    public static final String NO_SUCH_FILE = "Cannot find a file for %s.";
    public static final String NO_UPDATE_CHANGES = "No changes were found.";
    public static final String NOT_IN_MANIFEST = "Manifest does not contain %s %s.";
    public static final String NOT_LINKED = "Package %s is not linked to a valid manifest.";
    public static final String NOT_RECEIVED = "Package %s has not been received.";
    public static final String ONLY_METADATA_CHANGES =
            "Manifest update cannot change tubes or positions in accessioned rack %s.";
    public static final String PACKAGE_LINKED = "Package %s is linked to manifest in file %s.";
    public static final String QUARANTINED = "%s is quarantined due to: %s.";
    public static final String RACK_NOT_RECEIVED = "Package containing %s has not been received.";
    public static final String RACK_UPDATED = "Rack %s %s.";
    public static final String RCT = "Ticket <a id='rctTicketUrl' href=\"%s\">%s</a> was created or updated.";
    public static final String RECEIVED_LINKED = "Package %s is received and linked to manifest %s.";
    public static final String RECEIVED_QUARANTINED = "Package %s is received and quarantined for %s.";
    public static final String SAMPLES_UPDATED = "Metadata was updated for %d samples: %s.";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String UNQUARANTINED = "%s was unquarantined.";
    public static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest shows %s.";
    enum MessageLevel {NONE, WARN_ONLY, ERROR};

    public static final Map<String, CustomFieldDefinition> JIRA_DEFINITION_MAP =
            new HashMap<String, CustomFieldDefinition>() {{
                put("MaterialTypeCounts", new CustomFieldDefinition("customfield_15660", "Material Type Counts", true));
                put("Racks", new CustomFieldDefinition("customfield_18060", "Rack IDs", false));
                put("ShipmentCondition", new CustomFieldDefinition("customfield_15661", "Shipment Condition", false));
                put("TrackingNumber", new CustomFieldDefinition("customfield_15663", "Tracking Number", false));
                put("KitDeliveryMethod", new CustomFieldDefinition("customfield_13767", "Kit Delivery Method", false));
                put("RequestingPhysician",
                        new CustomFieldDefinition("customfield_16163", "Requesting Physician", false));
                put("ReceiptType", new CustomFieldDefinition("customfield_17360", "Receipt Type", false));
            }};

    private static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    // Assumes every rack is a 96 tube Matrix rack.
    public static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;
    private final static String JIRA_NONE = "None";

    private ManifestSessionDao manifestSessionDao;
    private GoogleBucketDao googleBucketDao;
    private UserBean userBean;
    private LabVesselDao labVesselDao;
    private MercurySampleDao mercurySampleDao;
    private TubeFormationDao tubeFormationDao;
    private JiraService jiraService;
    private QuarantinedDao quarantinedDao;

    /**
     * CDI constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    public MayoManifestEjb() {
    }

    @Inject
    public MayoManifestEjb(ManifestSessionDao manifestSessionDao, GoogleBucketDao googleBucketDao, UserBean userBean,
            LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao, TubeFormationDao tubeFormationDao,
            QuarantinedDao quarantinedDao, JiraService jiraService, Deployment deployment) {
        this.manifestSessionDao = manifestSessionDao;
        this.googleBucketDao = googleBucketDao;
        this.userBean = userBean;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.tubeFormationDao = tubeFormationDao;
        this.quarantinedDao = quarantinedDao;
        this.jiraService = jiraService;
        // This config is from the yaml file.
        MayoManifestConfig mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    /**
     * Package receipt validation should be comprehensive and must not persist entities since the
     * lab user may choose to cancel the receipt and fix problems first.
     *
     * UI errors should only be given for things that prevent the lab user from doing a receipt-only
     * operation. Manifest parsing and validation problems are warnings.
     */
    public void packageReceiptValidation(MayoPackageReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();

        // Finds a suitable file, or validates the bean's filename.
        String filename = searchForFile(bean);
        bean.setFilename(filename);
        messages.addWarning(messages.getErrors());
        messages.getErrors().clear();

        // Parses the spreadsheet for validation only. Any errors are turned into warnings
        // since a receipt-only operation does not need to have a manifest.
        Pair<List<ManifestRecord>, List<List<String>>> extractPair =
                extractManifestRecords(packageId, bean.getRackBarcodes(), filename, MessageLevel.WARN_ONLY, messages);
        List<ManifestRecord> records = extractPair.getLeft();
        bean.setManifestCellGrid(extractPair.getRight());
        if (!messages.hasWarnings() && records.isEmpty() && StringUtils.isNotBlank(filename)) {
            messages.addWarning(INVALID_MANIFEST, filename);
        }

        // If there are no valid manifest records, the lab user will need to manually link a
        // corrected manifest file to this package.
        if (CollectionUtils.isEmpty(records)) {
            messages.addInfo(NEEDS_MANUAL_LINKING, packageId);
        }
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession != null) {
            if (!bean.isAllowUpdate()) {
                messages.addError(NEEDS_ALLOW_UPDATE, packageId);
            }
            List<String> removedRacks = CollectionUtils.subtract(manifestSession.getVesselLabels(),
                    bean.getRackBarcodes()).stream().
                    sorted().collect(Collectors.toList());
            // Package re-receipt cannot remove accessioned racks.
            String accessionedRemovals = labVesselDao.findByListIdentifiers(removedRacks).stream().
                    map(LabVessel::getLabel).sorted().collect(Collectors.joining(" "));
            if (!accessionedRemovals.isEmpty()) {
                messages.addError(CANNOT_REMOVE_ACCESSIONED, accessionedRemovals);
            }
        }
        List<String> addedRacks = CollectionUtils.subtract(bean.getRackBarcodes(),
                (manifestSession == null ? Collections.emptyList() : manifestSession.getVesselLabels())).stream().
                sorted().collect(Collectors.toList());
        // Package receipt cannot use existing racks because the manifest cannot define the rack content.
        List<String> accessionedAdds = labVesselDao.findByListIdentifiers(addedRacks).stream().
                map(LabVessel::getLabel).sorted().collect(Collectors.toList());
        if (!accessionedAdds.isEmpty()) {
            messages.addError(CANNOT_ADD_ACCESSIONED, StringUtils.join(accessionedAdds, " "));
        }
        // Package receipt cannot use another package's received racks (even if not accessioned)
        // because the two packages can have conflicting manifests for the same rack.
        addedRacks.removeAll(accessionedAdds);
        addedRacks.stream().
                map(rack -> Pair.of(rack, manifestSessionDao.getSessionByVesselLabel(rack))).
                filter(pair -> pair.getRight() != null).
                sorted(Comparator.comparing(Pair::getLeft)).
                forEach(pair ->
                        messages.addError(CANNOT_ADD_RECEIVED, pair.getLeft(), pair.getRight().getSessionPrefix()));
    }

    /**
     * Finalizes a package receipt by making a manifest session (with or without records) and an RCT ticket.
     *
     * If the package already has a manifest session, this is a re-receipt and the manifest session is updated
     * including the received rack barcodes.
     */
    public void packageReceipt(MayoPackageReceiptActionBean bean) {
        String packageId = bean.getPackageBarcode();
        // It's validated by the action bean, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(packageId));

        MessageCollection messages = bean.getMessageCollection();
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        Set<String> previousRacks = new HashSet<>();
        if (manifestSession == null) {
            manifestSession = new ManifestSession(null, packageId, userBean.getBspUser(), false,
                    Collections.emptyList());
            manifestSessionDao.persist(manifestSession);
        } else {
            previousRacks.addAll(manifestSession.getVesselLabels());
            manifestSession.getVesselLabels().clear();
        }
        // Updates the received rack barcodes.
        manifestSession.getVesselLabels().addAll(bean.getRackBarcodes());

        // Any manifest parsing errors will have already been reported in the validation phase, and it's
        // assumed here that the manifest file hasn't changed since the time that the validation page's
        // "continue" button was clicked. Errors will cause the manifest session to have no records.
        Pair<List<ManifestRecord>, List<List<String>>> pair = extractManifestRecords(packageId,
                bean.getRackBarcodes(), bean.getFilename(), MessageLevel.NONE, messages);
        manifestSession.addRecords(pair.getLeft());
        bean.setManifestCellGrid(pair.getRight());

        // Updates package quarantine based on presence of manifest records.
        List<String> rctTicketComments = new ArrayList<>();
        String message;
        if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE,
                    packageId, Quarantined.MISSING_MANIFEST);
            message = String.format(RECEIVED_QUARANTINED, packageId, Quarantined.MISSING_MANIFEST);
            manifestSession.setManifestFilename("");
        } else {
            if (quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId)) {
                messages.addInfo(UNQUARANTINED, packageId);
            }
            manifestSession.setManifestFilename(bean.getFilename());
            message = String.format(RECEIVED_LINKED, packageId, bean.getFilename());
        }
        messages.addInfo(message);
        rctTicketComments.add(message);

        // Quarantines racks based on lab user input. Currently these get unquarantined by either
        // a successful accessioning or if the lab user changes their status in a package re-receipt.
        bean.getQuarantineBarcodeAndReason().forEach((barcode, reason) -> {
            previousRacks.remove(barcode);
            if (StringUtils.isNotBlank(reason)) {
                quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, barcode, reason);
                messages.addInfo(QUARANTINED, barcode, reason);
                rctTicketComments.add("Rack " + barcode + " is quarantined due to " + reason);
            }
        });
        // Unquarantines any racks from the previous receipt that were not in the re-receipt.
        previousRacks.forEach(label -> {
            if (quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, label)) {
                messages.addInfo(UNQUARANTINED, label);
            }
        });

        JiraTicket jiraTicket = makeOrUpdatePackageRct(bean, manifestSession, rctTicketComments);
        if (jiraTicket != null) {
            manifestSessionDao.persist(jiraTicket);
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            manifestSession.setReceiptTicket(jiraTicket.getTicketName());

            messages.addInfo(String.format(RCT, jiraTicket.getBrowserUrl(), jiraTicket.getTicketId()));
        }
    }

    /**
     * The ManifestSession of a previously received package is updated from the records in a manifest file.
     * Three cases are supported:
     * - The received package has no previously linked manifest data.
     * - Manifest data is present but none of the package racks have been accessioned.
     * - Package racks have been accessioned, but the manifest only changes the sample metadata.
     *
     * In each case the manifest file must match the rack barcodes entered at the time of package receipt.
     */
    public void updateManifest(MayoPackageReceiptActionBean bean) {
        // These are validated by the action bean or caller, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(bean.getPackageBarcode()));
        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();

        // A received package always has a manifest session. It may or may not have manifest records.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession == null) {
            messages.addError(NOT_RECEIVED, packageId);
            return;
        }
        List<String> receivedRacks = new ArrayList<>(manifestSession.getVesselLabels());
        // Finds a suitable file, or validates the bean's filename if it is present.
        String filename = searchForFile(bean);
        bean.setFilename(filename);
        // Makes the manifest records from the updated manifest file.
        Pair<List<ManifestRecord>, List<List<String>>> pair = extractManifestRecords(packageId, receivedRacks,
                filename, MessageLevel.ERROR, messages);
        List<ManifestRecord> newRecords = pair.getLeft();
        bean.setManifestCellGrid(pair.getRight());

        // Reports errors and makes no changes if there are manifest parsing or validation errors.
        // To accomplish a package rack change, the lab user should
        // first do a package re-receipt to change the vessel barcodes,
        // and then do the manifest update so that racks match.
        if (messages.hasErrors() || CollectionUtils.isEmpty(newRecords)) {
            return;
        }

        // The Allow Updates checkbox must be set to allow updates.
        if (!bean.isAllowUpdate()) {
            messages.addError(NEEDS_ALLOW_UPDATE, packageId);
            return;
        }

        // Checks if the new records are the same as the existing ones.
        if (manifestSession.getRecords().size() == newRecords.size() &&
                CollectionUtils.isEqualCollection(
                        manifestSession.getRecords().stream().
                                map(record -> record.getMetadata().stream().
                                        map(Metadata::toString).sorted().
                                        collect(Collectors.joining(","))).
                                sorted().collect(Collectors.toList()),
                        newRecords.stream().
                                map(record -> record.getMetadata().stream().
                                        map(Metadata::toString).sorted().
                                        collect(Collectors.joining(","))).
                                sorted().collect(Collectors.toList()))) {
            messages.addInfo(NO_UPDATE_CHANGES);
            return;
        }

        // Extracts the changes found in the manifest records.
        Pair<Map<MercurySample, Set<Metadata>>, Map<String, String>> diffPair =
                manifestDiffs(manifestSession, newRecords);
        Map<MercurySample, Set<Metadata>> metadataDiffs = diffPair.getLeft();
        Map<String, String> rackDiffs = diffPair.getRight();

        // Errors if any accessioned racks are removed or have position or tube changes.
        String accessionedRacks = labVesselDao.findByListIdentifiers(new ArrayList<>(rackDiffs.keySet())).stream().
                map(LabVessel::getLabel).
                sorted().collect(Collectors.joining(" "));
        if (StringUtils.isNotBlank(accessionedRacks)) {
            messages.addError(ONLY_METADATA_CHANGES, accessionedRacks);
            return;
        }

        // Reports the rack changes and the sample metadata changes in the UI and the RCT ticket.
        List<String> updateMessages = new ArrayList<>();
        updateMessages.add(String.format(PACKAGE_LINKED, packageId, filename));
        if (!metadataDiffs.isEmpty()) {
            updateMessages.add(String.format(SAMPLES_UPDATED, metadataDiffs.keySet().size(),
                    metadataDiffs.keySet().stream().
                            map(MercurySample::getSampleKey).
                            sorted().collect(Collectors.joining(" "))));
        }
        rackDiffs.forEach((rack, description) ->
                updateMessages.add(String.format(RACK_UPDATED, rack, description)));
        updateMessages.forEach(updateMessage -> {
            messages.addInfo(updateMessage);
            addRctComment(manifestSession.getReceiptTicket(), messages, updateMessage);
        });

        if (!messages.hasErrors()) {
            // Updates the MercurySample metadata.
            metadataDiffs.forEach((mercurySample, diffs) -> mercurySample.updateMetadata(diffs));
            // Updates the manifest session.
            manifestSession.getRecords().clear();
            manifestSession.addRecords(newRecords);
            manifestSession.setManifestFilename(filename);

            // Unquarantines the package if had a missing manifest.
            Quarantined quarantinedPkg = quarantinedDao.findItem(Quarantined.ItemSource.MAYO,
                    Quarantined.ItemType.PACKAGE, packageId);
            if (quarantinedPkg != null && quarantinedPkg.getReason().equals(Quarantined.MISSING_MANIFEST)) {
                quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId);
                messages.addInfo(String.format(UNQUARANTINED, packageId));
            }
        }
    }

    /**
     * If the bean does not provide a filename, search the storage bucket for one that matches the package barcode.
     * If none found or multiple found, return empty string and generate error message.
     */
    private String searchForFile(MayoPackageReceiptActionBean bean){
        String filename = "";
        if (StringUtils.isNotBlank(bean.getFilename())) {
            if (googleBucketDao.exists(bean.getFilename(), bean.getMessageCollection())) {
                filename = bean.getFilename();
            } else {
                bean.getMessageCollection().addError(NO_SUCH_FILE, bean.getFilename());
            }
        } else {
            List<String> matchingFilenames = googleBucketDao.list(bean.getMessageCollection()).stream().
                    filter(name -> name.endsWith(".csv") && name.contains(bean.getPackageBarcode())).
                    collect(Collectors.toList());
            switch (matchingFilenames.size()) {
            case 0:
                bean.getMessageCollection().addError(NO_SUCH_FILE, bean.getPackageBarcode());
                break;
            case 1:
                filename = matchingFilenames.get(0);
                break;
            default:
                bean.getMessageCollection().addError(MULTIPLE_FILES, bean.getPackageBarcode(),
                        matchingFilenames.stream().sorted().collect(Collectors.joining(" ")));
            }
        }
        return filename;
    }

    /**
     * Reads a manifest file, extracts its records, and validates them, including compatibility with
     * the existing manifest session.
     *
     * @return list of manifest records from the file (empty if parsing or validation errors were found) and
     *         the manifest cellGrid.
     */
    private Pair<List<ManifestRecord>, List<List<String>>> extractManifestRecords(String packageId,
            Collection<String> rackBarcodes, String filename, MessageLevel messageLevel, MessageCollection messages) {

        // Reads and parses the spreadsheet.
        List<List<String>> cellGrid = readManifestFileCellGrid(filename, messages);
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, filename, messages);
        if (records.isEmpty()) {
            if (StringUtils.isNotBlank(filename)) {
                messages.addError(INVALID_MANIFEST, filename);
            }
        } else if (!packageId.equals(records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
            messages.addError(NOT_IN_MANIFEST, "Package ID", packageId);
        } else {
            // Rack barcodes must match the entered rack barcodes.
            Set<String> spreadsheetBarcodes = records.stream().
                    map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.BOX_ID).getValue()).
                    collect(Collectors.toSet());
            String notInSpreadsheet = CollectionUtils.subtract(rackBarcodes, spreadsheetBarcodes).
                    stream().sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(notInSpreadsheet)) {
                messages.addError(NOT_IN_MANIFEST, "rack barcode", notInSpreadsheet);
            }
            String notEntered = CollectionUtils.subtract(spreadsheetBarcodes, rackBarcodes).
                    stream().sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(notEntered)) {
                messages.addError(EXTRA_IN_MANIFEST, notEntered);
            }
        }
        // Doesn't use the records if there was a parsing or validation error.
        if (messages.hasErrors()) {
            records.clear();
        }
        // Adjusts the messages level depending on the processing phase.
        if (messageLevel == MessageLevel.NONE) {
            messages.getErrors().clear();
            messages.getWarnings().clear();
        } else if (messageLevel == MessageLevel.WARN_ONLY) {
            messages.addWarning(messages.getErrors());
            messages.getErrors().clear();
        }

        return Pair.of(records, cellGrid);
    }

    /**
     * Extracts the differences found in the new manifest records.
     * @return the updates to existing MercurySample metadata, and a map of rack barcode to description of changes.
     */
    private Pair<Map<MercurySample, Set<Metadata>>, Map<String, String>> manifestDiffs(
            ManifestSession manifestSession, List<ManifestRecord> updateRecords) {

        // Maps the update record's sample name to update sample metadata.
        Multimap<String, Metadata> newMetadataRecords = HashMultimap.create();
        for (ManifestRecord manifestRecord : updateRecords) {
            // Sample name is the tube barcode.
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            newMetadataRecords.putAll(sampleName, manifestRecord.getMetadata());
        }
        // Collects the metadata differences for any MercurySamples (i.e. accessioned samples).
        Map<MercurySample, Set<Metadata>> metadataDiffs = new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(newMetadataRecords.keySet())) {
            Map<Metadata.Key, Metadata> existingData = mercurySample.getMetadata().stream().
                    collect(Collectors.toMap(Metadata::getKey, Function.identity()));
            // Checks if the manifest update wants to add a new metadata type or change an existing value.
            Set<Metadata> diffs = newMetadataRecords.get(mercurySample.getSampleKey()).stream().
                    filter(newData -> !existingData.containsKey(newData.getKey()) ||
                            !newData.getValue().equals(existingData.get(newData.getKey()).getValue())).
                    collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(diffs)) {
                metadataDiffs.put(mercurySample, diffs);
            }
        }

        // Lists the update record's rack barcode, position, and tube barcode.
        List<Triple<String, String, String>> updateVessels = updateRecords.stream().
                map(record -> Triple.of(
                        record.getValueByKey(Metadata.Key.BOX_ID),
                        record.getValueByKey(Metadata.Key.WELL_POSITION),
                        record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE))).
                sorted(Comparator.comparing(triple -> triple.toString("%s %s %s"))).
                collect(Collectors.toList());

        // Lists the existing manifest record's rack barcode, position, and tube barcode.
        List<Triple<String, String, String>> existingVessels = manifestSession.getRecords().stream().
                map(record -> Triple.of(
                        record.getValueByKey(Metadata.Key.BOX_ID),
                        record.getValueByKey(Metadata.Key.WELL_POSITION),
                        record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE))).
                sorted(Comparator.comparing(triple -> triple.toString("%s %s %s"))).
                collect(Collectors.toList());

        Map<String, String> rackDiffs = new HashMap<>();
        List<String> updateRacks = updateVessels.stream().
                map(Triple::getLeft).sorted().distinct().collect(Collectors.toList());
        List<String> existingRacks = existingVessels.stream().
                map(Triple::getLeft).sorted().distinct().collect(Collectors.toList());
        // Looks for added and removed racks.
        CollectionUtils.subtract(updateRacks, existingRacks).forEach(rack ->
                rackDiffs.put(rack, "added"));
        CollectionUtils.subtract(existingRacks, updateRacks).forEach(rack ->
                rackDiffs.put(rack, "removed"));
        // Counts the tube and position changes per rack.
        CollectionUtils.intersection(updateRacks, existingRacks).forEach(rack -> {
            Map<String, String> updateTubeAndPosition = updateVessels.stream().
                    filter(triple -> rack.equals(triple.getLeft())).
                    collect(Collectors.toMap(Triple::getRight, Triple::getMiddle));
            Map<String, String> existingTubeAndPosition = existingVessels.stream().
                    filter(triple -> rack.equals(triple.getLeft())).
                    collect(Collectors.toMap(Triple::getRight, Triple::getMiddle));
            // Counts tube adds and removals and position changes.
            int tubeAdds = CollectionUtils.subtract(
                    updateTubeAndPosition.keySet(), existingTubeAndPosition.keySet()).size();
            int tubeRemovals = CollectionUtils.subtract(
                    existingTubeAndPosition.keySet(), updateTubeAndPosition.keySet()).size();
            long positionChanges = existingTubeAndPosition.entrySet().stream().
                    filter(entry -> !Objects.equal(entry.getValue(), updateTubeAndPosition.get(entry.getKey()))).
                    count();
            List<String> description = new ArrayList<String>() {{
                if (tubeAdds > 0) {
                    add(tubeAdds + " tube adds");
                }
                if (tubeRemovals > 0) {
                    add(tubeRemovals + " tube removals");
                }
                if (positionChanges > 0) {
                    add(positionChanges + " position changes");
                }
            }};
            if (!description.isEmpty()) {
                rackDiffs.put(rack, StringUtils.join(description, ", "));
            }
        });

        return Pair.of(metadataDiffs, rackDiffs);
    }

    /**
     * Validates the rack scan. Neither the rack vessel nor the tubes are
     * expected to exist yet, so it's an error if they do.
     */
    public void validateForAccessioning(MayoSampleReceiptActionBean bean) {
        // The action bean checks that the rack and scan are not empty. These asserts are for unit tests.
        assert (bean.getRackScan() != null && bean.getRackScan().size() > 0);
        assert (StringUtils.isNotBlank(bean.getRackBarcode()));

        MessageCollection messages = bean.getMessageCollection();
        List<String> barcodes = new ArrayList<>();
        // Turns the rack scan into a list of position & sample to pass to and from jsp.
        bean.getRackScanEntries().clear();
        bean.getRackScan().forEach((key, value) -> {
            if (StringUtils.isNotBlank(value)) {
                barcodes.add(value);
                bean.getRackScanEntries().add(StringUtils.join(key, " ", value));
            }
        });
        assert (!barcodes.isEmpty());

        // Rack barcode must be linked to a ManifestSession of a received Mayo package.
        ManifestSession manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
        if (manifestSession == null) {
            messages.addError(RACK_NOT_RECEIVED, bean.getRackBarcode());
        } else if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            messages.addError(NOT_LINKED, manifestSession.getSessionPrefix());
        } else {
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
        }

        // Disallows vessels that were already accessioned.
        barcodes.add(0, bean.getRackBarcode());
        String existingVessels = labVesselDao.findByBarcodes(barcodes).entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                map(Map.Entry::getKey).
                sorted().
                collect(Collectors.joining(" "));
        if (!existingVessels.isEmpty()) {
            messages.addError(ALREADY_ACCESSIONED, existingVessels);
        }

        // Rack scan positions must all be valid for the rack type.
        String invalidPositions = CollectionUtils.subtract(
                bean.getRackScan().keySet(),
                Stream.of(DEFAULT_RACK_TYPE.getVesselGeometry().getVesselPositions()).
                        map(VesselPosition::name).
                        collect(Collectors.toList())).
                stream().sorted().collect(Collectors.joining(" "));
        if (!invalidPositions.isEmpty()) {
            messages.addError(UNKNOWN_WELL_SCAN, invalidPositions);
        }
    }

    /**
     * Accessions rack, tubes, samples provided the manifest is found and data matches up.
     */
    public void accession(MayoSampleReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        // Finds the existing manifest session.
        ManifestSession manifestSession;
        if (bean.getManifestSessionId() == null) {
            // Finds the manifest session. It must have manifest records from a manifest file.
            manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
            if (manifestSession == null) {
                messages.addError(RACK_NOT_RECEIVED, bean.getRackBarcode());
                return;
            }
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
        } else {
            manifestSession = manifestSessionDao.find(bean.getManifestSessionId());
            // The action bean obtained id from an existing ManifestSession. This assert is for unit tests.
            assert (manifestSession != null);
        }
        // If manifest records don't exist it means the package was received without having a manifest file.
        // This is unusable for accessioning.
        if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            messages.addError(NOT_LINKED, manifestSession.getSessionPrefix());
            return;
        }
        // Finds the records for the rack.
        List<ManifestRecord> manifestRecords =
                manifestSession.findRecordsByKey(bean.getRackBarcode(), Metadata.Key.BOX_ID);
        if (CollectionUtils.isEmpty(manifestRecords)) {
            messages.addError(NOT_IN_MANIFEST, "Box ID", bean.getRackBarcode());
            return;
        }

        // Matches the rack scan info against manifest info.
        Map<String, Pair<String, String>> mismatches = new HashMap<>();
        Map<String, String> manifestPositionToTube = new HashMap<>();
        for (ManifestRecord manifestRecord : manifestRecords) {
            String manifestTube = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            String manifestPosition = manifestRecord.getValueByKey(Metadata.Key.WELL_POSITION);
            manifestPositionToTube.put(manifestPosition, manifestTube);
            String rackTube = bean.getRackScan().get(manifestPosition);
            if (!Objects.equal(rackTube, manifestTube)) {
                mismatches.put(manifestPosition, Pair.of(rackTube, manifestTube));
            }
        }
        for (String position : CollectionUtils.subtract(bean.getRackScan().keySet(), manifestPositionToTube.keySet())) {
            String rackTube = bean.getRackScan().get(position);
            mismatches.put(position, Pair.of(rackTube, null));
        }
        mismatches.keySet().stream().sorted().forEach(position -> {
            String rackTube = mismatches.get(position).getLeft();
            String manifestTube = mismatches.get(position).getRight();
            messages.addError(WRONG_TUBE_IN_POSITION, position,
                    StringUtils.isBlank(rackTube) ? "no tube" : rackTube,
                    StringUtils.isBlank(manifestTube) ? "no tube" : manifestTube);
        });

        // Quarantines the rack if there is a mismatch.
        if (!mismatches.isEmpty()) {
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, bean.getRackBarcode(),
                    Quarantined.MISMATCH);
        }

        // It's an error if any of the samples exist.
        // The MercurySample sample key is the same as the tube barcode.
        String accessionedSamples = mercurySampleDao.findBySampleKeys(manifestPositionToTube.values()).stream().
                map(MercurySample::getSampleKey).collect(Collectors.joining(" "));
        if (!accessionedSamples.isEmpty()) {
            messages.addError(ALREADY_ACCESSIONED_SAMPLES, accessionedSamples);
        }

        if (messages.hasErrors()) {
            return;
        }

        // Creates tubes.
        Map<VesselPosition, BarcodedTube> vesselPositionToTube = manifestPositionToTube.entrySet().stream().
                collect(Collectors.toMap(
                        mapEntry -> VesselPosition.getByName(mapEntry.getKey()),
                        mapEntry -> new BarcodedTube(mapEntry.getValue(), DEFAULT_TUBE_TYPE))
                );
        List<Object> newEntities = new ArrayList<>(vesselPositionToTube.values());

        // Creates the rack and tube formation.
        String digest = TubeFormation.makeDigest(bean.getRackScan().entrySet().stream().
                map(mapEntry -> Pair.of(VesselPosition.getByName(mapEntry.getKey()), mapEntry.getValue()))
                .collect(Collectors.toList()));
        TubeFormation tubeFormation = tubeFormationDao.findByDigest(digest);
        if (tubeFormation == null) {
            tubeFormation = new TubeFormation(vesselPositionToTube, DEFAULT_RACK_TYPE);
            newEntities.add(tubeFormation);
        }
        RackOfTubes rack = new RackOfTubes(bean.getRackBarcode(), DEFAULT_RACK_TYPE);
        newEntities.add(rack);
        tubeFormation.addRackOfTubes(rack);
        rack.getTubeFormations().add(tubeFormation);

        // Collects sample info from the manifest.
        Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
        for (ManifestRecord manifestRecord : manifestRecords) {
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());
        }

        // Sets the tube's volume and concentration from values in the manifest.
        for (BarcodedTube tube : vesselPositionToTube.values()) {
            String sampleName = tube.getLabel();
            for (Metadata metadata : sampleMetadata.get(sampleName)) {
                if (metadata.getKey() == Metadata.Key.VOLUME) {
                    tube.setVolume(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(metadata.getValue())));
                } else if (metadata.getKey() == Metadata.Key.CONCENTRATION) {
                    tube.setConcentration(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(metadata.getValue())));
                }
            }
        }

        // Creates or updates samples. All MercurySamples will have MetadataSource.MERCURY.
        for (BarcodedTube tube : vesselPositionToTube.values()) {
            String sampleName = tube.getLabel();
            MercurySample mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.MERCURY);
            mercurySample.updateMetadata(new HashSet<>(sampleMetadata.get(sampleName)));
            tube.addSample(mercurySample);
            newEntities.add(mercurySample);
        }

        messages.addInfo(String.format(ACCESSIONED, bean.getRackBarcode(),
                sampleMetadata.keySet().size(),
                sampleMetadata.keySet().stream().sorted().collect(Collectors.joining(" "))));

        boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                Quarantined.ItemType.RACK, bean.getRackBarcode());
        if (wasQuarantined) {
            messages.addInfo(String.format(UNQUARANTINED, bean.getRackBarcode()));
        }

        // Adds comment to the existing RCT.
        if (addRctComment(manifestSession.getReceiptTicket(), messages,
                String.format(ACCESSIONED, bean.getRackBarcode(), manifestPositionToTube.values().size(),
                        manifestPositionToTube.values().stream().sorted().collect(Collectors.joining(" "))))) {
            // Only persists entities if Jira succeeds.
            labVesselDao.persistAll(newEntities);
        }
    }

    /**
     * Obtains the manifest file in a cell grid for the filename given.
     */
    public void readManifestFileCellGrid(MayoAdminActionBean bean) {
        bean.setManifestCellGrid(readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection()));
    }

    /**
     * Runs a step by step credentialing and access of the configured Google bucket.
     * Status is put into info messages for the UI.
     * Puts a listing of all bucket filenames in the bean.
     */
    public void testAccess(MayoAdminActionBean bean) {
        bean.setBucketList(googleBucketDao.test(bean.getMessageCollection()));
    }

    /**
     * Rewrites the credential file.
     */
    public void uploadCredential(MayoAdminActionBean bean) {
        try {
            String jsonContent = StringUtils.join(IOUtils.readLines(bean.getCredentialFileReader()),
                    System.lineSeparator());
            googleBucketDao.uploadCredential(false, jsonContent, bean.getMessageCollection());
        } catch (Exception e) {
            logger.error("Exception while uploading credential file: ", e);
            bean.getMessageCollection().addError("Cannot upload credential file: " + e.toString());
        }
    }

    /**
     * Changes the Google bucket access credential for Mercury's service account.
     * The new credential is written to the credential file that is configured in yaml.
     */
    public void rotateServiceAccountKey(MayoAdminActionBean bean) {
        googleBucketDao.rotateServiceAccountKey(bean.getMessageCollection());
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns empty if the manifest file doesn't exist or could not be read.
     * Bucket access or csv parsing errors are put in the MessageCollection.
     */
    private @NotNull
    List<List<String>> readManifestFileCellGrid(String filename, MessageCollection messages) {
        if (StringUtils.isNotBlank(filename)) {
            byte[] spreadsheet = googleBucketDao.download(filename, messages);
            if (spreadsheet == null || spreadsheet.length == 0) {
                return Collections.emptyList();
            } else {
                return MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, filename, messages);
            }
        }
        return Collections.emptyList();
    }


    /** Makes a new RCT in Jira for the Mayo package or updates an existing RCT. */
    private JiraTicket makeOrUpdatePackageRct(MayoPackageReceiptActionBean bean,
            @NotNull ManifestSession manifestSession, List<String> comments) {
        String title = bean.getPackageBarcode();
        CustomField titleField = new CustomField(new CustomFieldDefinition("summary", "Summary", true), title);

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(JIRA_DEFINITION_MAP.get("ShipmentCondition"),
                    StringUtils.defaultIfBlank(bean.getShipmentCondition(), " ")));
            add(new CustomField(JIRA_DEFINITION_MAP.get("TrackingNumber"),
                    StringUtils.defaultIfBlank(bean.getTrackingNumber(), " ")));
            // Delivery Method is a Jira selection and does not like to be set if value is "None".
            if (StringUtils.isNotBlank(bean.getDeliveryMethod()) && !JIRA_NONE.equals(bean.getDeliveryMethod())) {
                add(new CustomField(bean.getDeliveryMethod(), JIRA_DEFINITION_MAP.get("KitDeliveryMethod")));
            }
            add(new CustomField(JIRA_DEFINITION_MAP.get("Racks"), bean.getRackBarcodeString()));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"), " "));
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), " "));
        }};

        JiraTicket jiraTicket = null;
        JiraIssue jiraIssue;
        try {
            if (StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
                jiraTicket = labVesselDao.findSingle(JiraTicket.class, JiraTicket_.ticketId,
                        manifestSession.getReceiptTicket());
            }
            if (jiraTicket == null) {
                jiraIssue = jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT,
                        userBean.getLoginUserName(), CreateFields.IssueType.RECEIPT, title, customFieldList);
                // Re-writes the RCT title which for some reason gets ignored in createIssue.
                jiraService.updateIssue(jiraIssue.getKey(), Collections.singleton(titleField));
                jiraTicket = new JiraTicket(jiraService, jiraIssue.getKey());
            } else {
                // If a reference to a previous RCT exists, updates the RCT.
                jiraIssue = jiraService.getIssue(manifestSession.getReceiptTicket());
                customFieldList.add(titleField);
                jiraService.updateIssue(jiraIssue.getKey(), customFieldList);
            }

            // Writes any comments.
            for (String comment : comments) {
                jiraIssue.addComment(comment);
            }
            return jiraTicket;

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
            return null;
        }
    }

    private boolean addRctComment(String rctId, MessageCollection messageCollection, String comment) {
        JiraIssue jiraIssue = null;
        try {
            jiraIssue = jiraService.getIssue(rctId);
            if (jiraIssue == null) {
                messageCollection.addError("Cannot find Jira ticket " + rctId);
            } else {
                jiraIssue.addComment(comment);
            }
        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            messageCollection.addError(JIRA_PROBLEM, e.toString());
        }
        return jiraIssue != null;
    }

    public UserBean getUserBean() {
        return userBean;
    }
}
