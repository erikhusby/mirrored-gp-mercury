package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
    public static final String CANNOT_REDO_RECEIPT = "Cannot re-receive package after accessioning.";
    public static final String EXTRA_IN_MANIFEST = "Manifest contains additional rack barcode %s.";
    public static final String INVALID_MANIFEST = "File %s does not contain a manifest.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String MULTIPLE_FILES = "Found multiple files for %s : %s.";
    public static final String NEEDS_ALLOW_UPDATE =
            "%s has already been received. Allow Update must be checked to update it.";
    public static final String NEEDS_MANUAL_LINKING = "You'll need to link a manifest file to this package.";
    public static final String NO_SUCH_FILE = "Cannot find a file for %s.";
    public static final String NO_SAMPLES_UPDATED = "No sample metadata changes were found; is the filename correct?";
    public static final String NOT_IN_MANIFEST = "Manifest does not contain %s %s.";
    public static final String NOT_LINKED = "Package %s is not linked to a valid manifest.";
    public static final String NOT_RECEIVED = "Package %s has not been received.";
    public static final String ONLY_METADATA_CHANGES =
            "Manifest update cannot change rack or position for accessioned sample tube %s.";
    public static final String PACKAGE_LINKED = "Package %s is linked to manifest from file %s.";
    public static final String QUARANTINED = "%s is quarantined due to: %s.";
    public static final String QUARANTINE_DELETES =
            "Package re-receipt will unquarantine previously quarantined items: %s.";
    public static final String RACK_NOT_RECEIVED = "Package containing %s has not been received.";
    public static final String RECEIVED =
            "Package %s is received and <a id='rctTicketUrl' href=\"%s\">RCT ticket</a> was created.";
    public static final String SAMPLES_NOT_UPDATED = "No metadata changes found for %d samples.";
    public static final String SAMPLES_UPDATED = "Metadata was updated for %d samples: %s.";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String UNQUARANTINED = "%s was unquarantined.";
    public static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest shows %s.";

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
     * Package receipt validation should be comprehensive and must not persist entities since
     * the lab user may choose to cancel the receipt and first fix any errors. OTOH it should
     * not generate errors for things that wouldn't prevent the lab user from a receipt-only
     * operation.
     */
    public void packageReceiptValidation(MayoPackageReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();

        List<ManifestRecord> records = null;
        // Looks for a manifest file for the package barcode. If there's only one matching file
        // it gets used. If none or multiple found, a warning is given and no file is used, i.e.
        // the ManifestSession has the rack barcodes as manifestVessels but no ManifestRecords.
        List<String> matchingFilenames = googleBucketDao.list(messages).stream().
                filter(name -> name.endsWith(".csv") && name.contains(packageId)).
                collect(Collectors.toList());
        if (matchingFilenames.size() == 0) {
            messages.addWarning(NO_SUCH_FILE, packageId);
        } else if (matchingFilenames.size() > 1) {
            messages.addWarning(MULTIPLE_FILES, packageId,
                    matchingFilenames.stream().sorted().collect(Collectors.joining(" ")));
        } else {
            bean.setFilename(matchingFilenames.get(0));
            // Parses the spreadsheet so errors can be found. Data mismatches and other manifest problems
            // are turned into warnings at this point because they do not stop a package receipt.
            records = extractManifestRecords(packageId, bean.getRackBarcodes(),
                    bean.getFilename(), messages);
            messages.addWarning(messages.getErrors());
            messages.getErrors().clear();
            if (!messages.hasWarnings() && records.isEmpty()) {
                messages.addWarning(INVALID_MANIFEST, bean.getFilename());
            }
        }
        // If no manifest file, the lab user will need to manually link a manifest file to this package later.
        if (CollectionUtils.isEmpty(records)) {
            messages.addWarning(NEEDS_MANUAL_LINKING, packageId);
        }
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession != null) {
            if (!bean.isAllowUpdate()) {
                messages.addError(NEEDS_ALLOW_UPDATE, packageId);
            }
            // The lab user cannot re-receive a package if the samples have been accessioned.
            if (CollectionUtils.isNotEmpty(
                    labVesselDao.findByListIdentifiers(new ArrayList<>(manifestSession.getVesselLabels())))) {
                messages.addError(CANNOT_REDO_RECEIPT);
            }
            // Warns about any quarantines from the previous receipt.
            String previousQuarantines = quarantinedDao.findItems(Quarantined.ItemSource.MAYO).stream().
                    map(Quarantined::getItem).
                    filter(item -> item.equals(packageId) ||
                            manifestSession.getVesselLabels().contains(item)).
                    sorted().
                    collect(Collectors.joining(" "));
            if (!previousQuarantines.isEmpty()) {
                messages.addWarning(QUARANTINE_DELETES, previousQuarantines);
            }
        }
    }

    /**
     * Finalizes a package receipt by making a manifest session (with or without records) and an RCT ticket.
     * This should succeed since validation has already been done.
     *
     * If a manifest session already exists, this is a re-receipt and the manifest session is updated
     * including the received rack barcodes.
     */
    public void packageReceipt(MayoPackageReceiptActionBean bean) {
        String packageId = bean.getPackageBarcode();
        // It's validated by the action bean, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(packageId));

        MessageCollection messages = bean.getMessageCollection();
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession == null) {
            manifestSession = new ManifestSession(null, packageId, userBean.getBspUser(), false,
                    Collections.emptyList());
            manifestSessionDao.persist(manifestSession);
        } else {
            // Unquarantines any package or rack quarantined in the previous receipt.
            quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE, packageId);
            manifestSession.getVesselLabels().forEach(label ->
                    quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, label));
            manifestSession.getVesselLabels().clear();
        }

        // Updates the received rack barcodes.
        manifestSession.getVesselLabels().addAll(bean.getRackBarcodes());

        // Any errors will have already been reported in the validation phase.
        // Errors are ignored here and the manifest session will have no manifest records.
        manifestSession.addRecords(extractManifestRecords(packageId, bean.getRackBarcodes(),
                bean.getFilename(), new MessageCollection()));

        // Quarantines the package if there are no manifest records.
        List<String> rctTicketComments = new ArrayList<>();
        if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE,
                    packageId, Quarantined.MISSING_MANIFEST);
            messages.addWarning(QUARANTINED, packageId, Quarantined.MISSING_MANIFEST);
            rctTicketComments.add(String.format(QUARANTINED, packageId, Quarantined.MISSING_MANIFEST));
        } else {
            rctTicketComments.add("Package " + packageId + " is received and linked to manifest " +
                    manifestSession.getManifestFilename());
        }
        // Quarantines racks based on lab user input. Currently these get unquarantined by a successful accessioning.
        bean.getQuarantineBarcodeAndReason().forEach((barcode, reason) -> {
            if (StringUtils.isNotBlank(reason)) {
                quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, barcode, reason);
                messages.addInfo(QUARANTINED, barcode, reason);
                rctTicketComments.add("Rack " + barcode + " is quarantined due to " + reason);
            }
        });
        JiraTicket jiraTicket = makePackageRct(bean, manifestSession, rctTicketComments);
        if (jiraTicket != null) {
            manifestSessionDao.persist(jiraTicket);
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            manifestSession.setReceiptTicket(jiraTicket.getTicketName());

            messages.addInfo(String.format(RECEIVED, packageId, jiraTicket.getBrowserUrl()));
        }
    }

    /**
     * Links a previously received package to a manifest file. This means updating the manifest session
     * with manifest records made from the manifest file.
     *
     * These cases are supported:
     * - The received package has no previously linked manifest data.
     * - The package samples are not accessioned and the manifest file matches package and rack barcodes.
     * - Samples are accessioned and the manifest only has changes to the sample metadata.
     */
    public void updateManifest(MayoPackageReceiptActionBean bean) {
        // These are validated by the action bean or caller, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(bean.getPackageBarcode()));

        MessageCollection messages = bean.getMessageCollection();
        String packageId = bean.getPackageBarcode();

        // Searches for a manifest file if the filename is not given.
        if (StringUtils.isBlank(bean.getFilename())) {
            List<String> matchingFilenames = googleBucketDao.list(messages).stream().
                    filter(name -> name.endsWith(".csv") && name.contains(packageId)).
                    collect(Collectors.toList());
            if (matchingFilenames.size() == 0) {
                messages.addError(NO_SUCH_FILE, packageId);
                return;
            } else if (matchingFilenames.size() > 1) {
                messages.addError(MULTIPLE_FILES, packageId,
                        matchingFilenames.stream().sorted().collect(Collectors.joining(" ")));
                return;
            } else {
                bean.setFilename(matchingFilenames.get(0));
            }
        }
        String filename = bean.getFilename();

        // There will always be a manifest session for a received package. It may or may not have manifest records.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession == null) {
            messages.addError(NOT_RECEIVED, packageId);
            return;
        }
        List<String> receivedRacks = new ArrayList<>(manifestSession.getVesselLabels());
        List<ManifestRecord> newRecords = extractManifestRecords(packageId, receivedRacks, filename, messages);
        // If there are manifest file parsing errors, or the manifest file doesn't match package and rack barcodes,
        // report errors to the user and make no changes.
        if (messages.hasErrors()) {
            return;
        }
        // Accessioned samples must not have updates to the rack, tube, or position changes.
        if (CollectionUtils.isNotEmpty(manifestSession.getRecords())) {
            if (!bean.isAllowUpdate()) {
                // The checkbox must be set to allow updates.
                messages.addError(NEEDS_ALLOW_UPDATE, packageId);

            } else if (CollectionUtils.isNotEmpty(labVesselDao.findByListIdentifiers(receivedRacks))) {
                // Extracts any sample metadata changes from the new records.
                Map<MercurySample, Set<Metadata>> metadataDiffs =
                        extractMetadataChanges(manifestSession, newRecords, messages);
                if (messages.hasErrors()) {
                    return;
                }
                // Does the MercurySample metadata updates.
                metadataDiffs.forEach((mercurySample, diffs) -> mercurySample.updateMetadata(diffs));

                // Reports the changes.
                List<String> updatedSamples = metadataDiffs.keySet().stream().
                        map(MercurySample::getSampleKey).
                        sorted().collect(Collectors.toList());
                if (updatedSamples.isEmpty()) {
                    messages.addWarning(NO_SAMPLES_UPDATED);
                } else {
                    messages.addInfo(SAMPLES_UPDATED, updatedSamples.size(),
                            StringUtils.join(updatedSamples, " "));
                    // Adds comment to the existing RCT.
                    addRctComment(manifestSession.getReceiptTicket(), messages,
                            "Sample metadata was updated from manifest file " +
                                    manifestSession.getManifestFilename() + " for samples " +
                                    StringUtils.join(updatedSamples, " "));
                }
            }
        }

        if (!messages.hasErrors()) {
            manifestSession.getRecords().clear();
            manifestSession.addRecords(newRecords);
            manifestSession.setManifestFilename(filename);

            String success = String.format(PACKAGE_LINKED, packageId, filename);
            messages.addInfo(success);
            // Unquarantines the package, relying on the above validation.
            boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                    Quarantined.ItemType.PACKAGE, packageId);
            if (wasQuarantined) {
                messages.addInfo(String.format(UNQUARANTINED, packageId));
                // Adds comment to the existing RCT.
                addRctComment(manifestSession.getReceiptTicket(), messages, success);
            }
        }
    }

    /**
     * Validates that the manifest file is compatible with the existing manifest session and returns the
     * manifest records from the file if no parsing or other errors were found.
     */
    private List<ManifestRecord> extractManifestRecords(String packageId, Collection<String> rackBarcodes,
            String filename, MessageCollection messages) {
        // Parses the the spreadsheet into ManifestRecords.
        List<List<String>> cellGrid = readManifestFileCellGrid(filename, messages);
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, filename, messages);
        if (records.isEmpty()) {
            messages.addError(INVALID_MANIFEST, filename);
        } else if (!packageId.equals(records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
            messages.addError(NOT_IN_MANIFEST, "Package ID", packageId);
        } else {
            // The spreadsheet's rack barcodes must match the previously entered rack barcodes.
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
        return messages.hasErrors() ? Collections.emptyList() : records;
    }

    /**
     * Errors if there are vessel or position changes in the new manifest records.
     * If no errors found, returns the updates to existing MercurySample metadata.
     */
    private Map<MercurySample, Set<Metadata>> extractMetadataChanges(ManifestSession manifestSession,
            List<ManifestRecord> updateRecords, MessageCollection messages) {
        Map<MercurySample, Set<Metadata>> metadataDiffs = new HashMap<>();

        // Maps the updated records' sample name to sample metadata.
        Multimap<String, Metadata> newMetadataRecords = HashMultimap.create();
        for (ManifestRecord manifestRecord : updateRecords) {
            // Sample name is the tube barcode.
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            newMetadataRecords.putAll(sampleName, manifestRecord.getMetadata());
        }

        // Maps the tube to rack and position for old records and new records.
        Map<String, String> oldTubeMap = manifestSession.getRecords().stream().
                collect(Collectors.toMap(
                        record -> record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE),
                        record -> record.getValueByKey(Metadata.Key.BOX_ID) +
                                record.getValueByKey(Metadata.Key.WELL_POSITION)));

        Map<String, String> newTubeMap = updateRecords.stream().
                collect(Collectors.toMap(
                        record -> record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE),
                        record -> record.getValueByKey(Metadata.Key.BOX_ID) +
                                record.getValueByKey(Metadata.Key.WELL_POSITION)));

        // For the accessioned samples, the new records must have identical combinations of rack, position, and tube.
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(newMetadataRecords.keySet());
        String nonMetadataChanges = sampleMap.values().stream().
                filter(mercurySample -> mercurySample != null).
                map(MercurySample::getSampleKey).  // Tube barcode is the sample name.
                filter(tubeBarcode -> !Objects.equal(oldTubeMap.get(tubeBarcode), newTubeMap.get(tubeBarcode))).
                sorted().
                collect(Collectors.joining(" "));

        if (StringUtils.isNotBlank(nonMetadataChanges)) {
            messages.addError(ONLY_METADATA_CHANGES, nonMetadataChanges);
        } else {
            // Collects any metadata differences found for each accessioned sample.
            for (MercurySample mercurySample : sampleMap.values()) {
                if (mercurySample != null) {
                    Map<Metadata.Key, Metadata> existingData = mercurySample.getMetadata().stream().
                            collect(Collectors.toMap(Metadata::getKey, Function.identity()));

                    Set<Metadata> diffs = newMetadataRecords.get(mercurySample.getSampleKey()).stream().
                            filter(newData -> !newData.getValue().equals(existingData.get(newData.getKey()))).
                            collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(diffs)) {
                        metadataDiffs.put(mercurySample, diffs);
                    }
                }
            }
        }
        return metadataDiffs;
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


    /** Makes a new RCT in Jira for the Mayo package. */
    private JiraTicket makePackageRct(MayoPackageReceiptActionBean bean, @NotNull  ManifestSession manifestSession,
            List<String> comments) {
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
                // If a reference to a previous RCT exists, updates the RCT. There won't be a reference if a
                // previous RCT was made without a manifest. That RCT just gets abandoned and a new one is made.
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
