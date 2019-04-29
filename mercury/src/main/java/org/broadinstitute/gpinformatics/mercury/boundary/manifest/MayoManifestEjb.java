package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.MayoReceivingActionBean;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
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
import java.util.LinkedHashMap;
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
    public static final String NO_RACK_TYPE = "Cannot find a rack type for scanned positions %s.";
    public static final String ONLY_RECEIVED = "Received rack %s, tubes %s.";
    public static final String OVERWRITING = "Previously uploaded rack %s will be overwritten.";
    public static final String RACK_NOT_IN_MANIFEST = "Rack %s is not in the manifest.";
    public static final String REACCESSIONED = "Samples have been re-accessioned: %s";
    public static final String TUBE_NOT_IN_MANIFEST = "Some rack scan tubes are not in the manifest: %s";
    public static final String TUBE_NOT_IN_RACKSCAN = "Some manifest tubes are not in the rack scan: %s";
    public static final String UNKNOWN_WELL = "Manifest contains unknown well position: %s.";
    public static final String UNKNOWN_WELL_SCAN = "Rack scan contains unknown well position: %s.";
    public static final String WRONG_BUCKET = "Manifest file %s is not in the configured storage bucket %s.";
    public static final String WRONG_POSITION = "Tube %s has manifest position %s but rack scan position %s.";

    public static final String RCT_TITLE = "Mayo rack %s";
    public static final String QUARANTINED_RCT_TITLE = "Quarantined Mayo rack %s";

    public static final Map<String, CustomFieldDefinition> JIRA_DEFINITION_MAP =
            new HashMap<String, CustomFieldDefinition>() {{
        put("MaterialTypeCounts", new CustomFieldDefinition("customfield_15660", "Material Type Counts", true));
        put("Samples", new CustomFieldDefinition("customfield_12662", "Sample IDs", false));
        put("ShipmentCondition", new CustomFieldDefinition("customfield_15661", "Shipment Condition", false));
        put("TrackingNumber", new CustomFieldDefinition("customfield_15663", "Tracking Number", false));
        put("RequestingPhysician", new CustomFieldDefinition("customfield_16163", "Requesting Physician", false));
        put("KitDeliveryMethod", new CustomFieldDefinition("customfield_13767", "Kit Delivery Method", false));
        put("ReceiptType", new CustomFieldDefinition("customfield_17360", "Receipt Type", false));
    }};
    /** Maps customfield_nnnnn to JIRA_DEFINITION_MAP.key */
    public static final Map<String, String> REVERSE_JIRA_DEFINITION_MAP = JIRA_DEFINITION_MAP.entrySet().stream().
            collect(Collectors.toMap(
                    mapEntry -> mapEntry.getValue().getJiraCustomFieldId(),
                    mapEntry -> mapEntry.getKey()));

    private static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    private static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;
    private static final String FILE_DELIMITER = ",";
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
    public void validateAndScan(MayoReceivingActionBean bean) {
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
        if (StringUtils.isNotBlank(invalidPositions)) {
            bean.getMessageCollection().addError(UNKNOWN_WELL_SCAN, invalidPositions);
        }

        // Turns the rack scan into a list of position & sample to pass to and from jsp.
        if (!bean.getMessageCollection().hasErrors()) {
            bean.getRackScanEntries().addAll(bean.getRackScan().entrySet().stream().
                    map(mapEntry -> StringUtils.join(mapEntry.getKey(), " ", mapEntry.getValue())).
                    sorted().collect(Collectors.toList()));
            // Figures out the minimal rack that will accommodate all vessel positions.
            RackOfTubes.RackType rackType = inferRackType(bean.getRackScan().keySet());
            if (rackType != null) {
                bean.setVesselGeometry(rackType.getVesselGeometry());
            } else {
                bean.getMessageCollection().addError(NO_RACK_TYPE,
                        bean.getRackScan().keySet().stream().sorted().collect(Collectors.joining(", ")));
            }
        }
    }

    /**
     * Makes receipt artifacts for the rack and tube and does the sample accessioning if a manifest
     * is found and all matches up.
     */
    public void receiveAndAccession(MayoReceivingActionBean bean) {
        RackOfTubes rack = null;
        Map<String, BarcodedTube> tubeMap = new HashMap<>();
        Map<String, VesselPosition> tubeToPosition = bean.getRackScan().entrySet().stream().
                filter(mapEntry -> StringUtils.isNotBlank(mapEntry.getValue())).
                collect(Collectors.toMap(mapEntry -> mapEntry.getValue(),
                        mapEntry -> VesselPosition.valueOf(mapEntry.getKey())));

        // Finds existing vessels.
        for (LabVessel vessel : labVesselDao.findListByList(LabVessel.class, LabVessel_.label,
                Stream.concat(tubeToPosition.keySet().stream(), Stream.of(bean.getRackBarcode())).
                        filter(StringUtils::isNotBlank).
                        collect(Collectors.toList()))) {
            if (vessel.getLabel().equals(bean.getRackBarcode()) &&
                    OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
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

        // Creates or updates the rack. Uses a new or existing tube formation obtained from the rackScan.
        List<Pair<VesselPosition, String>> positionBarcodeList = new ArrayList<>();
        for (String positionName : bean.getRackScan().keySet()) {
            positionBarcodeList.add(Pair.of(VesselPosition.getByName(positionName),
                    bean.getRackScan().get(positionName)));
        }
        String digest = TubeFormation.makeDigest(positionBarcodeList);
        TubeFormation tubeFormation = tubeFormationDao.findByDigest(digest);
        RackOfTubes.RackType rackType = (rack != null) ?
                rack.getRackType() : inferRackType(bean.getRackScan().keySet());
        if (tubeFormation == null) {
            tubeFormation = new TubeFormation(vesselPositionToTube, rackType);
            newEntities.add(tubeFormation);
        }
        if (rack == null) {
            rack = new RackOfTubes(bean.getRackBarcode(), rackType);
            newEntities.add(rack);
        }
        tubeFormation.addRackOfTubes(rack);

        // Looks for a manifest in order to make the samples and do accessioning. Any problems found
        // while processing manifests should not prevent the rack and tubes from being Received.
        ManifestSession manifestSession = lookupManifestSession(bean.getManifestKey(), bean.getMessageCollection(),
                true);
        Map<String, MercurySample> sampleMap = Collections.emptyMap();
        Set<String> packageId = new HashSet<>();
        Set<String> trackingNumber = new HashSet<>();
        Set<String> requestingPhysician = new HashSet<>();
        List<String> materialTypes = new ArrayList<>();
        if (manifestSession == null) {
            bean.getMessageCollection().addWarning(INVALID, "manifest", bean.getManifestKey());
        } else {
            Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
            Map<String, VesselPosition> manifestPositions = new HashMap<>();
            Map<String, String> sampleToTube = new HashMap<>();
            Map<String, String> tubeToSample = new HashMap<>();

            for (ManifestRecord manifestRecord : manifestSession.findRecordsByKey(bean.getRackBarcode(),
                    Metadata.Key.BOX_ID)) {
                // Uses the tube barcode for the MercurySample.sampleKey.
                String sampleName = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
                sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());
                materialTypes.add(StringUtils.defaultIfBlank(
                        manifestRecord.getValueByKey(Metadata.Key.MATERIAL_TYPE), "No Type Given"));

                String tubeBarcode = manifestRecord.getValueByKey(Metadata.Key.BROAD_2D_BARCODE);
                String positionName = manifestRecord.getValueByKey(Metadata.Key.WELL_POSITION);
                VesselPosition position = VesselPosition.getByName(positionName);
                if (position == null) {
                    bean.getMessageCollection().addError(UNKNOWN_WELL, positionName);
                } else {
                    manifestPositions.put(tubeBarcode, position);
                }
                sampleToTube.put(sampleName, tubeBarcode);
                tubeToSample.put(tubeBarcode, sampleName);

                packageId.add(StringUtils.trimToEmpty(manifestRecord.getValueByKey(Metadata.Key.PACKAGE_ID)));
                trackingNumber.add(StringUtils.trimToEmpty(manifestRecord.getValueByKey(Metadata.Key.TRACKING_NUMBER)));
                requestingPhysician.add(StringUtils.trimToEmpty(manifestRecord.getValueByKey(
                        Metadata.Key.REQUESTING_PHYSICIAN)));
            }
            if (tubeToSample.isEmpty()) {
                bean.getMessageCollection().addError(RACK_NOT_IN_MANIFEST, bean.getRackBarcode());
            }
            // The rack scan tube barcodes and positions must match the manifest.
            if (!CollectionUtils.isEqualCollection(tubeToPosition.keySet(), manifestPositions.keySet())) {
                String missingFromManifest = StringUtils.join(
                        CollectionUtils.subtract(tubeToPosition.keySet(), manifestPositions.keySet()), " ");
                if (StringUtils.isNotBlank(missingFromManifest)) {
                    bean.getMessageCollection().addError(TUBE_NOT_IN_MANIFEST, missingFromManifest);
                }
                String missingFromRackscan = StringUtils.join(
                        CollectionUtils.subtract(manifestPositions.keySet(), tubeToPosition.keySet()), " ");
                if (StringUtils.isNotBlank(missingFromRackscan)) {
                    bean.getMessageCollection().addError(TUBE_NOT_IN_RACKSCAN, missingFromRackscan);
                }
            } else {
                tubeToPosition.keySet().stream().
                        filter(tube -> !tubeToPosition.get(tube).equals(manifestPositions.get(tube))).
                        forEachOrdered(tube -> bean.getMessageCollection().addError(WRONG_POSITION, tube,
                                manifestPositions.get(tube), tubeToPosition.get(tube)));
            }

            if (!bean.getMessageCollection().hasErrors()) {
                // Creates or updates samples. All will have MetadataSource.MERCURY.
                sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleMetadata.keySet());
                for (String sampleName : sampleMetadata.keySet()) {
                    MercurySample mercurySample = sampleMap.get(sampleName);
                    if (mercurySample == null) {
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
        }

        // Jira limit is 255.
        String physicians = requestingPhysician.stream().sorted().distinct().collect(Collectors.joining(","));
        if (physicians.length() > 254) {
            physicians = physicians.substring(0, 251) + "...";
        }

        JiraTicket jiraTicket = makeOrUpdateRct(bean, rack,
                tubeToPosition.keySet().stream().sorted().collect(Collectors.toList()),
                sampleMap.keySet().stream().sorted().collect(Collectors.toList()),
                materialTypes,
                packageId.stream().sorted().collect(Collectors.joining(", ")),
                physicians,
                trackingNumber.stream().sorted().collect(Collectors.joining(", "))
        );
        if (jiraTicket != null) {
            // Doesn't persist any entities if Jira fails.
            newEntities.add(jiraTicket);
            labVesselDao.persistAll(newEntities);
        }
    }
    /**
     * Re-accessions a rack and its tubes. Any tube or position changes in the rack must be fixed up
     * first. If a matching tube formation isn't found and error is generated.
     */
    public void reaccession(MayoReceivingActionBean bean) {
        assert(StringUtils.isNotBlank(bean.getRackBarcode()));
        LabVessel vessel = labVesselDao.findByIdentifier(bean.getRackBarcode());
        if (vessel == null) {
            bean.getMessageCollection().addError(INVALID, "rack", bean.getRackBarcode());
            return;
        }
        RackOfTubes rack = OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class) ?
                OrmUtil.proxySafeCast(vessel, RackOfTubes.class) : null;
        if (rack == null) {
            bean.getMessageCollection().addError(NOT_A_RACK, bean.getRackBarcode());
            return;
        }
        if (CollectionUtils.isEmpty(rack.getTubeFormations())) {
            bean.getMessageCollection().addError(INVALID, "tube formation", bean.getRackBarcode());
            return;
        } else if (rack.getTubeFormations().size() > 1) {
            bean.getMessageCollection().addError(MULTIPLE_TUBE_FORMS, bean.getRackBarcode());
            return;
        }
        TubeFormation tubeFormation = rack.getTubeFormations().iterator().next();
        bean.setVesselGeometry(tubeFormation.getVesselGeometry());
        // Synthesizes a rack scan from the existing rack.
        bean.setRackScan(new LinkedHashMap<>());
        tubeFormation.getContainerRole().getMapPositionToVessel().entrySet().forEach(entry -> {
            VesselPosition position = entry.getKey();
            BarcodedTube tube = entry.getValue();
            bean.getRackScan().put(position.name(), tube == null ? "" : tube.getLabel());
        });
        receiveAndAccession(bean);
    }

    /**
     * Obtains the manifest file in a cell grid for either the filename or the filename associated with the rack.
     */
    public void readManifestFileCellGrid(MayoReceivingActionBean bean) {
        // If no filename was given, looks up a filename via the manifest session for the rack barcode.
        if (StringUtils.isBlank(bean.getFilename())) {
            if (StringUtils.isBlank(bean.getManifestKey())) {
                bean.getMessageCollection().addError("Need a filename or a rack barcode.");
            } else {
                ManifestSession manifestSession = lookupManifestSession(bean.getManifestKey(),
                        bean.getMessageCollection(), true);
                if (manifestSession == null || manifestSession.getManifestFile() == null) {
                    bean.getMessageCollection().addInfo(INVALID, "Mercury manifest record", bean.getManifestKey());
                } else {
                    String[] fileBucket = manifestSession.getManifestFile().getQualifiedFilename().
                            split(FILE_DELIMITER);
                    String bucketName = mayoManifestConfig.getBucketName();
                    if (fileBucket.length == 0 || !bucketName.equals(fileBucket[1])) {
                        bean.getMessageCollection().addError(WRONG_BUCKET, fileBucket[0], bucketName);
                    } else {
                        bean.setFilename(fileBucket[0]);
                    }
                }
            }
        }
        bean.setManifestCellGrid(readManifestFileCellGrid(bean.getFilename(), bean.getMessageCollection()));
    }

    /** Generates info messages. Puts a listing of all bucket filenames in the bean. */
    public void testAccess(MayoReceivingActionBean bean) {
        bean.getBucketList().clear();
        bean.setBucketList(googleBucketDao.test(bean.getMessageCollection()));
    }

    /**
     * Finds bucket filenames that have been processed but were not made into manifest sessions
     * and puts the list into the bean.
     */
    public void getFailedFiles(MayoReceivingActionBean bean) {
        List<String> previouslySeen = manifestSessionDao.getQualifiedFilenames(
                FILE_DELIMITER + mayoManifestConfig.getBucketName());
        List<String> manifestFilenames = manifestSessionDao.findAll(ManifestSession.class).stream().
                map(manifestSession -> manifestSession.getManifestFile()).
                filter(manifestFile -> manifestFile != null).
                map(manifestFile -> manifestFile.getQualifiedFilename()).
                collect(Collectors.toList());
        previouslySeen.removeAll(manifestFilenames);
        // Expects qualifiedFilename = filename + delimiter + bucket name.
        // Keep only the filename part.
        bean.setFailedFilesList(previouslySeen.stream().
                map(qualifiedFilename -> qualifiedFilename.split(FILE_DELIMITER)[0]).
                sorted().
                collect(Collectors.toList()));
    }

    public void pullAll(MayoReceivingActionBean bean) {
        processNewManifestFiles(null, bean.getMessageCollection());
    }

    /** Reads the manifest file given in the bean.filename and makes a new manifest if values have changed. */
    public void pullOne(MayoReceivingActionBean bean) {
        processNewManifestFiles(bean.getFilename(), bean.getMessageCollection());
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * True errors are put in the MessageCollection.
     */
    private @NotNull List<List<String>> readManifestFileCellGrid(String filename, MessageCollection messages) {
        if (StringUtils.isNotBlank(filename)) {
            byte[] spreadsheet = googleBucketDao.download(filename, messages);
            if (spreadsheet != null && spreadsheet.length > 0) {
                return MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, filename, messages);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns the most recent ManifestSession for the manifest key (rack barcode).
     * Reads files from storage as necessary and makes Manifest Sessions from any new ones.
     * @return the manifest session or null if not found.
     */
    private ManifestSession lookupManifestSession(@Nullable String manifestKey, MessageCollection messageCollection,
            boolean mayReadStorage) {
        ManifestSession manifestSession = null;
        if (StringUtils.isNotBlank(manifestKey)) {
            manifestSession = manifestSessionDao.getSessionsByPrefix(manifestKey).stream().findFirst().orElse(null);
        }
        if (manifestSession == null && mayReadStorage) {
            // If there is no ManifestSession for the rack, searches bucket storage for new manifest files.
            processNewManifestFiles(null, messageCollection);
            return lookupManifestSession(manifestKey, messageCollection, false);
        }
        return manifestSession;
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
            filenames = Collections.singletonList(forceReloadFilename);
        } else {
            filenames = googleBucketDao.list(messages);
            // Removes the filenames already seen from those found in the bucket.
            // Expects qualifiedFilename = filename + delimiter + bucket name.
            List<String> previouslySeen = manifestSessionDao.getQualifiedFilenames(FILE_DELIMITER + bucketName).
                    stream().
                    map(name -> StringUtils.substringBefore(name, FILE_DELIMITER)).
                    collect(Collectors.toList());
            filenames.removeAll(previouslySeen);
            Collections.sort(filenames);
        }
        // Each manifest file is made into one or more new manifest sessions, one per rack.
        for (String filename : filenames) {
            if (MayoManifestImportProcessor.cleanupValue(filename).equals(filename)) {
                manifestSessions.addAll(makeManifestSessions(filename, bucketName, messages, newEntities));
            } else {
                logger.error("Skipping Google bucket file '%s' because the name contains non-7-bit ascii chars.");
            }
        }
        if (!newEntities.isEmpty()) {
            // Manifest Sessions and Manifest Files are persisted here because they are independent of any
            // further sample receipt and should not be rolled back in case the sample receipt fails.
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

    private Collection<ManifestSession> makeManifestSessions(String filename, String bucketName,
            MessageCollection messageCollection, Set<Object> newEntities) {

        // Always persists the filename. If already persisted then uses the entity.
        String qualifiedFilename = filename + FILE_DELIMITER + bucketName;
        ManifestFile manifestFile = findManifestFile(qualifiedFilename);
        if (manifestFile == null) {
            manifestFile = new ManifestFile(qualifiedFilename);
            newEntities.add(manifestFile);
        }

        List<ManifestSession> sessions = new ArrayList<>();
        List<List<String>> cellGrid = readManifestFileCellGrid(filename, messageCollection);
        if (!cellGrid.isEmpty()) {
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            // Makes a new ManifestSession for each manifest key (i.e. rack barcode).
            Multimap<String, ManifestRecord> manifestKeyToManifestRecords =
                    processor.makeManifestRecords(cellGrid, filename, messageCollection);
            for (String manifestKey : manifestKeyToManifestRecords.keySet()) {
                ManifestSession manifestSession = new ManifestSession(null, manifestKey,
                        userBean.getBspUser(), false, manifestKeyToManifestRecords.get(manifestKey));
                manifestSession.setManifestFile(manifestFile);
                newEntities.add(manifestSession);
                sessions.add(manifestSession);
            }
        }
        return sessions;
    }

    private ManifestFile findManifestFile(String qualifiedFilename) {
        return manifestSessionDao.findSingle(ManifestFile.class, ManifestFile_.qualifiedFilename, qualifiedFilename);
    }

    // Infers the rack type from the vessel positions in the rackScan.
    private RackOfTubes.RackType inferRackType(Collection<String> positionNames) {
        // Prefers the default rack type over any others.
        List<RackOfTubes.RackType> searchList = new ArrayList<>();
        searchList.add(DEFAULT_RACK_TYPE);
        searchList.addAll(Arrays.asList(RackOfTubes.RackType.values()));

        Set<VesselPosition> positions = positionNames.stream().
                map(VesselPosition::getByName).
                collect(Collectors.toSet());

        return searchList.stream().
                filter(type -> type.isRackScannable() && CollectionUtils.isSubCollection(positions,
                        Arrays.asList(type.getVesselGeometry().getVesselPositions()))).
                findFirst().
                orElse(null);
    }

    private JiraTicket makeOrUpdateRct(MayoReceivingActionBean bean, RackOfTubes rack,
            List<String> tubeBarcodes, List<String> sampleNames, List<String> materialTypes,
            String packageId, String requestingPhysician, String trackingNumber) {
        // Infers that it's accessioned if no errors were given and all the samples are present.
        boolean isQuarantined = bean.getMessageCollection().hasErrors() || (tubeBarcodes.size() != sampleNames.size());
        // The title field.
        String title = String.format(isQuarantined ? QUARANTINED_RCT_TITLE : RCT_TITLE, rack.getLabel());
        CustomField titleField = new CustomField(new CustomFieldDefinition("summary", "Summary", true), title);

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(JIRA_DEFINITION_MAP.get("MaterialTypeCounts"), materialTypes.isEmpty() ?
                    (tubeBarcodes.size() + " unknown type") : materialTypes.stream().
                    // Counts each type of material, then for each material type grouping makes a string
                    // of the count and the material type, e.g. "3 Fresh Frozen Blood, 2 Dried Blood"
                    collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream().
                    map(mapEntry -> String.format("%d %s", mapEntry.getValue(), mapEntry.getKey())).
                    collect(Collectors.joining(", "))));
            add(new CustomField(JIRA_DEFINITION_MAP.get("Samples"), StringUtils.join(sampleNames, " ")));
            add(new CustomField(JIRA_DEFINITION_MAP.get("ShipmentCondition"),
                    StringUtils.defaultIfBlank(bean.getShipmentCondition(), "unknown")));
            add(new CustomField(JIRA_DEFINITION_MAP.get("TrackingNumber"),
                    StringUtils.defaultIfBlank(trackingNumber, "unknown")));
            add(new CustomField(JIRA_DEFINITION_MAP.get("RequestingPhysician"),
                    StringUtils.defaultIfBlank(requestingPhysician, "unknown")));
            // Delivery Method is a Jira selection and does not like to be set if value is "None".
            if (StringUtils.isNotBlank(bean.getDeliveryMethod()) && !JIRA_NONE.equals(bean.getDeliveryMethod())) {
                add(new CustomField(bean.getDeliveryMethod(), JIRA_DEFINITION_MAP.get("KitDeliveryMethod")));
            }
            // Receipt Type is a Jira multi-selection and does not like to be set if value is "None".
            if (StringUtils.isNotBlank(bean.getReceiptType()) && !JIRA_NONE.equals(bean.getReceiptType())) {
                // Multiple values are comma-space separated.
                add(new CustomField(bean.getReceiptType().split(", "), JIRA_DEFINITION_MAP.get("ReceiptType")));
            }
        }};

        JiraTicket jiraTicket = null;
        try {
            // Gets the most recent linked RCT ticket for the rack.
            if (rack.getJiraTickets() != null) {
                jiraTicket = rack.getJiraTickets().stream().
                        filter(t -> t.getTicketName().
                                startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                        sorted(Comparator.comparing(JiraTicket::getTicketName).reversed()).
                        findFirst().orElse(null);
            }
            JiraIssue jiraIssue = null;
            String issueStatus = null;

            if (jiraTicket == null) {
                // Makes a new ticket.
                jiraIssue = jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT,
                        userBean.getLoginUserName(), CreateFields.IssueType.RECEIPT, title, customFieldList);
                // Writes the RCT title which for some reason gets ignored in createIssue.
                jiraService.updateIssue(jiraIssue.getKey(), Collections.singleton(titleField));
                // Links RCT ticket to the rack.
                jiraTicket = new JiraTicket(jiraService, jiraIssue.getKey());
                rack.addJiraTicket(jiraTicket);
                bean.getMessageCollection().addInfo("Created " + urlLink(jiraTicket));
            } else {
                // Updates the ticket, possibly from Quarantined to Accessioned.
                jiraIssue = jiraService.getIssue(jiraTicket.getTicketId());
                issueStatus = jiraIssue.getStatus();
                customFieldList.add(titleField);
                jiraService.updateIssue(jiraIssue.getKey(), customFieldList);
                bean.getMessageCollection().addInfo("Updated " + urlLink(jiraTicket));
            }

            // Writes a comment and transition that depends on if samples are known.
            String comment = "|Package Id|" + StringUtils.defaultIfBlank(packageId, "unknown") + "|\n" +
                "|Total Number of Samples|" + tubeBarcodes.size() + "|\n" +
                "|Acknowledgement of Shipping Form|" +
                    StringUtils.defaultIfBlank(bean.getShippingAcknowledgement(), "unknown") + "|\n";
            if (isQuarantined) {
                comment += "Quarantine reason: ";
                if (bean.getMessageCollection().hasErrors()) {
                    comment += StringUtils.join(bean.getMessageCollection().getErrors(), "; ") + "\n";
                } else if (bean.getMessageCollection().hasWarnings()) {
                    comment += StringUtils.join(bean.getMessageCollection().getWarnings(), "; ") + "\n";
                } else {
                    comment += "(unknown)\n";
                }
                comment += String.format(ONLY_RECEIVED, rack.getLabel(), StringUtils.join(tubeBarcodes, " "));
            } else if (ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName().equals(issueStatus)) {
                comment += String.format(REACCESSIONED, StringUtils.join(sampleNames, " "));
            } else {
                comment += String.format(ACCESSIONED, rack.getLabel(), StringUtils.join(sampleNames, " "));
                jiraIssue.postTransition(ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName(), comment);
            }
            jiraIssue.addComment(comment);

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            bean.getMessageCollection().addError(JIRA_PROBLEM, e.toString());
        }
        return jiraTicket;
    }

    private String urlLink(JiraTicket jiraTicket) {
        return "<a id=rctUrl href=" + jiraTicket.getBrowserUrl() + ">" + jiraTicket.getTicketName() + "</a>";
    }

    public UserBean getUserBean() {
        return userBean;
    }
}
