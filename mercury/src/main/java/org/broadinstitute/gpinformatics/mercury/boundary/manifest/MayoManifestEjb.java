package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestFile;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestFile_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
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
import java.util.ArrayList;
import java.util.Arrays;
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
    public static final String ACCESSIONED = "Accessioned rack %s, samples %s.";
    public static final String ALREADY_ACCESSIONED = "Tubes have already been accessioned: %s.";
    public static final String ALREADY_ACCESSIONED_RACK = "The rack's tubes have already been accessioned: %s.";
    public static final String FILES_PROCESSED = "%d manifest files processed: %s.";
    public static final String INVALID = "Cannot find a %s for %s.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String MANIFEST_CREATED = "Created manifest for %s.";
    public static final String MANIFEST_CREATE_ITEM = "rack %s (%d tubes)";
    public static final String MULTIPLE_TUBE_FORMS = "Rack %s must only have one tube formation.";
    public static final String NOT_A_RACK = "Rack %s matches an existing vessel but it is not a rack.";
    public static final String NOT_A_TUBE = "Tube %s matches an existing vessel but it is not a tube.";
    public static final String OVERWRITING = "Previously uploaded rack %s will be overwritten.";
    public static final String RACK_NOT_IN_MANIFEST = "Manifest %s does not contain rack(s) %s.";
    public static final String REACCESSIONED = "Samples have been re-accessioned: %s";
    public static final String UNKNOWN_WELL = "Manifest contains unknown well position: %s.";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String WRONG_TUBE_IN_POSITION = "Rack position %s has %s but manifest has %s.";

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

    /**
     * CDI constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    public MayoManifestEjb() {
    }

    @Inject
    public MayoManifestEjb(ManifestSessionDao manifestSessionDao, GoogleBucketDao googleBucketDao, UserBean userBean,
            LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao, TubeFormationDao tubeFormationDao,
            JiraService jiraService, Deployment deployment) {
        this.manifestSessionDao = manifestSessionDao;
        this.googleBucketDao = googleBucketDao;
        this.userBean = userBean;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.tubeFormationDao = tubeFormationDao;
        this.jiraService = jiraService;
        // This config is from the yaml file.
        this.mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    /**
     * Validates the rack, rack scan, and tubes.
     */
    public void validateAndScan(MayoSampleReceiptActionBean bean) {
        // The action bean should have checked that the rack and scan are not empty.
        assert(StringUtils.isNotBlank(bean.getRackBarcode()));
        List<String> tubeBarcodes = bean.getRackScan().values().stream().
                filter(StringUtils::isNotBlank).collect(Collectors.toList());
        assert(!tubeBarcodes.isEmpty());

        // Verifies that if barcodes match an existing vessel, it must be a tube.
        List<LabVessel> tubes = labVesselDao.findByListIdentifiers(tubeBarcodes);
        String notTubes = tubes.stream().
                filter(labVessel -> !OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)).
                map(LabVessel::getLabel).collect(Collectors.joining(", "));
        if (!notTubes.isEmpty()) {
            bean.getMessageCollection().addError(NOT_A_TUBE, notTubes);
        }
        // Disallows a re-upload of tubes that were successful accessioned.
        String accessionedTubes = tubes.stream().
                filter(labVessel -> CollectionUtils.isNotEmpty(labVessel.getMercurySamples())).
                map(LabVessel::getLabel).
                collect(Collectors.joining(" "));
        if (!accessionedTubes.isEmpty()) {
            bean.getMessageCollection().addError(ALREADY_ACCESSIONED, accessionedTubes);
        }

        // Verifies that the rack barcode is for a rack.
        if (!bean.getMessageCollection().hasErrors()) {
            LabVessel rackVessel = labVesselDao.findByIdentifier(bean.getRackBarcode());
            if (rackVessel != null) {
                if (OrmUtil.proxySafeIsInstance(rackVessel, RackOfTubes.class)) {
                    RackOfTubes rack = OrmUtil.proxySafeCast(rackVessel, RackOfTubes.class);
                    // Disallows a re-upload of a rack if its tubes were successful accessioned.
                    if (rack != null) {
                        String racksAccessionedTubes = rack.getTubeFormations().stream().
                                flatMap(tubeFormation -> tubeFormation.getVesselContainers().stream()).
                                flatMap(vesselContainer -> vesselContainer.getContainedVessels().stream()).
                                filter(labVessel -> CollectionUtils.isNotEmpty(labVessel.getMercurySamples())).
                                map(LabVessel::getLabel).
                                collect(Collectors.joining(" "));
                        if (!racksAccessionedTubes.isEmpty()) {
                            bean.getMessageCollection().addError(ALREADY_ACCESSIONED_RACK, racksAccessionedTubes);
                        } else {
                            bean.getMessageCollection().addInfo(OVERWRITING, rackVessel.getLabel());
                        }
                    }
                } else {
                    bean.getMessageCollection().addError(NOT_A_RACK, bean.getRackBarcode());
                }
            }
        }
        // Checks that the rack scan positions are valid.
        String invalidPositions = bean.getRackScan().keySet().stream().
                filter(position -> VesselPosition.getByName(position) == null).
                collect(Collectors.joining(", "));
        if (StringUtils.isBlank(invalidPositions)) {
            // Turns the rack scan into a list of position & sample to pass to and from jsp.
            bean.getRackScanEntries().addAll(bean.getRackScan().entrySet().stream().
                    map(mapEntry -> StringUtils.join(mapEntry.getKey(), " ", mapEntry.getValue())).
                    sorted().collect(Collectors.toList()));
        } else {
            bean.getMessageCollection().addError(UNKNOWN_WELL_SCAN, invalidPositions);
        }
    }

    public void receive(MayoPackageReceiptActionBean bean) {
        // Generates a warning if the manifest is missing.
        ManifestSession manifestSession = bean.getManifestSessionId() == null ? null :
                manifestSessionDao.find(bean.getManifestSessionId());
        if (manifestSession == null) {
            bean.getMessageCollection().addWarning(String.format(MayoManifestEjb.INVALID, "manifest",
                    bean.getPackageBarcode()));
        } else {
            // A received package is represented only by an RCT ticket.
            // Generates warnings if the manifest and the scanned rack barcodes do not match.
            Set<String> rackBarcodes = new HashSet<>(Arrays.asList(StringUtils.split(bean.getRackBarcodeString())));
            Set<String> manifestBarcodes = manifestSession.getVesselLabels();
            String unexpectedRacks = CollectionUtils.subtract(rackBarcodes, manifestBarcodes).stream().
                    sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(unexpectedRacks)) {
                bean.getMessageCollection().addError(String.format(MayoManifestEjb.RACK_NOT_IN_MANIFEST,
                        manifestSession.getSessionName(), unexpectedRacks));
            }
            String missingRacks = CollectionUtils.subtract(manifestBarcodes, rackBarcodes).stream().
                    sorted().collect(Collectors.joining(" "));
            if (StringUtils.isNotBlank(missingRacks)) {
                bean.getMessageCollection().addError(String.format(MayoManifestEjb.INVALID, "manifest rack",
                        manifestBarcodes));
            }
        }
        JiraTicket jiraTicket = makePackageRct(bean);
        if (jiraTicket != null) {
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            manifestSession.setReceiptTicket(jiraTicket.getTicketName());
            labVesselDao.persist(jiraTicket);
        }
    }

    public void receive(MayoSampleReceiptActionBean bean) {
        RackOfTubes rack = null;
        Map<String, VesselPosition> tubeToPosition = bean.getRackScan().entrySet().stream().
                filter(mapEntry -> StringUtils.isNotBlank(mapEntry.getValue())).
                collect(Collectors.toMap(mapEntry -> mapEntry.getValue(),
                        mapEntry -> VesselPosition.valueOf(mapEntry.getKey())));
        Map<String, BarcodedTube> tubeMap = new HashMap<>();

        // Finds existing vessels.
        for (LabVessel vessel : labVesselDao.findListByList(LabVessel.class, LabVessel_.label,
                Stream.concat(tubeToPosition.keySet().stream(), Stream.of(bean.getRackBarcode())).
                        filter(StringUtils::isNotBlank).
                        collect(Collectors.toList()))) {
            if (vessel.getLabel().equals(bean.getRackBarcode()) &&
                    OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
                // Rack is being re-received, so clearing old tube formations is appropriate.
                rack.getTubeFormations().clear();
            } else if (OrmUtil.proxySafeIsInstance(vessel, BarcodedTube.class)) {
                tubeMap.put(vessel.getLabel(), OrmUtil.proxySafeCast(vessel, BarcodedTube.class));
            } else {
                bean.getMessageCollection().addError(vessel.getLabel() + " is neither RackOfTubes nor BarcodedTube.");
            }
        }

        // Creates or updates tubes.
        List<Object> newEntities = new ArrayList<>();
        Map<VesselPosition, BarcodedTube> vesselPositionToTube = new HashMap<>();
        for (String barcode : tubeToPosition.keySet()) {
            BarcodedTube tube = tubeMap.get(barcode);
            if (tube == null) {
                tube = new BarcodedTube(barcode, DEFAULT_TUBE_TYPE);
                newEntities.add(tube);
                tubeMap.put(barcode, tube);
            } else {
                // Tube is being re-uploaded, so clearing old samples is appropriate.
                tube.getMercurySamples().clear();
            }
            vesselPositionToTube.put(tubeToPosition.get(barcode), tube);
        }

        // Creates or updates the rack with a new tube formation created from the rackScan.
        List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
        for (String positionName : bean.getRackScan().keySet()) {
            positionBarcodeList.add(Pair.of(VesselPosition.getByName(positionName),
                    bean.getRackScan().get(positionName)));
        }
        String digest = TubeFormation.makeDigest(positionBarcodeList);
        TubeFormation tubeFormation = tubeFormationDao.findByDigest(digest);
        if (tubeFormation == null) {
            tubeFormation = new TubeFormation(vesselPositionToTube, DEFAULT_RACK_TYPE);
            newEntities.add(tubeFormation);
        }
        if (rack == null) {
            rack = new RackOfTubes(bean.getRackBarcode(), DEFAULT_RACK_TYPE);
            newEntities.add(rack);
        }
        tubeFormation.addRackOfTubes(rack);

        // Makes a receipt-only ticket.
        JiraTicket jiraTicket = makeRackRct(bean, rack, null, null, null, null, null);
        if (jiraTicket != null) {
            // Doesn't persist any entities if Jira fails.
            newEntities.add(jiraTicket);
            labVesselDao.persistAll(newEntities);
        }
    }

    /**
     * Accessions samples in the given rack if a manifest is found and data matches up.
     */
    public void accession(MayoSampleReceiptActionBean bean) {
        ManifestSession manifestSession = bean.getManifestSessionId() == null ? null :
                manifestSessionDao.find(bean.getManifestSessionId());
        if (manifestSession == null) {
            bean.getMessageCollection().addWarning(INVALID, "manifest", bean.getRackBarcode());
            return;
        }
        LabVessel vessel = bean.getRackBarcode() == null ? null : labVesselDao.findByIdentifier(bean.getRackBarcode());
        if (vessel == null) {
            bean.getMessageCollection().addError(INVALID, "received rack", bean.getRackBarcode());
            return;
        }
        RackOfTubes rack = OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class) ?
                OrmUtil.proxySafeCast(vessel, RackOfTubes.class) : null;
        if (rack == null) {
            bean.getMessageCollection().addError(NOT_A_RACK, bean.getRackBarcode());
            return;
        }
        // A received rack should have exactly one tube formation.
        if (CollectionUtils.isEmpty(rack.getTubeFormations())) {
            bean.getMessageCollection().addError(INVALID, "tube formation", bean.getRackBarcode());
            return;
        } else if (rack.getTubeFormations().size() > 1) {
            bean.getMessageCollection().addError(MULTIPLE_TUBE_FORMS, bean.getRackBarcode());
            return;
        }

        List<ManifestRecord> manifestRecords = manifestSession.findRecordsByKey(rack.getLabel(), Metadata.Key.BOX_ID);
        if (CollectionUtils.isEmpty(manifestRecords)) {
            bean.getMessageCollection().addError(RACK_NOT_IN_MANIFEST, manifestSession.getSessionName(),
                    rack.getLabel());
            return;
        }

        TubeFormation tubeFormation = rack.getTubeFormations().iterator().next();
        Map<VesselPosition, String> rackPositions = tubeFormation.getContainerRole().getMapPositionToVessel().
                entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                collect(Collectors.toMap(mapEntry -> mapEntry.getKey(), mapEntry -> mapEntry.getValue().getLabel()));
        Map<VesselPosition, String> manifestPositions = new HashMap<>();
        Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
        Map<String, String> sampleToTube = new HashMap<>();
        Map<String, String> tubeToSample = new HashMap<>();
        List<String> materialTypes = new ArrayList<>();
        String packageBarcode = null;
        String trackingId = null;

        // Finds the manifest records for this rack.
        for (ManifestRecord manifestRecord : manifestRecords) {
            // Uses the tube barcode for the MercurySample.sampleKey.
            String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());
            materialTypes.add(StringUtils.defaultIfBlank(
                    manifestRecord.getValueByKey(Metadata.Key.MATERIAL_TYPE), "No Type Given"));
            if (StringUtils.isBlank(packageBarcode)) {
                packageBarcode = manifestRecord.getValueByKey(Metadata.Key.PACKAGE_ID);
            }
            if (StringUtils.isBlank(trackingId)) {
                trackingId = manifestRecord.getValueByKey(Metadata.Key.TRACKING_NUMBER);
            }
            String tubeBarcode = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
            String positionName = manifestRecord.getValueByKey(Metadata.Key.WELL_POSITION);
            VesselPosition position = VesselPosition.getByName(positionName);
            if (position == null) {
                bean.getMessageCollection().addError(UNKNOWN_WELL, positionName);
            } else {
                manifestPositions.put(position, tubeBarcode);
            }
            sampleToTube.put(sampleName, tubeBarcode);
            tubeToSample.put(tubeBarcode, sampleName);
        }

        // The rack scan's tube barcodes and positions must match the manifest.
        Map<String, Pair<String, String>> mismatches = new HashMap<>();
        rackPositions.entrySet().forEach(mapEntry -> {
            VesselPosition rackPosition = mapEntry.getKey();
            String rackTube = mapEntry.getValue();
            String manifestTube = manifestPositions.get(rackPosition);
            if (!Objects.equal(rackTube, manifestTube)) {
                mismatches.put(rackPosition.name(), Pair.of(rackTube, manifestTube));
            }
        });
        CollectionUtils.subtract(manifestPositions.keySet(), rackPositions.keySet()).forEach(manifestPosition -> {
            String manifestTube = manifestPositions.get(manifestPosition);
            mismatches.put(manifestPosition.name(), Pair.of(null, manifestTube));
        });
        mismatches.keySet().stream().sorted().forEach(position -> {
            String rackTube = mismatches.get(position).getLeft();
            String manifestTube = mismatches.get(position).getRight();
            bean.getMessageCollection().addError(WRONG_TUBE_IN_POSITION, position,
                    StringUtils.isBlank(rackTube) ? "no tube" : rackTube,
                    StringUtils.isBlank(manifestTube) ? "no tube" : manifestTube);
        });

        // Creates or updates samples. Does not do partial accessioning; either the whole rack or none.
        Map<String, MercurySample> sampleMap = new HashMap<>();
        Map<String, BarcodedTube> tubeMap = new HashMap<>();
        List<Object> newEntities = new ArrayList<>();
        if (!bean.getMessageCollection().hasErrors() && !sampleToTube.isEmpty()) {
            sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleMetadata.keySet());
            for (String sampleName : sampleMetadata.keySet()) {
                MercurySample mercurySample = sampleMap.get(sampleName);
                if (mercurySample == null) {
                    // All MercurySamples will have MetadataSource.MERCURY.
                    mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.MERCURY);
                    newEntities.add(mercurySample);
                    sampleMap.put(sampleName, mercurySample);
                }
                mercurySample.updateMetadata(new HashSet<>(sampleMetadata.get(sampleName)));
                // Links sample to tube.
                tubeMap.get(sampleToTube.get(sampleName)).addSample(mercurySample);
            }

            // Sets the tube's volume and concentration from values in the manifest.
            for (String barcode : tubeMap.keySet()) {
                BarcodedTube tube = tubeMap.get(barcode);
                for (Metadata metadata : sampleMetadata.get(tubeToSample.get(barcode))) {
                    if (metadata.getKey() == Metadata.Key.VOLUME) {
                        tube.setVolume(NumberUtils.toScaledBigDecimal(metadata.getValue()));
                    } else if (metadata.getKey() == Metadata.Key.CONCENTRATION) {
                        tube.setConcentration(NumberUtils.toScaledBigDecimal(metadata.getValue()));
                    }
                }
            }
        }

        JiraTicket jiraTicket = makeRackRct(bean, rack, tubeMap.keySet(), sampleMap.keySet(),
                materialTypes, packageBarcode, trackingId);
        if (jiraTicket != null) {
            // Doesn't persist any entities if Jira fails.
            bean.setRctUrl(jiraTicket.getBrowserUrl());
            newEntities.add(jiraTicket);
            labVesselDao.persistAll(newEntities);
        }
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
        processNewManifestFiles(null, bean.getMessageCollection());
    }

    /** Reads the manifest file given in the bean.filename and makes a new manifest if values have changed. */
    public void pullOne(MayoSampleReceiptActionBean bean) {
        processNewManifestFiles(bean.getFilename(), bean.getMessageCollection());
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * True errors are put in the MessageCollection.
     */
    public @NotNull List<List<String>> readManifestFileCellGrid(String filename, MessageCollection messages) {
        if (StringUtils.isNotBlank(filename)) {
            byte[] spreadsheet = googleBucketDao.download(filename, messages);
            if (spreadsheet != null && spreadsheet.length > 0) {
                return MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, filename, messages);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Finds the most recent ManifestSession for the rack barcode. Does not read bucket storage.
     * The manifest must already exist from an earlier package receipt.
     * The bean is updated with the result.
     */
    public void lookupManifestSession(MayoSampleReceiptActionBean bean) {
        manifestSessionDao.getSessionsByVesselLabel(bean.getRackBarcode()).stream().findFirst().ifPresent(
                manifestSession -> {
                    bean.setManifestSessionId(manifestSession.getManifestSessionId());
                    ManifestFile manifestFile = manifestSession.getManifestFile();
                    if (!manifestFile.getNamespaceFilename().getLeft().equals(mayoManifestConfig.getBucketName())) {
                        bean.getMessageCollection().addError("Manifest file " +
                                manifestFile.getNamespaceFilename().getRight() +
                                " in unreadable bucket " + manifestFile.getNamespaceFilename().getLeft() +
                                " (expected " + mayoManifestConfig.getBucketName() + ").");
                    }
                    bean.setFilename(manifestFile.getNamespaceFilename().getRight());
                });
    }

    /**
     * Finds the most recent ManifestSession for the package id.
     * Reads files from storage as necessary and makes Manifest Sessions from any new ones.
     * The bean is updated with the result.
     */
    public void lookupManifestSession(MayoPackageReceiptActionBean bean) {
        ManifestSession manifestSession;
        assert(StringUtils.isNotBlank(bean.getPackageBarcode()));
        // If manifest(s) already exist, pick the most recent one.
        manifestSession = manifestSessionDao.getSessionsByPrefix(bean.getPackageBarcode()).stream().
                findFirst().orElse(null);
        if (manifestSession == null) {
            // Searches bucket storage for new manifest files. First tries a filename made from the manifest key.
            // If no luck yet, searches for all new files. This makes ManifestSessions for each new one.
            for (String filename : Arrays.asList(
                    bean.getPackageBarcode() + ".csv",
                    bean.getPackageBarcode() + ".CSV",
                    null)) {
                manifestSession = processNewManifestFiles(filename, bean.getMessageCollection()).stream().
                        filter(session -> bean.getPackageBarcode().equals(session.getSessionPrefix())).
                        findFirst().orElse(null);
                if (manifestSession != null) {
                    break;
                }
            }
        }
        if (manifestSession != null) {
            bean.setManifestSessionId(manifestSession.getManifestSessionId());
            ManifestFile manifestFile = manifestSession.getManifestFile();
            if (!manifestFile.getNamespaceFilename().getLeft().equals(mayoManifestConfig.getBucketName())) {
                bean.getMessageCollection().addError("Manifest file " + manifestFile.getNamespaceFilename().getRight() +
                        " in unreadable bucket " + manifestFile.getNamespaceFilename().getLeft() +
                        " (expected " + mayoManifestConfig.getBucketName() + ").");
            }
            bean.setFilename(manifestFile.getNamespaceFilename().getRight());
        }
    }

    /**
     * Reads manifest file storage and persists files as ManifestSessions.
     * @param forceReloadFilename if non-blank forces reload of the one file only. If blank, all new files are read.
     */
    private List<ManifestSession> processNewManifestFiles(@Nullable String forceReloadFilename,
            MessageCollection messages) {
        List<ManifestSession> manifestSessions = new ArrayList<>();
        Set<Object> newEntities = new HashSet<>();
        String bucketName = mayoManifestConfig.getBucketName();

        // Makes the list of filenames that need to be processed.
        List<String> filenames;
        if (StringUtils.isNotBlank(forceReloadFilename)) {
            byte[] content = googleBucketDao.download(forceReloadFilename, messages);
            if (content == null || content.length < 1) {
                messages.addWarning(INVALID, "storage bucket file", forceReloadFilename);
                filenames = Collections.emptyList();
            } else {
                filenames = Collections.singletonList(forceReloadFilename);
            }
        } else {
            filenames = googleBucketDao.list(messages);
            filenames.sort(Comparator.naturalOrder());
            // Removes the filenames already processed, including files that could not be made into manifests.
            filenames.removeAll(manifestSessionDao.getFilenamesForNamespace(mayoManifestConfig.getBucketName()));
        }
        // Each manifest file is made into one manifest session if it has no errors.
        for (String filename : filenames) {
            if (MayoManifestImportProcessor.cleanupValue(filename).equals(filename)) {
                manifestSessions.add(makeManifestSession(filename, bucketName, messages, newEntities));
            } else {
                logger.error("Skipping Google bucket file '%s' because the name contains non-7-bit ascii chars.");
            }
        }
        if (!newEntities.isEmpty()) {
            // Manifest Sessions and Manifest Files are persisted here because they are independent of
            // further processing and should not be rolled back in any case.
            manifestSessionDao.persistAll(newEntities);
            manifestSessionDao.flush();
        }
        if (!filenames.isEmpty()) {
            messages.addInfo(String.format(FILES_PROCESSED, filenames.size(), StringUtils.join(filenames, ", ")));
        }
        if (!manifestSessions.isEmpty()) {
            messages.addInfo(String.format(MANIFEST_CREATED, manifestSessions.stream().
                    map(session -> String.format(MANIFEST_CREATE_ITEM,
                            session.getSessionPrefix(), session.getRecords().size())).
                    collect(Collectors.joining(", "))));
        }
        return manifestSessions;
    }

    private ManifestSession makeManifestSession(String filename, String bucketName,
            MessageCollection accumulatedMessages, Set<Object> newEntities) {
        MessageCollection messages = new MessageCollection();

        // Always persists the filename if the file exists. If already persisted then uses the entity.
        String qualifiedFilename = ManifestFile.qualifiedFilename(bucketName, filename);
        ManifestFile manifestFile = findManifestFile(qualifiedFilename);
        if (manifestFile == null) {
            manifestFile = new ManifestFile(qualifiedFilename);
            newEntities.add(manifestFile);
        }

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
        accumulatedMessages.addErrors(messages.getErrors());
        accumulatedMessages.addWarning(messages.getWarnings());
        accumulatedMessages.addInfos(messages.getInfos());
        return manifestSession;
    }

    private ManifestFile findManifestFile(String qualifiedFilename) {
        return manifestSessionDao.findSingle(ManifestFile.class, ManifestFile_.qualifiedFilename, qualifiedFilename);
    }

    /** Makes a new RCT in Jira for the Mayo package. */
    private JiraTicket makePackageRct(MayoPackageReceiptActionBean bean) {
        String title = bean.getPackageBarcode();
        CustomField titleField = new CustomField(new CustomFieldDefinition("summary", "Summary", true), title);

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(JIRA_DEFINITION_MAP.get("ShipmentCondition"), bean.getShipmentCondition()));
            add(new CustomField(JIRA_DEFINITION_MAP.get("TrackingNumber"), bean.getTrackingNumber()));
            // Delivery Method is a Jira selection and does not like to be set if value is "None".
            if (StringUtils.isNotBlank(bean.getDeliveryMethod()) && !JIRA_NONE.equals(bean.getDeliveryMethod())) {
                add(new CustomField(bean.getDeliveryMethod(), JIRA_DEFINITION_MAP.get("KitDeliveryMethod")));
            }
            // todo emp change to "Racks" when Jira field is added.
            add(new CustomField(JIRA_DEFINITION_MAP.get("Samples"), bean.getRackCount() + " Racks: " +
                    bean.getRackBarcodeString()));
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), " "));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"), " "));
        }};

        JiraTicket jiraTicket = null;
        JiraIssue jiraIssue;
        try {
            ManifestSession manifestSession = bean.getManifestSessionId() == null ? null :
                    manifestSessionDao.find(bean.getManifestSessionId());
            if (manifestSession == null || StringUtils.isBlank(manifestSession.getReceiptTicket())) {
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

            // Writes a comment if there was some error.
            if (StringUtils.isNotBlank(bean.getQuarantineReason()) && bean.getManifestSessionId() == null) {
                String comment = "Quarantine reason: ";
                if (StringUtils.isNotBlank(bean.getQuarantineReason())) {
                    comment += bean.getQuarantineReason();
                } else if (bean.getMessageCollection().hasErrors()) {
                    comment += StringUtils.join(bean.getMessageCollection().getErrors(), "; ") + "\n";
                } else if (bean.getMessageCollection().hasWarnings()) {
                    comment += StringUtils.join(bean.getMessageCollection().getWarnings(), "; ") + "\n";
                } else {
                    comment += "(unknown)\n";
                }
                jiraIssue.addComment(comment);
            }
            return jiraTicket;

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
            return null;
        }
    }

    private JiraTicket makeRackRct(MayoSampleReceiptActionBean bean, @NotNull RackOfTubes rack,
            @Nullable Collection<String> tubes, @Nullable Collection<String> samples,
            @Nullable List<String> materialTypes, @Nullable String packageId, @Nullable String trackingNumber) {

        List<String> tubeBarcodes = tubes == null ? Collections.emptyList() :
                tubes.stream().sorted().collect(Collectors.toList());
        List<String> sampleNames = samples == null ? Collections.emptyList() :
                samples.stream().sorted().collect(Collectors.toList());
        boolean isQuarantined = StringUtils.isNotBlank(bean.getQuarantineReason()) &&
                bean.getManifestSessionId() == null;
        boolean isAccessioned = CollectionUtils.isNotEmpty(sampleNames);

        // The title field.
        String title = bean.getRackBarcode() + (StringUtils.isNotBlank(packageId) ? " in " + packageId : "");
        CustomField titleField = new CustomField(new CustomFieldDefinition("summary", "Summary", true), title);

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), CollectionUtils.isEmpty(materialTypes) ?
                    (tubeBarcodes.size() + " unknown type") :
                    // Counts each type of material, then for each material type grouping makes a string
                    // of the count and the material type, e.g. "3 Fresh Frozen Blood, 2 Dried Blood"
                    materialTypes.stream().
                            collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).
                            entrySet().stream().
                            sorted(Comparator.comparing(Map.Entry<String, Long>::getKey)).
                            map(mapEntry -> String.format("%d %s", mapEntry.getValue(), mapEntry.getKey())).
                            collect(Collectors.joining(", "))));
            if (isAccessioned) {
                add(new CustomField(JIRA_DEFINITION_MAP.get("Samples"), StringUtils.join(sampleNames, " ")));
            }
            add(new CustomField(JIRA_DEFINITION_MAP.get("TrackingNumber"), StringUtils.trimToEmpty(trackingNumber)));
            add(new CustomField(JIRA_DEFINITION_MAP.get("ShipmentCondition"), " "));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"), " "));
        }};

        JiraTicket jiraTicket = null;
        try {
            // Gets the highest numbered RCT ticket for the rack.
            if (rack.getJiraTickets() != null) {
                jiraTicket = rack.getJiraTickets().stream().
                        filter(t -> t.getTicketName().
                                startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                        max(Comparator.comparing(JiraTicket::getTicketName)).orElse(null);
            }
            JiraIssue jiraIssue;
            String issueStatus;
            boolean isUpdate = false;
            boolean alreadyAccessioned = false;
            if (jiraTicket == null) {
                // Makes a new ticket.
                jiraIssue = jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT,
                        userBean.getLoginUserName(), CreateFields.IssueType.RECEIPT, title, customFieldList);
                // Writes the RCT title which for some reason gets ignored in createIssue.
                jiraService.updateIssue(jiraIssue.getKey(), Collections.singleton(titleField));
                // Links RCT ticket to the rack.
                jiraTicket = new JiraTicket(jiraService, jiraIssue.getKey());
                rack.addJiraTicket(jiraTicket);
            } else {
                // Updates the existing ticket.
                isUpdate = true;
                jiraIssue = jiraService.getIssue(jiraTicket.getTicketName());
                issueStatus = jiraIssue.getStatus();
                customFieldList.add(titleField);
                jiraService.updateIssue(jiraIssue.getKey(), customFieldList);
                alreadyAccessioned = ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName().equals(issueStatus);
            }
            bean.setRctUrl(jiraTicket.getBrowserUrl());

            if (isQuarantined) {
                String comment = "Quarantine reason: ";
                if (StringUtils.isNotBlank(bean.getQuarantineReason())) {
                    comment += bean.getQuarantineReason();
                } else if (bean.getMessageCollection().hasErrors()) {
                    comment += StringUtils.join(bean.getMessageCollection().getErrors(), "; ") + "\n";
                } else if (bean.getMessageCollection().hasWarnings()) {
                    comment += StringUtils.join(bean.getMessageCollection().getWarnings(), "; ") + "\n";
                } else {
                    comment += "(unknown)\n";
                }
                jiraIssue.addComment(comment);
            }
            String comment = (isUpdate && alreadyAccessioned) ?
                    String.format(REACCESSIONED, StringUtils.join(sampleNames, " ")) :
                    String.format(ACCESSIONED, rack.getLabel(), StringUtils.join(sampleNames, " "));
            if (isAccessioned && !alreadyAccessioned) {
                jiraIssue.postTransition(ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName(), comment);
            } else {
                jiraIssue.addComment(comment);
            }

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
        }
        return jiraTicket;
    }

    public UserBean getUserBean() {
        return userBean;
    }
}
