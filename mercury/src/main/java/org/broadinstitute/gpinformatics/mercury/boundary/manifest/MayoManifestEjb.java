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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequestScoped
@Stateful
public class MayoManifestEjb {
    private static Log logger = LogFactory.getLog(MayoManifestEjb.class);
    private static final String FILE_DELIMITER = ",";
    public static final String MISSING_MANIFEST = "Cannot find a manifest for package %s.";
    public static final String RACK_NOT_IN_MANIFEST = "Rack %s is not in the manifest.";
    public static final String TUBE_NOT_IN_MANIFEST = "Some rack scan tubes are not in the manifest: %s";
    public static final String TUBE_NOT_IN_RACKSCAN = "Some manifest tubes are not in the rack scan: %s";
    public static final String WRONG_POSITION = "Tube %s has manifest position %s but rack scan position %s.";
    public static final String UNKNOWN_WELL = "Manifest contains unknown well position \"%s\".";
    public static final String NOT_A_TUBE = "Tube %s matches an existing vessel but it is not a tube.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String ONLY_RACK_RECEIVED = "Received package %s, rack %s.";
    public static final String ONLY_RECEIVED = "Received package, %s rack %s, tubes %s.";
    public static final String ACCESSIONED = "Accessioned package %s, rack %s, samples %s.";
    public static final String REACCESSIONED = "Samples have been re-accessioned: %s";

    public static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    public static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;

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

