package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestFile;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestFile_;
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
import org.jetbrains.annotations.Nullable;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
@Stateful
public class MayoManifestEjb {
    private static Log logger = LogFactory.getLog(MayoManifestEjb.class);
    public static final String ACCESSIONED = "Accessioned rack %s with %s samples %s.";
    public static final String ALREADY_ACCESSIONED = "Rack %s is already accessioned.";
    public static final String ALREADY_ACCESSIONED_SAMPLES = "Samples %s are already accessioned.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String ALREADY_LINKED = "Package %s is already linked to a valid manifest.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String ALREADY_RECEIVED = "Package %s is already received.";
    public static final String INVALID_FILENAME = "File '%s' has non-7-bit ascii filename and cannot be processed.";
    public static final String INVALID_MANIFEST = "File %s does not contain a manifest.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String MANIFEST_CREATED = "Created manifest from file %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String MISSING_MANIFEST = "Cannot find a manifest for %s.";
    public static final String NO_SUCH_FILE = "Cannot find file %s.";
    public static final String NO_SAMPLES_UPDATED = "No sample metadata changes were found; is the filename correct?";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String NOT_ACCESSIONED = "Update manifest samples have not been accessioned: %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String NOT_IN_MANIFEST = "Manifest does not contain %s %s.";
    public static final String NOT_IN_RACK_LIST = "Rack barcode input does not include manifest rack(s) %s.";
    public static final String NOT_LINKED = "Package %s is not linked to a valid manifest.";
    public static final String NOT_RECEIVED = "Package %s has not been received.";
    public static final String PACKAGE_LINKED = "Package %s is linked to manifest from file %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String QUARANTINED = "%s is quarantined due to: %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String RECEIVED =
            "Package %s is received and <a id='rctTicketUrl' href=\"%s\">RCT ticket</a> was created.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String SAMPLES_NOT_UPDATED = "No metadata changes found for samples: %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String SAMPLES_UPDATED = "Metadata was updated for samples: %s.";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String SKIPPING_RECEIVED = "Ignoring file %s because " + ALREADY_RECEIVED;
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String SKIPPING_LINKED = "Ignoring file %s because " + ALREADY_LINKED;
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String UNQUARANTINED = "%s was unquarantined.";
    public static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest shows %s.";

