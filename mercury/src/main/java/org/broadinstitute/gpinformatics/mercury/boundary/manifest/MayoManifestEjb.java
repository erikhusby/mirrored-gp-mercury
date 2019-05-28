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
    public static final String ALREADY_ACCESSIONED = "%s has already been accessioned.";
    public static final String ALREADY_LINKED = "%s has already been linked with a manifest file.";
    public static final String ALREADY_RECEIVED = "%s has already been received.";
    public static final String FILES_PROCESSED = "%d manifest files processed: %s.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String MANIFEST_CREATED = "Created manifest for %s.";
    public static final String MANIFEST_CREATE_ITEM = "rack %s (%d tubes)";
    public static final String MISSING_MANIFEST = "Cannot find a manifest file for %s.";
    public static final String NOT_A_MANIFEST_FILE = "%s cannot be processed as a manifest file.";
    public static final String NOT_IN_MANIFEST = "Manifest does not contain %s %s.";
    public static final String NOT_IN_RACK_LIST = "List of rack barcodes is missing manifest rack(s) %s.";
    public static final String NOT_LINKED = "%s has not been linked to a manifest file.";
    public static final String NOT_YET_RECEIVED = "%s has not been received yet.";
    public static final String QUARANTINED = "%s has been quarantined due to %s.";
    public static final String RECEIVED = "%s has been received and an <a href=\"%s\">RCT ticket</a> has been created.";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String UNQUARANTINED = "%s was unquarantined.";
    public static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest has %s.";

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
     * Does a manifest lookup for a package receipt, or links an existing package to a manifest.
     * @return true if receipt can continue; false if errors prevent continuing.
     */
    public boolean packageLookupOrLinkup(MayoPackageReceiptActionBean bean) {
        // The bean is validated by the action bean so these asserts are for unit tests.
        assert(StringUtils.isNotBlank(bean.getPackageBarcode()));
        assert(CollectionUtils.isNotEmpty(bean.getRackBarcodes()) || StringUtils.isNotBlank(bean.getFilename()));

        if (bean.getRackBarcodes().isEmpty()) {
            // Updates the ManifestSession using the specified manifest file and does validation.
            return linkPackageToManifest(bean);
        } else {
            // Looks up a manifest for the package id and does validation.
            return packageReceiptLookup(bean);
        }
    }

    /**
     * Finds the most recent ManifestSession for the package id.
     * Reads files from storage as necessary and makes Manifest Sessions from any new ones.
     * @return true if receipt can continue; false if errors prevent continuing.
     */
    private boolean packageReceiptLookup(MayoPackageReceiptActionBean bean) {
        // Picks the most recent of the existing manifests for the packageId.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(bean.getPackageBarcode());
        if (manifestSession == null) {
            // If none found, searches bucket storage for the manifest file. First tries with a specific
            // filename, and if no luck, pulls in all new files and makes a ManifestSession for each.
            manifestSession = processNewManifestFiles(bean.getPackageBarcode() + ".csv",
                    bean.getPackageBarcode(), bean.getMessageCollection());
            if (manifestSession == null) {
                manifestSession = processNewManifestFiles(null, bean.getPackageBarcode(),
                        bean.getMessageCollection());
            }
        }
        boolean canProceed = true;
        if (manifestSession == null) {
            // Generates a warning if the manifest is missing. The package can still be received.
            bean.getMessageCollection().addError(MISSING_MANIFEST, bean.getPackageBarcode());
        } else if (CollectionUtils.isNotEmpty(manifestSession.getRecords()) && !bean.getPackageBarcode().equals(
                manifestSession.getRecords().get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
            // Errors if the packageId doesn't match the manifest content.
            bean.getMessageCollection().addError(NOT_IN_MANIFEST, "packageId", bean.getPackageBarcode());
            canProceed = false;
        } else if (StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
            // Errors if the package has already been received.
            bean.getMessageCollection().addError(ALREADY_RECEIVED, bean.getPackageBarcode());
            canProceed = false;
        } else {
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
            bean.setFilename(manifestSession.getManifestFile() == null ? null :
                    manifestSession.getManifestFile().getNamespaceFilename().getRight());
            bean.setManifestCellGrid(readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection()));
            if (CollectionUtils.isNotEmpty(bean.getRackBarcodes())) {
                // Generates errors if the manifest and the scanned rack barcodes do not match.
                Set<String> manifestBarcodes = manifestSession.getVesselLabels();
                String unexpectedRacks = CollectionUtils.subtract(bean.getRackBarcodes(), manifestBarcodes).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(unexpectedRacks)) {
                    bean.getMessageCollection().addError(NOT_IN_MANIFEST, "rack(s)", unexpectedRacks);
                }
                String missingRacks = CollectionUtils.subtract(manifestBarcodes, bean.getRackBarcodes()).stream().
                        sorted().collect(Collectors.joining(" "));
                if (StringUtils.isNotBlank(missingRacks)) {
                    bean.getMessageCollection().addError(NOT_IN_RACK_LIST, missingRacks);
                }
            }
        }
        return canProceed;
    }

    /**
     * Links a received package to a manifest file.
     * @return true if receipt can continue; false if errors prevent continuing.
     */
    private boolean linkPackageToManifest(MayoPackageReceiptActionBean bean) {
        boolean canContinue = true;
        // Puts up an error if the package has not been received yet.
        ManifestSession manifestSession = manifestSessionDao.getSessionByPrefix(bean.getPackageBarcode());
        if (manifestSession == null) {
            bean.getMessageCollection().addError(NOT_YET_RECEIVED, bean.getPackageBarcode());
            canContinue = false;
        } else if (CollectionUtils.isNotEmpty(manifestSession.getRecords())) {
            bean.getMessageCollection().addError(ALREADY_LINKED, bean.getPackageBarcode());
            canContinue = false;
        } else {
            // Parses the the spreadsheet into ManifestRecords.
            List<List<String>> cellGrid = readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection());
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, bean.getFilename(),
                    bean.getMessageCollection());
            // Errors if the manifest file cannot be used for the package.
            String packageId = manifestSession.getSessionPrefix();
            if (!packageId.equals(records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue())) {
                bean.getMessageCollection().addError(NOT_IN_MANIFEST, "packageId", packageId);
                canContinue = false;
            } else {
                // To avoid continual retries, every filename processed should be persisted, even if there were
                // errors parsing it. The file is only for updating and never becomes a new ManifestSession.
                Collection<Object> newEntities = new ArrayList<>();
                ManifestFile manifestFile = findOrCreateManifestFile(bean.getFilename(), newEntities);
                manifestSessionDao.persistAll(newEntities);
                // Adds ManifestFile and ManifestRecords to the existing manifest session.
                manifestSession.addRecords(records);
                manifestSession.setManifestFile(manifestFile);

                // If successful, unquarantines the ManifestSession.
                boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                        Quarantined.ItemType.PACKAGE, bean.getPackageBarcode());
                if (wasQuarantined) {
                    bean.getMessageCollection().addInfo(String.format(UNQUARANTINED, bean.getPackageBarcode()));
                }
            }
        }
        return canContinue;
    }

    /**
     * Does a package receipt, either with or without a linked manifest file.
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

        // Makes comments for the RCT. Marks the package "quarantined" if there is no manifest.
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
        // Currently these are unquarantined by either a successful accessioning.
        if (CollectionUtils.isNotEmpty(bean.getQuarantineReasons())) {
            for (int i = 0; i < bean.getQuarantineBarcodes().size(); ++i) {
                String rackBarcode = bean.getQuarantineBarcodes().get(i);
                String reason = bean.getQuarantineReasons().get(i);
                if (StringUtils.isNotBlank(reason)) {
                    quarantinedDao
                            .addOrUpdate(Quarantined.ItemSource.MAYO, Quarantined.ItemType.RACK, rackBarcode, reason);
                    comments.add("Rack " + rackBarcode + " is quarantined due to " + reason);
                }
            }
        }
        List<String> materialTypes = manifestSession.getRecords().stream(). map(manifestRecord ->
                StringUtils.defaultIfBlank(manifestRecord.getValueByKey(Metadata.Key.MATERIAL_TYPE), "unspecified")).
                collect(Collectors.toList());

        JiraTicket jiraTicket = makePackageRct(bean, comments, materialTypes);
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
            bean.getMessageCollection().addError(NOT_YET_RECEIVED, "Package containing " + bean.getRackBarcode());
        } else if (CollectionUtils.isEmpty(manifestSession.getRecords())) {
            bean.getMessageCollection().addError(NOT_LINKED, manifestSession.getSessionPrefix());
        } else {
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
        }

        // Disallows vessels that were already accessioned.
        barcodes.add(0, bean.getRackBarcode());
        String existingVessels = labVesselDao.findByBarcodes(barcodes).entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                map(mapEntry -> mapEntry.getKey()).
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
                bean.getMessageCollection().addError(NOT_YET_RECEIVED, "Package containing " + bean.getRackBarcode());
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
            bean.getMessageCollection().addError(NOT_IN_MANIFEST, "box id", bean.getRackBarcode());
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
            bean.getMessageCollection().addError(ALREADY_ACCESSIONED, "samples " + accessionedSamples);
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

        bean.getMessageCollection().addInfo(String.format(ACCESSIONED, bean.getRackBarcode(), sampleMetadata.size(),
                sampleMetadata.keySet().stream().sorted().collect(Collectors.joining(" "))));

        boolean wasQuarantined = quarantinedDao.unQuarantine(Quarantined.ItemSource.MAYO,
                Quarantined.ItemType.RACK, bean.getRackBarcode());
        if (wasQuarantined) {
            bean.getMessageCollection().addInfo(String.format(UNQUARANTINED, bean.getRackBarcode()));
        }

        // Adds comments to the existing RCT
        if (addAccessionComment(manifestSession.getReceiptTicket(), bean, manifestPositionToTube.values())) {
            // Doesn't persist any entities if Jira fails.
            labVesselDao.persistAll(newEntities);
        }
    }

    /**
     * Updates sample metadata from the given manifest file for samples in tubes in the given rack.
     * The manifest file is not made into a ManifestSession and is expected to only contain the "diffs"
     * i.e. only the samples needing changes and the new metadata values.
     */
    public void updateSampleMetadata(MayoSampleReceiptActionBean bean) {
        // The action bean checks for non-blank filename and rack barcode. These asserts are for unit tests.
        assert(StringUtils.isNotBlank(bean.getFilename()));
        assert(StringUtils.isNotBlank(bean.getRackBarcode()));
        // todo emp
        bean.getMessageCollection().addError("UNIMPLEMENTED");
        // Parses the manifest session spreadsheet into one sample per row.
        // Updates tube volume, conc, mass.
        // Updates sample metadata.
        // Adds comment to the RCT ticket from the manifest session for the rack.
    }

    /**
     * Obtains the manifest file in a cell grid for the filename given.
     */
    public void readManifestFileCellGrid(MayoSampleReceiptActionBean bean) {
        bean.setManifestCellGrid(readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection()));
    }

    /** Generates info messages. Puts a listing of all bucket filenames in the bean. */
    public void testAccess(MayoSampleReceiptActionBean bean) {
        bean.getBucketList().clear();
        bean.setBucketList(googleBucketDao.test(bean.getMessageCollection()));
    }

    public void rotateServiceAccountKey(MayoSampleReceiptActionBean bean) {
        googleBucketDao.rotateServiceAccountKey(bean.getMessageCollection());
    }

    /**
     * Finds bucket filenames that have been processed but were not made into manifest sessions
     * and puts the list into the bean.
     */
    public void getFailedFiles(MayoSampleReceiptActionBean bean) {
        bean.setFailedFilesList(manifestSessionDao.getFailedFilenames(mayoManifestConfig.getBucketName()));
    }

    public void pullAll(MayoSampleReceiptActionBean bean) {
        processNewManifestFiles(null, null, bean.getMessageCollection());
    }

    /** Reads the manifest file given in the bean.filename and makes a new manifest if values have changed. */
    public void pullOne(MayoSampleReceiptActionBean bean) {
        processNewManifestFiles(bean.getFilename(), null, bean.getMessageCollection());
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * True errors are put in the MessageCollection.
     */
    public @NotNull List<List<String>> readManifestFileCellGrid(String filename, MessageCollection messages) {
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
     * Reads manifest file storage and persists files as new ManifestSessions.
     *
     * @param forceReloadFilename if non-blank forces reload of the one file only. If blank, all new files are read.
     * @param packageId specifies which manifest session to return.
     */
    private ManifestSession processNewManifestFiles(@Nullable String forceReloadFilename,
            @Nullable String packageId, MessageCollection messages) {

        List<String> filenames;
        if (StringUtils.isBlank(forceReloadFilename)) {
            // Makes the list of filenames that need to be processed, excluding the filenames already handled.
            filenames = googleBucketDao.list(messages);
            filenames.sort(Comparator.naturalOrder());
            filenames.removeAll(manifestSessionDao.getFilenamesForNamespace(mayoManifestConfig.getBucketName()));
        } else if (googleBucketDao.exists(forceReloadFilename, messages)) {
            filenames = Collections.singletonList(forceReloadFilename);
        } else {
            return null;
        }

        // Creates new session for each file.
        Set<Object> newEntities = new HashSet<>();
        List<ManifestSession> manifestSessions = new ArrayList<>();
        for (String filename : filenames) {
            // Only process clean filenames to avoid endless ManifestFile entities being created none
            // of which would ever match up with the unclean filename.
            if (MayoManifestImportProcessor.cleanupValue(filename).equals(filename)) {
                // Uses local message collection so one file can error and it won't stop others from loading.
                MessageCollection localMessages = new MessageCollection();
                manifestSessions.add(createManifestSession(filename, localMessages, newEntities));
                messages.addErrors(localMessages.getErrors());
                messages.addWarning(localMessages.getWarnings());
                messages.addInfos(localMessages.getInfos());
            } else {
                logger.warn("Skipping Google bucket file '%s' because the name contains non-7-bit ascii chars.");
            }
        }
        // ManifestFile and ManifestSession entities made from files are persisted here because
        // they are valid regardless of any further receiving or accessioning.
        if (!newEntities.isEmpty()) {
            manifestSessionDao.persistAll(newEntities);
            manifestSessionDao.flush();
        }
        // Puts up info and error messages.
        if (!filenames.isEmpty()) {
            messages.addInfo(String.format(FILES_PROCESSED, filenames.size(), StringUtils.join(filenames, ", ")));
        }
        if (!manifestSessions.isEmpty()) {
            messages.addInfo(String.format(MANIFEST_CREATED, manifestSessions.stream().
                    map(session -> String.format(MANIFEST_CREATE_ITEM,
                            session.getSessionPrefix(), session.getRecords().size())).
                    collect(Collectors.joining(", "))));
        } else if (StringUtils.isNotBlank(forceReloadFilename)) {
            messages.addError(String.format(NOT_A_MANIFEST_FILE, forceReloadFilename));
        }
        // Returns the manifestSession that matches the packageId, or null if none match.
        return manifestSessions.stream().
                filter(session -> session.getSessionPrefix().equals(packageId)).
                findFirst().orElse(null);
    }

    private ManifestSession createManifestSession(String filename, MessageCollection messages,
            Set<Object> newEntities) {
        // To avoid continual retries, every filename processed should be persisted,
        // even if there were errors parsing it and doesn't become a ManifestSession.
        ManifestFile manifestFile = findOrCreateManifestFile(filename, newEntities);
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        List<ManifestRecord> records = processor.makeManifestRecords(
                readManifestFileCellGrid(filename, messages), filename, messages);
        ManifestSession manifestSession = null;
        if (!records.isEmpty() && !messages.hasErrors()) {
            String packageId = records.get(0).getMetadataByKey(Metadata.Key.PACKAGE_ID).getValue();
            Set<String> rackBarcodes = records.stream().
                    map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.BOX_ID).getValue()).collect(
                    Collectors.toSet());
            manifestSession = new ManifestSession(null, packageId, userBean.getBspUser(), false, records);
            manifestSession.setManifestFile(manifestFile);
            manifestSession.setVesselLabels(rackBarcodes);
            newEntities.add(manifestSession);
        }
        return manifestSession;
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
    private JiraTicket makePackageRct(MayoPackageReceiptActionBean bean, List<String> comments,
            List<String> materialTypes) {
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
            add(new CustomField(JIRA_DEFINITION_MAP.get("Samples"), bean.getRackCount() + " Racks: " +
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

    private boolean addAccessionComment(String rctId, MayoSampleReceiptActionBean bean, Collection<String> tubes) {
        JiraIssue jiraIssue = null;
        try {
            jiraIssue = jiraService.getIssue(rctId);
            if (jiraIssue == null) {
                bean.getMessageCollection().addError("Cannot find Jira ticket " + rctId);
            } else {
                jiraIssue.addComment(String.format(ACCESSIONED, bean.getRackBarcode(), tubes.size(),
                        tubes.stream().sorted().collect(Collectors.joining(" "))));
            }
        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
        }
        return jiraIssue != null;
    }

    public UserBean getUserBean() {
        return userBean;
    }
}