    public void processScan(MayoReceivingActionBean bean) {
        // Checks if the rack barcode matches an existing vessel. If so, it needs to be a Mayo rack.
        LabVessel existingVessel = labVesselDao.findByIdentifier(bean.getRackBarcode());
        if (existingVessel != null) {
            bean.setPreviousMayoRack(isMayoRack(existingVessel));
            if (!bean.isPreviousMayoRack()) {
                bean.getMessageCollection().addError("Cannot continue. " + bean.getRackBarcode() +
                        " exists in Mercury but is not a Mayo rack.");
            }
        }
        // Checks that the positions are valid.
        if (!bean.getMessageCollection().hasErrors()) {
            String invalidPositions = bean.getRackScan().keySet().stream().
                    filter(position -> VesselPosition.getByName(position) == null).
                    collect(Collectors.joining(", "));
            if (StringUtils.isNotBlank(invalidPositions)) {
                bean.getMessageCollection().addError("Rack scan has unknown position: " + invalidPositions);
            }
        }
        // Checks if the tubes exist. If so they need to be Mayo tubes.
        if (!bean.getMessageCollection().hasErrors()) {
            bean.getRackScanBarcodes().addAll(bean.getRackScan().values().stream().
                    filter(StringUtils::isNotBlank).collect(Collectors.toList()));
            List<LabVessel> existingVessels = labVesselDao.findByListIdentifiers(bean.getRackScanBarcodes());
            if (!existingVessels.isEmpty()) {
                String nonMayoTubes = nonMayoTubes(existingVessels).stream().
                        map(LabVessel::getLabel).
                        collect(Collectors.joining(" "));
                if (!nonMayoTubes.isEmpty()) {
                    bean.getMessageCollection().addError("Cannot continue. Tubes exist in Mercury " +
                            "but are not Mayo tubes: " + nonMayoTubes);
                }
            }
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
                bean.getMessageCollection().addError("Cannot find a rack type for scanned positions " +
                        bean.getRackScan().keySet().stream().sorted().collect(Collectors.joining(", ")));
            }
        }
    }

    public void saveScan(MayoReceivingActionBean bean) {
        if (!bean.getMessageCollection().hasErrors()) {
            if (StringUtils.isBlank(bean.getPackageBarcode())) {
                bean.getMessageCollection().addError("Package barcode is blank.");
            } else if (StringUtils.isBlank(bean.getRackBarcode())) {
                bean.getMessageCollection().addError("Rack barcode is blank.");
            } else if (bean.getRackScanBarcodes().isEmpty()) {
                bean.getMessageCollection().addWarning("Rack scan is blank.");
            } else {
                LabVessel existingRack = labVesselDao.findByIdentifier(bean.getRackBarcode());
                if (existingRack == null) {
                    // If the rack is new but tube barcodes match an existing vessel overwrite must be set.
                    List<LabVessel> existingTubes = labVesselDao.findByListIdentifiers(bean.getRackScanBarcodes());
                    if (!existingTubes.isEmpty()) {
                        String tubeBarcodes = existingTubes.stream().
                                map(LabVessel::getLabel).collect(Collectors.joining(" "));
                        if (!bean.isOverwriteFlag()) {
                            bean.getMessageCollection().addError(
                                    "Overwrite must be selected in order to re-save existing tubes: " + tubeBarcodes);
                        } else {
                            bean.getMessageCollection().addInfo("Existing tubes will be overwritten: " + tubeBarcodes);
                        }
                    }
                } else {
                    // FYI an existing rack will have already been checked that it is a Mayo rack.
                    if (bean.isOverwriteFlag()) {
                        bean.getMessageCollection().addInfo("Rack " + bean.getRackBarcode() +
                                " already exists and will be updated.");
                    } else {
                        bean.getMessageCollection().addError(
                                "Cannot update an existing rack unless overwrite is selected.");
                    }
                }
            }
        }
        if (!bean.getMessageCollection().hasErrors()) {
            receiveAndAccession(bean);
        }
    }

    /**
     * Using package info from the UI, looks up a manifest session, finds the manifest filename,
     * reads the file content from storage, and parses content into a cell grid (either csv or Excel).
     * @return spreadsheet content including headers, or empty list if there is no manifest session or file found.
     */
    public @NotNull List<List<String>> readManifestAsCellGrid(MayoReceivingActionBean bean) {
        List<List<String>> cellGrid = Collections.emptyList();
        ManifestSession manifestSession = lookupManifestSession(bean);
        if (manifestSession == null || manifestSession.getManifestFile() == null) {
            bean.getMessageCollection().addInfo("No manifest file found for package " + bean.getPackageBarcode());
        } else {
            String bucketName = mayoManifestConfig.getBucketName();
            String[] fileAndBucket = manifestSession.getManifestFile().getQualifiedFilename().split(FILE_DELIMITER);
            if (fileAndBucket.length > 1 && bucketName.equals(fileAndBucket[1])) {
                bean.setFilename(fileAndBucket[0]);
                cellGrid = readFileAsCellGrid(fileAndBucket[0], bean.getMessageCollection());
            } else {
                bean.getMessageCollection().addError("Manifest session file " + fileAndBucket[0] +
                        " is not in the configured storage bucket " + bucketName);
            }
        }
        return cellGrid;
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * Errors are put in the MessageCollection.
     */
    private @NotNull List<List<String>> readFileAsCellGrid(String filename, MessageCollection messageCollection) {
        if (StringUtils.isNotBlank(filename)) {
            byte[] spreadsheet = googleBucketDao.download(filename, messageCollection);
            if (spreadsheet != null && spreadsheet.length > 0) {
                return MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, filename, messageCollection);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Does the receipt of rack, or receipt of rack and tubes if rack scan is present, and if manifest
     * is available does a manifest comparison. If no problems found, does sample accession.
     * This method will always make at least an RCT received ticket for package and rack, and
     * persist the empty rack (in order to find the RCT when the rack is processed again).
     * At most it will make an RCT accessioned ticket and persist rack, tubes, and samples.
     */
    private void receiveAndAccession(MayoReceivingActionBean bean) {
        // If the ManifestSession exists, makes maps of sample name to sample metadata.
        Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
        Map<String, VesselPosition> manifestPositions = new HashMap<>();
        Map<String, String> sampleToTube = new HashMap<>();
        Map<String, String> tubeToSample = new HashMap<>();
        ManifestSession manifestSession = lookupManifestSession(bean);
        if (manifestSession != null) {
            for (ManifestRecord manifestRecord : manifestSession.findRecordsByKey(bean.getRackBarcode(),
                    Metadata.Key.BOX_ID)) {
                String sampleName = manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID);
                sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());

                String tubeBarcode = manifestRecord.getValueByKey(Metadata.Key.MATRIX_ID);
                String positionName = manifestRecord.getValueByKey(Metadata.Key.WELL);
                VesselPosition position = VesselPosition.getByName(positionName);
                if (position == null) {
                    bean.getMessageCollection().addError(UNKNOWN_WELL, positionName);
                } else {
                    manifestPositions.put(tubeBarcode, position);
                }
                sampleToTube.put(sampleName, tubeBarcode);
                tubeToSample.put(tubeBarcode, sampleName);
            }
            if (tubeToSample.isEmpty()) {
                bean.getMessageCollection().addError(RACK_NOT_IN_MANIFEST, bean.getRackBarcode());
            }
        } else {
            bean.getMessageCollection().addWarning(MISSING_MANIFEST, bean.getPackageBarcode());
        }

        final Map<String, VesselPosition> reverseRackScan = new HashMap<>();
        Map<String, BarcodedTube> tubeMap = new HashMap<>();
        if (!bean.getMessageCollection().hasErrors()) {
            // Makes a reverse mapped rackScan for the non-blank barcode entries.
            reverseRackScan.putAll(bean.getRackScan().entrySet().stream().
                    filter(mapEntry -> StringUtils.isNotBlank(mapEntry.getValue())).
                    collect(Collectors.toMap(Map.Entry::getValue,
                            mapEntry -> VesselPosition.valueOf(mapEntry.getKey()))));

            // The rack scan tube barcodes and positions must match the manifest, if it exists.
            if (manifestSession != null) {
                if (!CollectionUtils.isEqualCollection(reverseRackScan.keySet(), manifestPositions.keySet())) {
                    String missingFromManifest = StringUtils.join(
                            CollectionUtils.subtract(reverseRackScan.keySet(), manifestPositions.keySet()), " ");
                    if (StringUtils.isNotBlank(missingFromManifest)) {
                        bean.getMessageCollection().addError(TUBE_NOT_IN_MANIFEST, missingFromManifest);
                    }
                    String missingFromRackscan = StringUtils.join(
                            CollectionUtils.subtract(manifestPositions.keySet(), reverseRackScan.keySet()), " ");
                    if (StringUtils.isNotBlank(missingFromRackscan)) {
                        bean.getMessageCollection().addError(TUBE_NOT_IN_RACKSCAN, missingFromRackscan);
                    }
                } else {
                    reverseRackScan.keySet().stream().
                            filter(tube -> !reverseRackScan.get(tube).equals(manifestPositions.get(tube))).
                            forEachOrdered(tube -> bean.getMessageCollection().addError(WRONG_POSITION, tube,
                                    manifestPositions.get(tube), reverseRackScan.get(tube)));
                }
            }
        }
        RackOfTubes rack = null;
        if (!bean.getMessageCollection().hasErrors()) {
            // Finds existing vessels.
            for (LabVessel vessel : labVesselDao.findListByList(LabVessel.class, LabVessel_.label,
                    new ArrayList<String>() {{
                        addAll(reverseRackScan.keySet());
                        add(bean.getRackBarcode());
                    }})) {
                String barcode = vessel.getLabel();
                if (barcode.equals(bean.getRackBarcode())) {
                    if (OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                        rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
                    }
                } else {
                    if (OrmUtil.proxySafeIsInstance(vessel, BarcodedTube.class)) {
                        tubeMap.put(barcode, OrmUtil.proxySafeCast(vessel, BarcodedTube.class));
                    } else {
                        bean.getMessageCollection().addError(NOT_A_TUBE, barcode);
                    }
                }
            }
        }
        List<Object> newEntities = new ArrayList<>();
        if (!bean.getMessageCollection().hasErrors()) {
            // Creates or updates tubes.
            Map<VesselPosition, BarcodedTube> vesselPositionToTube = new HashMap<>();
            for (String barcode : reverseRackScan.keySet()) {
                BarcodedTube tube = tubeMap.get(barcode);
                if (tube == null) {
                    tube = new BarcodedTube(barcode, DEFAULT_TUBE_TYPE);
                    newEntities.add(tube);
                    tubeMap.put(barcode, tube);
                } else {
                    // Tube is being re-uploaded, so clearing old samples is appropriate.
                    tube.getMercurySamples().clear();
                }
                vesselPositionToTube.put(reverseRackScan.get(barcode), tube);
                // Sets the tube's volume and concentration.
                for (Metadata metadata : sampleMetadata.get(tubeToSample.get(barcode))) {
                    if (metadata.getKey() == Metadata.Key.QUANTITY) {
                        tube.setVolume(NumberUtils.toScaledBigDecimal(metadata.getValue()));
                    } else if (metadata.getKey() == Metadata.Key.CONCENTRATION) {
                        tube.setConcentration(NumberUtils.toScaledBigDecimal(metadata.getValue()));
                    }
                }
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

            // If manifest is known, creates or updates samples.
            Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleMetadata.keySet());
            List<String> sampleNames = sampleMap.entrySet().stream().
                    filter(mapEntry -> mapEntry.getValue() != null).
                    map(Map.Entry::getKey).sorted().
                    collect(Collectors.toList());
            for (String sampleName : sampleMetadata.keySet()) {
                MercurySample mercurySample = sampleMap.get(sampleName);
                if (mercurySample == null) {
                    mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.MERCURY);
                    newEntities.add(mercurySample);
                    sampleMap.put(sampleName, mercurySample);
                }
                // Adds new values or replaces existing values, which can be done since samples have MetadataSource.MERCURY.
                mercurySample.updateMetadata(new HashSet<>(sampleMetadata.get(sampleName)));

                // Links sample to tube.
                tubeMap.get(sampleToTube.get(sampleName)).addSample(mercurySample);
            }
            JiraTicket jiraTicket =  makeOrUpdateRct(bean.getPackageBarcode(), rack, reverseRackScan.keySet(),
                    sampleNames, bean.getMessageCollection());
            if (jiraTicket != null) {
                newEntities.add(jiraTicket);
                labVesselDao.persistAll(newEntities);
            }
        }
    }

    /**
     * Returns the ManifestSession associated with the package barcode. If it doesn't already exist,
     * reads new files from manifest file storage and makes new ManifestSessions, and then searches
     * those. Returns null if no matching manifest session found.
     */
    private ManifestSession lookupManifestSession(MayoReceivingActionBean bean) {
        List<ManifestSession> manifestSessions = manifestSessionDao.getSessionsForPackage(bean.getPackageBarcode());
        if (manifestSessions.isEmpty()) {
            // Looks at storage for unprocessed files and makes new Manifest Sessions from them.
            manifestSessions = processNewManifestFiles(bean);
        }
        if (manifestSessions.size() > 1) {
            bean.getMessageCollection().addInfo("Using most recent of " + manifestSessions.size() +
                    " sessions found for package " + bean.getPackageBarcode() + ".");
        }
        return manifestSessions.stream().findFirst().orElse(null);
    }

    /**
     * Queries Google Storage for all new manifest files and persists them as ManifestSessions.
     * Returns manifest sessions that match the bean info, or empty collection if none found.
     */
    private List<ManifestSession> processNewManifestFiles(MayoReceivingActionBean bean) {
        String bucketName = mayoManifestConfig.getBucketName();
        List<String> filenames = googleBucketDao.list(bean.getMessageCollection());
        // Removes the filenames already seen from those found in the bucket.
        // Expects qualifiedFilename = filename + delimiter + bucket name.
        filenames.removeAll(manifestSessionDao.getQualifiedFilenames().stream().
                filter(name -> bucketName.equals(StringUtils.substringAfter(name, FILE_DELIMITER))).
                map(name -> StringUtils.substringBefore(name, FILE_DELIMITER)).
                collect(Collectors.toList()));

        Collection<Object> newEntities = new ArrayList<>();
        List<ManifestSession> manifestSessions = new ArrayList<>();
        // If the file in the storage bucket can be parsed as a manifest file, it made
        // into a new Mercury ManifestSession.
        for (String filename : filenames) {
            ManifestFile manifestFile = new ManifestFile(filename + FILE_DELIMITER + bucketName);
            newEntities.add(manifestFile);
            List<List<String>> cellGrid = readFileAsCellGrid(filename, bean.getMessageCollection());
            if (!cellGrid.isEmpty()) {
                MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
                List<ManifestRecord> manifestRecordsFromFile = processor.makeManifestRecords(cellGrid,
                        filename, bean.getMessageCollection());
                if (CollectionUtils.isNotEmpty(manifestRecordsFromFile)) {
                    ManifestSession manifestSession = new ManifestSession(null, bean.getPackageBarcode(),
                            userBean.getBspUser(), false, manifestRecordsFromFile);
                    manifestSession.setManifestFile(manifestFile);
                    newEntities.add(manifestSession);
                    manifestSessions.add(manifestSession);
                }
            }
        }
        manifestSessionDao.persistAll(newEntities);
        // Sorts manifests by modified date with most recent first.
        return manifestSessions.stream().
                filter(session -> Objects.equals(session.getSessionPrefix(), bean.getPackageBarcode())).
                sorted((a, b) -> b.getUpdateData().getModifiedDate().compareTo(a.getUpdateData().getModifiedDate())).
                distinct().
                collect(Collectors.toList());
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

    /** Returns true if the existing vessel is a rack of tubes and linked to an RCT. */
    private boolean isMayoRack(LabVessel existingVessel) {
        return OrmUtil.proxySafeIsInstance(existingVessel, RackOfTubes.class) &&
                OrmUtil.proxySafeCast(existingVessel, RackOfTubes.class).getJiraTickets().stream().
                        filter(ticket -> ticket.getTicketName().
                                startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                        findFirst().isPresent();
    }

    /** Returns the existing tubes that are not Mayo uploaded tubes. */
    private List<LabVessel> nonMayoTubes(Collection<LabVessel> existingTubes) {
        List<LabVessel> nonMayoTubes = new ArrayList<>();
        for (LabVessel tube : existingTubes) {
            if (!OrmUtil.proxySafeIsInstance(tube, BarcodedTube.class)) {
                nonMayoTubes.add(tube);
            } else {
                // Looks at containing rack(s). If one is a Mayo upload then it's a
                // Mayo upload tube. FYI there won't be a sample linkage if this is
                // a redo of a non-accessioned Mayo upload.
                if (!tube.getContainers().stream().
                        filter(container -> OrmUtil.proxySafeIsInstance(container, TubeFormation.class)).
                        flatMap(container ->
                                OrmUtil.proxySafeCast(container, TubeFormation.class).getRacksOfTubes().stream()).
                        filter(this::isMayoRack).
                        findAny().isPresent()) {
                    nonMayoTubes.add(tube);
                }
            }
        }
        return nonMayoTubes;
    }

    private JiraTicket makeOrUpdateRct(String packageBarcode, RackOfTubes rack, final Collection<String> tubeBarcodes,
            final Collection<String> sampleNames, MessageCollection messages) {

        List<CustomField> customFieldList = new ArrayList<CustomField>() {{
            add(new CustomField(new CustomFieldDefinition("customfield_15660", "Material Type Counts", true),
                    String.valueOf(tubeBarcodes.size())));
            add(new CustomField(new CustomFieldDefinition("customfield_15661", "Shipment Condition", true),
                    "ok"));
            add(new CustomField(new CustomFieldDefinition("customfield_12662", "Sample IDs", false),
                    StringUtils.join(sampleNames, " ")));
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
            JiraIssue jiraIssue;
            String title = String.format("Mayo package %s rack %s", packageBarcode, rack.getLabel());
            String comment;
            String issueStatus = null;
            if (jiraTicket != null) {
                jiraIssue = jiraService.getIssue(jiraTicket.getTicketId());
                jiraService.updateIssue(jiraIssue.getKey(), customFieldList);
                JiraIssue issueInfo = jiraService.getIssueInfo(jiraIssue.getKey());
                issueStatus = issueInfo.getStatus();
                messages.addInfo("Updated " + urlLink(jiraTicket));
            } else {
                jiraIssue = jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT,
                        userBean.getLoginUserName(), CreateFields.IssueType.RECEIPT, title, customFieldList);
                // Writes the RCT title which for some reason gets ignored in the create.
                CustomFieldDefinition summaryDef = new CustomFieldDefinition("summary", "Summary", true);
                jiraService.updateIssue(jiraIssue.getKey(), Collections.singleton(new CustomField(summaryDef, title)));
                // Links RCT ticket to the rack.
                jiraTicket = new JiraTicket(jiraService, jiraIssue.getKey());
                rack.addJiraTicket(jiraTicket);
                messages.addInfo("Created " + urlLink(jiraTicket));
            }

            // Writes a comment and transition that depends on if tubes and samples are known.
            if (sampleNames.isEmpty()) {
                if (tubeBarcodes.isEmpty()) {
                    comment = String.format(ONLY_RACK_RECEIVED, packageBarcode, rack.getLabel());
                } else {
                    comment = String.format(ONLY_RECEIVED, packageBarcode, rack.getLabel(),
                            StringUtils.join(tubeBarcodes, " "));
                }
                // (no transition needed since ticket is created with status of Received)
            } else if (ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName().equals(issueStatus)) {
                comment = String.format(REACCESSIONED, sampleNames.stream().sorted().collect(Collectors.joining(" ")));
                // (no transition, already has status of Accessioned)
            } else {
                comment = String.format(ACCESSIONED, packageBarcode, rack.getLabel(),
                        sampleNames.stream().sorted().collect(Collectors.joining(", ")));
                jiraIssue.postTransition(ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName(), comment);
            }
            jiraIssue.addComment(comment);

        } catch (Exception e) {
            logger.error(String.format(JIRA_PROBLEM, ""), e);
            messages.addError(JIRA_PROBLEM, e.toString());
        }
        return jiraTicket;
    }

    private String urlLink(JiraTicket jiraTicket) {
        return "<a id=rctUrl href=" + jiraTicket.getBrowserUrl() + ">" + jiraTicket.getTicketName() + "</a>";
    }
}