    public static final Map<String, CustomFieldDefinition> JIRA_DEFINITION_MAP =
            new HashMap<String, CustomFieldDefinition>() {{
                put("MaterialTypeCounts", new CustomFieldDefinition("customfield_15660", "Material Type Counts", true));
                put("Samples", new CustomFieldDefinition("customfield_12662", "Sample IDs", false));
                put("ShipmentCondition", new CustomFieldDefinition("customfield_15661", "Shipment Condition", false));
                put("TrackingNumber", new CustomFieldDefinition("customfield_15663", "Tracking Number", false));
                put("KitDeliveryMethod", new CustomFieldDefinition("customfield_13767", "Kit Delivery Method", false));
                put("RequestingPhysician", new CustomFieldDefinition("customfield_16163", "Requesting Physician", false));
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
    private MayoManifestConfig mayoManifestConfig;
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
        this.mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    /**
     * Finds the ManifestSession for the package id, either an existing one for the package, or
     * makes a new one from a manifest file in the storage bucket.
     *
     * @return boolean indicating if the package has already been received.
     */
    public boolean packageReceiptLookup(MayoPackageReceiptActionBean bean) {
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(bean.getPackageBarcode());
        if (manifestSession == null) {
            // If none exists, searches bucket storage for the manifest file to use.
            // First tries with a specific filename.
            String expectedFilename = bean.getPackageBarcode() + ".csv";
            manifestSession = processNewManifestFiles(bean.getMessageCollection(), expectedFilename,
                    bean.getRackBarcodes()).
                    stream().filter(session -> session != null).findFirst().orElse(null);
            if (manifestSession == null) {
                // If no luck, pulls in all new files and tries to make a ManifestSession for each.
                // Keeps the one ManifestSession that matches the package barcode.
                manifestSession = processNewManifestFiles(bean.getMessageCollection(), null, bean.getRackBarcodes()).
                        stream().
                        filter(session -> session.getSessionPrefix().equals(bean.getPackageBarcode())).
                        findFirst().orElse(null);
            }
        }
        boolean isAlreadyReceived = false;
        if (manifestSession == null) {
            // Generates a warning if the manifest is missing. The package can still be received.
            bean.getMessageCollection().addError(MISSING_MANIFEST, bean.getPackageBarcode());
        } else if (StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
            // Errors if the package has already been received.
            bean.getMessageCollection().addError(ALREADY_RECEIVED, bean.getPackageBarcode());
            isAlreadyReceived = true;
        } else {
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
            bean.setFilename(manifestSession.getManifestFile() == null ? null :
                    manifestSession.getManifestFile().getNamespaceFilename().getRight());
            bean.setManifestCellGrid(readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection()));
            Set<String> spreadsheetBarcodes = manifestSession.getRecords().stream().
                    map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.BOX_ID).getValue()).
                    collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(bean.getRackBarcodes()) && !spreadsheetBarcodes.isEmpty()) {
                // Generates errors if the manifest and the scanned rack barcodes do not match.
                String notInManifest = CollectionUtils.subtract(bean.getRackBarcodes(), spreadsheetBarcodes).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notInManifest)) {
                    bean.getMessageCollection().addError(NOT_IN_MANIFEST, "Box ID", notInManifest);
                }
                String notEntered = CollectionUtils.subtract(spreadsheetBarcodes, bean.getRackBarcodes()).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notEntered)) {
                    bean.getMessageCollection().addError(NOT_IN_RACK_LIST, notEntered);
                }
            }
        }
        return isAlreadyReceived;
    }

    /**
     * Links a received package to a manifest file.
     */
    public void linkPackageToManifest(MayoPackageReceiptActionBean bean) {
        // Puts up an error if the package has not been received yet.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(bean.getPackageBarcode());
        if (manifestSession == null) {
            bean.getMessageCollection().addError(NOT_RECEIVED, bean.getPackageBarcode());
        } else if (CollectionUtils.isNotEmpty(manifestSession.getRecords())) {
            bean.getMessageCollection().addError(ALREADY_LINKED, bean.getPackageBarcode());
        } else {
            // Parses the the spreadsheet into ManifestRecords.
            List<List<String>> cellGrid = readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection());
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, bean.getFilename(),
                    bean.getMessageCollection());
            // Errors if the manifest file cannot be used for the package.
            String packageId = manifestSession.getSessionPrefix();
            if (!packageId.equals(records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
                bean.getMessageCollection().addError(NOT_IN_MANIFEST, "Package ID", packageId);
            } else {
                // To avoid continual retries, every file processed should have its filename persisted,
                // even if there were errors parsing it. In this case the file is only for updating
                // and never becomes a new ManifestSession, but it should not be revisited.
                Collection<Object> newEntities = new ArrayList<>();
                ManifestFile manifestFile = findOrCreateManifestFile(bean.getFilename(), newEntities);
                manifestSessionDao.persistAll(newEntities);
                // Adds ManifestFile and ManifestRecords to the existing manifest session.
                manifestSession.addRecords(records);
                manifestSession.setManifestFile(manifestFile);

                // Generates error messages when the spreadsheet's rack barcodes
                // and the previously entered rack barcodes do not match.
                Set<String> enteredBarcodes = manifestSession.getRecords().stream().
                    map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.BOX_ID).getValue()).
                    collect(Collectors.toSet());
                Set<String> spreadsheetBarcodes = records.stream().
                        map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.BOX_ID).getValue()).
                        collect(Collectors.toSet());
                String notInSpreadsheet = CollectionUtils.subtract(enteredBarcodes, spreadsheetBarcodes).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notInSpreadsheet)) {
                    bean.getMessageCollection().addError(NOT_IN_MANIFEST, "previously entered rack(s)",
                            notInSpreadsheet);
                }
                String notEntered = CollectionUtils.subtract(spreadsheetBarcodes, enteredBarcodes).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(notEntered)) {
                    bean.getMessageCollection().addError(NOT_IN_RACK_LIST, notEntered);
                }
                if (bean.getMessageCollection().hasErrors()) {
                    // Updates the quarantine record for the package with a new reason.
                    quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE,
                            bean.getPackageBarcode(), Quarantined.RACK_BARCODE_MISMATCH);
                    // Adds comments to the existing RCT.
                    addRctComment(manifestSession.getReceiptTicket(), bean.getMessageCollection(),
                            String.format(PACKAGE_LINKED, bean.getPackageBarcode(), bean.getFilename()));
                    addRctComment(manifestSession.getReceiptTicket(), bean.getMessageCollection(),
                            String.format(QUARANTINED, bean.getPackageBarcode(), Quarantined.RACK_BARCODE_MISMATCH));
                } else {
                    // If successful, unquarantines the package.
                    boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                            Quarantined.ItemType.PACKAGE, bean.getPackageBarcode());
                    if (wasQuarantined) {
                        bean.getMessageCollection().addInfo(String.format(UNQUARANTINED, bean.getPackageBarcode()));
                        // Adds comment to the existing RCT.
                        addRctComment(manifestSession.getReceiptTicket(), bean.getMessageCollection(),
                                String.format(PACKAGE_LINKED, bean.getPackageBarcode(), bean.getFilename()));
                    }
                }
            }
        }
    }

    /**
     * Updates existing sample's metadata from the given manifest file.
     */
    public void updateMetadata(MayoPackageReceiptActionBean bean) {
        // Parses the the spreadsheet into ManifestRecords.
        List<List<String>> cellGrid = readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection());
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        List<ManifestRecord> updateRecords = processor.makeManifestRecords(cellGrid, bean.getFilename(),
                bean.getMessageCollection());
        if (bean.getMessageCollection().hasErrors()) {
            return;
        }
        // To avoid continual retries, every filename processed should be persisted, even if there were
        // errors parsing it. The file is only for updating and never becomes a new ManifestSession.
        Collection<Object> newEntities = new ArrayList<>();
        findOrCreateManifestFile(bean.getFilename(), newEntities);
        manifestSessionDao.persistAll(newEntities);

        // There must be an existing manifest session.
        String packageId = updateRecords.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue();
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(packageId);
        if (manifestSession == null) {
            bean.getMessageCollection().addError(NOT_RECEIVED, packageId);
            return;
        }
        // Collects the update data.
        Multimap<String, Metadata> metadataMap = HashMultimap.create();
        for (ManifestRecord manifestRecord : updateRecords) {
            // Sample name is the tube barcode.
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            metadataMap.putAll(sampleName, manifestRecord.getMetadata());
        }
        // All samples must have already been accessioned.
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(metadataMap.keySet());
        Collection<String> noSuchSamples = CollectionUtils.subtract(metadataMap.keySet(),
                sampleMap.entrySet().stream().
                        filter(mapEntry -> mapEntry.getValue() != null).
                        map(Map.Entry::getKey).
                        collect(Collectors.toSet()));
        if (!noSuchSamples.isEmpty()) {
            bean.getMessageCollection().addError(NOT_ACCESSIONED, noSuchSamples.stream().
                    sorted().collect(Collectors.joining(" ")));
            return;
        }
        // For each sample, updates any metadata changes.
        List<String> updatedSamples = new ArrayList<>();
        List<String> noChangesToSample = new ArrayList<>();
        for (MercurySample mercurySample : sampleMap.values()) {
            Set<Metadata> diffs = new HashSet<>();
            for (Metadata update : metadataMap.get(mercurySample.getSampleKey())) {
                Metadata.Key key = update.getKey();
                if (mercurySample.getMetadata().stream().
                        anyMatch(existing -> key.equals(existing.getKey()) &&
                                !existing.getValue().equals(update.getValue()))) {
                    diffs.add(update);
                }
            }
            if (diffs.isEmpty()) {
                noChangesToSample.add(mercurySample.getSampleKey());
            } else {
                updatedSamples.add(mercurySample.getSampleKey());
                mercurySample.updateMetadata(diffs);
            }
        }
        updatedSamples.sort(Comparator.naturalOrder());
        noChangesToSample.sort(Comparator.naturalOrder());
        String updatedSampleNames = StringUtils.join(updatedSamples, " ");
        if (updatedSamples.isEmpty()) {
            bean.getMessageCollection().addWarning(NO_SAMPLES_UPDATED);
        } else {
            bean.getMessageCollection().addInfo(SAMPLES_UPDATED, updatedSampleNames);
            if (!noChangesToSample.isEmpty()) {
                bean.getMessageCollection().addInfo(SAMPLES_NOT_UPDATED, StringUtils.join(noChangesToSample, " "));
            }
            // Adds comment to the existing RCT.
            addRctComment(manifestSession.getReceiptTicket(), bean.getMessageCollection(),
                    "Sample metadata was updated from manifest file " + bean.getFilename() +
                            " for samples " + updatedSampleNames);
        }
    }

    /**
     * Does a package receipt, with or without a linked manifest file.
     * Makes a RCT ticket for the package.
     */
    public void packageReceipt(MayoPackageReceiptActionBean bean) {
        // It's validated by the action bean, so this assert is for unit tests.
        assert (StringUtils.isNotBlank(bean.getPackageBarcode()));
        Collection<Object> newEntities = new ArrayList<>();
        ManifestSession manifestSession;
        if (bean.getManifestSessionId() == null) {
            // Makes an unlinked ManifestSession, i.e. it's not linked to a manifest file.
            manifestSession = new ManifestSession(null, bean.getPackageBarcode(),
                    userBean.getBspUser(), false, Collections.emptyList());
            manifestSession.setVesselLabels(new HashSet<>(bean.getRackBarcodes()));
            newEntities.add(manifestSession);
        } else {
            manifestSession = manifestSessionDao.find(bean.getManifestSessionId());
        }

        // Makes comments for the RCT including "quarantined" if there is no manifest.
        List<String> comments = new ArrayList<>();
        if (manifestSession.getManifestFile() == null) {
            comments.add(String.format(QUARANTINED, bean.getPackageBarcode(), Quarantined.MISSING_MANIFEST));
            quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.PACKAGE,
                    bean.getPackageBarcode(), Quarantined.MISSING_MANIFEST);
            bean.getMessageCollection().addError(QUARANTINED, bean.getPackageBarcode(),
                    Quarantined.MISSING_MANIFEST);
        } else {
            comments.add("Package " + bean.getPackageBarcode() + " is received and linked to manifest " +
                    manifestSession.getManifestFile().getNamespaceFilename().getRight());
        }
        // The individual racks can be separately quarantined by the lab user.
        // Currently these are unquarantined by a successful accessioning.
        bean.getQuarantineBarcodeAndReason().forEach((barcode, reason) -> {
            if (StringUtils.isNotBlank(reason)) {
                quarantinedDao.addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK,
                        barcode, reason);
                bean.getMessageCollection().addInfo(QUARANTINED, barcode, reason);
                comments.add("Rack " + barcode + " is quarantined due to " + reason);
            }});
        JiraTicket jiraTicket = makePackageRct(bean, comments);
        if (jiraTicket != null) {
            newEntities.add(jiraTicket);
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            manifestSession.setReceiptTicket(jiraTicket.getTicketName());

            bean.getMessageCollection().addInfo(String.format(RECEIVED, bean.getPackageBarcode(),
                    jiraTicket.getBrowserUrl()));
        }
        labVesselDao.persistAll(newEntities);
    }

    /**
     * Validates the rack scan. Neither the rack vessel nor the tubes are
     * expected to exist yet, so it's an error if they do.
     */
    public void validateForAccessioning(MayoSampleReceiptActionBean bean) {
        // The action bean checks that the rack and scan are not empty. These asserts are for unit tests.
        assert(bean.getRackScan() != null && bean.getRackScan().size() > 0);
        assert(StringUtils.isNotBlank(bean.getRackBarcode()));
        List<String> barcodes = new ArrayList<>();
        // Turns the rack scan into a list of position & sample to pass to and from jsp.
        bean.getRackScanEntries().clear();
        bean.getRackScan().forEach((key, value) -> {
            if (StringUtils.isNotBlank(value)) {
                barcodes.add(value);
                bean.getRackScanEntries().add(StringUtils.join(key, " ", value));
            }
        });
        assert(!barcodes.isEmpty());

        // Rack barcode must be linked to a ManifestSession of a received Mayo package.
        ManifestSession manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
        if (manifestSession == null) {
            bean.getMessageCollection().addError(NOT_RECEIVED, "containing " + bean.getRackBarcode());
        } else if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            bean.getMessageCollection().addError(NOT_LINKED, manifestSession.getSessionPrefix());
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
            bean.getMessageCollection().addError(ALREADY_ACCESSIONED, existingVessels);
        }

        // Rack scan positions must all be valid for the rack type.
        String invalidPositions = CollectionUtils.subtract(
                bean.getRackScan().keySet(),
                Stream.of(DEFAULT_RACK_TYPE.getVesselGeometry().getVesselPositions()).
                        map(VesselPosition::name).
                        collect(Collectors.toList())).
                stream().sorted().collect(Collectors.joining(" "));
        if (!invalidPositions.isEmpty()) {
            bean.getMessageCollection().addError(UNKNOWN_WELL_SCAN, invalidPositions);
        }
    }

    /**
     * Accessions rack, tubes, samples provided the manifest is found and data matches up.
     */
    public void accession(MayoSampleReceiptActionBean bean) {
        // Finds the existing manifest session.
        ManifestSession manifestSession;
        if (bean.getManifestSessionId() == null) {
            // Finds the manifest session. It must have manifest records from a manifest file.
            manifestSession = manifestSessionDao.getSessionByVesselLabel(bean.getRackBarcode());
            if (manifestSession == null) {
                bean.getMessageCollection().addError(NOT_RECEIVED, "containing " + bean.getRackBarcode());
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
            bean.getMessageCollection().addError(NOT_LINKED, manifestSession.getSessionPrefix());
            return;
        }
        // Finds the records for the rack.
        List<ManifestRecord> manifestRecords =
                manifestSession.findRecordsByKey(bean.getRackBarcode(), Metadata.Key.BOX_ID);
        if (CollectionUtils.isEmpty(manifestRecords)) {
            bean.getMessageCollection().addError(NOT_IN_MANIFEST, "Box ID", bean.getRackBarcode());
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
            bean.getMessageCollection().addError(WRONG_TUBE_IN_POSITION, position,
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
            bean.getMessageCollection().addError(ALREADY_ACCESSIONED_SAMPLES, accessionedSamples);
        }

        if (bean.getMessageCollection().hasErrors()) {
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

        bean.getMessageCollection().addInfo(String.format(ACCESSIONED, bean.getRackBarcode(),
                sampleMetadata.keySet().size(),
                sampleMetadata.keySet().stream().sorted().collect(Collectors.joining(" "))));

        boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                Quarantined.ItemType.RACK, bean.getRackBarcode());
        if (wasQuarantined) {
            bean.getMessageCollection().addInfo(String.format(UNQUARANTINED, bean.getRackBarcode()));
        }

        // Adds comment to the existing RCT.
        if (addRctComment(manifestSession.getReceiptTicket(), bean.getMessageCollection(),
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
     * Changes the Google bucket access credential for Mercury's service account.
     * The new credential is written to the credential file that is configured in yaml.
     */
    public void rotateServiceAccountKey(MayoAdminActionBean bean) {
        googleBucketDao.rotateServiceAccountKey(bean.getMessageCollection());
    }

    /**
     * Finds bucket filenames that have been processed but were not made into manifest sessions
     * and puts the list into the bean.
     */
    public void getFailedFiles(MayoAdminActionBean bean) {
        bean.setFailedFilesList(manifestSessionDao.getFailedFilenames(mayoManifestConfig.getBucketName()).
                stream().
                map(qualifiedFilename -> ManifestFile.extractFilename(qualifiedFilename)).
                collect(Collectors.toList()));
    }

    /**
     * Reads the new manifest files from storage and makes new manifests
     * when the package has not been received yet.
     */
    public void pullAll(MayoAdminActionBean bean) {
        processNewManifestFiles(bean.getMessageCollection(), null, null).forEach(session ->
                bean.getMessageCollection().addInfo(MANIFEST_CREATED,
                        session.getManifestFile().getNamespaceFilename().getRight()));
    }

    /**
     * Reads the manifest file given by the bean filename and makes a new manifest
     * if the package has not been received yet.
     */
    public void pullOne(MayoAdminActionBean bean) {
        processNewManifestFiles(bean.getMessageCollection(), bean.getFilename(), null).forEach(session ->
                bean.getMessageCollection().addInfo(MANIFEST_CREATED,
                        session.getManifestFile().getNamespaceFilename().getRight()));
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * True errors are put in the MessageCollection.
     */
    private @NotNull List<List<String>> readManifestFileCellGrid(String filename, MessageCollection messages) {
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

    /**
     * Reads manifest file storage and persists files as new ManifestSessions, provided the package isn't received.
     */
    private List<ManifestSession> processNewManifestFiles(MessageCollection messages,
            @Nullable String loadOneFilename, @Nullable List<String> rackBarcodes) {
        List<ManifestSession> manifestSessions = new ArrayList<>();

        // Makes the list of filenames that need to be processed, excluding the filenames already handled.
        List<String> filenames = new ArrayList<>();
        if (StringUtils.isBlank(loadOneFilename)) {
            filenames.addAll(googleBucketDao.list(messages));
            filenames.sort(Comparator.naturalOrder());
            filenames.removeAll(manifestSessionDao.getFilenamesForNamespace(mayoManifestConfig.getBucketName()));
        } else if (googleBucketDao.exists(loadOneFilename, messages)) {
            // Processes just the specified filename and only if the file exists.
            filenames.add(loadOneFilename);
        }
        // Makes the manifest sessions.
        Set<Object> newEntities = new HashSet<>();
        for (String filename : filenames) {
            ManifestSession manifestSession = createManifestSession(filename, messages, rackBarcodes, newEntities);
            if (manifestSession != null) {
                manifestSessions.add(manifestSession);
            }
        }
        // ManifestFile and ManifestSession entities made from files are persisted here because
        // they are valid regardless of any further receiving or accessioning.
        if (!newEntities.isEmpty()) {
            manifestSessionDao.persistAll(newEntities);
            manifestSessionDao.flush();
        }
        return manifestSessions;
    }

    /**
     * Makes a new ManifestSession from the file for packages that haven't been received yet.
     */
    private @Nullable ManifestSession createManifestSession(String filename, MessageCollection messages,
            @Nullable List<String> rackBarcodes, Set<Object> newEntities) {

        // Only process filenames that can be stored in the database without substitution characters.
        if (!MayoManifestImportProcessor.cleanupValue(filename).equals(filename)) {
            messages.addWarning(String.format(INVALID_FILENAME, filename));
            logger.warn(String.format(INVALID_FILENAME, filename));

        } else {
            // To avoid continual retries, every file processed should have its filename persisted,
            // even if there were errors parsing it, or it's only for updating or linking and never
            // becomes a new ManifestSession.
            ManifestFile manifestFile = findOrCreateManifestFile(filename, newEntities);

            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            // Ignores earlier errors in the message collection (from other manifest files).
            int oldErrorCount = messages.getErrors().size();
            List<List<String>> cellGrid = readManifestFileCellGrid(filename, messages);
            List<ManifestRecord> records =
                    processor.makeManifestRecords(cellGrid, filename, messages);
            int parsingErrorCount = messages.getErrors().size() - oldErrorCount;

            if (parsingErrorCount == 0) {
                if (records.isEmpty()) {
                    messages.addError(INVALID_MANIFEST, filename);
                } else {
                    String packageId = records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue();

                    // If a manifestSession for the package already exists, only makes a new manifestSession
                    // if the package hasn't been received yet.
                    ManifestSession oldSession = manifestSessionDao.getSessionByPrefix(packageId);
                    // A package receipt is indicated by a receipt ticket on the manifestSession.
                    if (oldSession != null && StringUtils.isNotBlank(oldSession.getReceiptTicket())) {

                        // If the old session is valid (has manifest records) then the lab user can only
                        // Update Sample Metadata. If more than that is needed then a data fixup is needed.
                        // If the old session was a quarantined package with no manifest records, the
                        // lab user should use Link Package to Manifest.
                        messages.addWarning(String.format(
                                oldSession.getRecords().isEmpty() ? SKIPPING_RECEIVED : SKIPPING_LINKED,
                                filename, packageId));
                    } else {
                        ManifestSession manifestSession = new ManifestSession(null, packageId, userBean.getBspUser(),
                                false, records);
                        manifestSession.setManifestFile(manifestFile);

                        // ManifestSession vesselLabels will hold the rack barcodes that the lab user entered
                        // during the package receipt. It's not the rack barcodes from the spreadsheet.
                        if (CollectionUtils.isNotEmpty(rackBarcodes)) {
                            manifestSession.setVesselLabels(new HashSet<>(rackBarcodes));
                        }
                        newEntities.add(manifestSession);
                        return manifestSession;
                    }
                }
            }
        }
        return null;
    }

    /** Finds or creates a new ManifestFile. */
    private ManifestFile findOrCreateManifestFile(String filename, Collection<Object> newEntities) {
        String qualifiedFilename = ManifestFile.qualifiedFilename(mayoManifestConfig.getBucketName(), filename);
        // ManifestFile entities should be reused for the same filename.
        ManifestFile manifestFile = manifestSessionDao.findSingle(ManifestFile.class,
                ManifestFile_.qualifiedFilename, qualifiedFilename);
        if (manifestFile == null) {
            manifestFile = new ManifestFile(qualifiedFilename);
            newEntities.add(manifestFile);
        }
        return manifestFile;
    }

    /** Makes a new RCT in Jira for the Mayo package. */
    private JiraTicket makePackageRct(MayoPackageReceiptActionBean bean, List<String> comments) {
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
            // todo emp change to "Racks" when Jira field is added.
            add(new CustomField(JIRA_DEFINITION_MAP.get("Samples"), bean.getRackCount() + " Racks:\n" +
                    bean.getRackBarcodeString()));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"), " "));
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), " "));
        }};

        JiraTicket jiraTicket = null;
        JiraIssue jiraIssue;
        try {
            ManifestSession manifestSession = bean.getManifestSessionId() == null ? null :
                    manifestSessionDao.find(bean.getManifestSessionId());
            if (manifestSession != null && StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
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
