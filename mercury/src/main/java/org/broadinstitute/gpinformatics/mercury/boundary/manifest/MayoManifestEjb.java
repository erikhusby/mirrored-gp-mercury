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
import java.util.stream.Collectors;

@RequestScoped
@Stateful
public class MayoManifestEjb {
    private static Log logger = LogFactory.getLog(MayoManifestEjb.class);
    public static final String MISSING_MANIFEST = "Cannot find a manifest for package %s. Scanned tubes and rack " +
            "will be saved, but not samples.";
    public static final String RACK_NOT_IN_MANIFEST = "Rack %s is not in the manifest.";
    public static final String TUBE_NOT_IN_MANIFEST = "Some rack scan tubes are not in the manifest: %s";
    public static final String TUBE_NOT_IN_RACKSCAN = "Some manifest tubes are not in the rack scan: %s";
    public static final String WRONG_POSITION = "Tube %s has manifest position %s but rack scan position %s.";
    public static final String UNKNOWN_WELL = "Manifest contains unknown well position \"%s\".";
    public static final String NOT_A_TUBE = "Tube %s matches an existing vessel but it is not a tube.";
    public static final String JIRA_PROBLEM = "Problem creating or updating RCT ticket: %s";
    public static final String ONLY_RECEIVED = "Sample tubes for Mayo package %s have been received, but " +
            "samples could not be accessioned because the manifest was not found. Tube barcodes: %s";
    public static final String ACCESSIONED = "Samples for Mayo package %s have been accessioned: %s";
    public static final String REACCESSIONED = "Samples for Mayo package %s have been re-accessioned: %s";

    public static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    public static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;

    private ManifestSessionDao manifestSessionDao;
    private GoogleBucketDao googleBucketDao;
    private UserBean userBean;
    private LabVesselDao labVesselDao;
    private MercurySampleDao mercurySampleDao;
    private MayoManifestConfig mayoManifestConfig;
    private TubeFormationDao tubeFormationDao;
    private JiraService jiraService;

    /** CDI constructor. */
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
        mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                getConfig(MayoManifestConfig.class, deployment);
        this.googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);
    }

    /**
     * Searches database for an existing ManifestSession associated with the package barcode. If none
     * found, queries Google Storage for the manifest file, parses it and persists the ManifestSession.
     * Errors are put in the MessageCollection.
     * @param redoLookup if true then the Google storage file is re-read.
     */
    public ManifestSession lookupOrMakeManifestSession(String packageBarcode, String filename,
            boolean redoLookup, MessageCollection messages) {

        ManifestSession manifestSession = null;
        List<ManifestSession> manifestSessions = manifestSessionDao.getSessionsForPackage(packageBarcode);
        if (manifestSessions.size() == 1) {
            manifestSession = manifestSessions.iterator().next();
        } else if (manifestSessions.size() > 1) {
            messages.addInfo("Using most recent of " + manifestSessions.size() + " sessions found for package " +
                    packageBarcode + ".");
            manifestSession = manifestSessions.stream().
                    sorted(Comparator.comparingLong(ManifestSession::getManifestSessionId).reversed()).
                    findFirst().get();
        }
        // Reads or re-reads the file from Google storage. If file doesn't exist that is not an error.
        if (manifestSessions.isEmpty() || redoLookup) {
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            Map<String, List<List<String>>> cells = readFileAsCellGrid(filename, messages);
            if (cells != null) {
                List<ManifestRecord> manifestRecordsFromFile = processor.makeManifestRecords(cells, messages);
                if (CollectionUtils.isNotEmpty(manifestRecordsFromFile)) {
                    if (manifestSessions.isEmpty()) {
                        manifestSession = new ManifestSession(null, packageBarcode, userBean.getBspUser(), false,
                                manifestRecordsFromFile);
                        manifestSessionDao.persist(manifestSession);
                    } else if (redoLookup) {
                        // Persist the new version of manifest only if different from the existing one.
                        if (!CollectionUtils.isEqualCollection(
                                processor.flattenedListOfMetadata(manifestSession.getRecords()),
                                processor.flattenedListOfMetadata(manifestRecordsFromFile))) {
                            manifestSession = new ManifestSession(null, packageBarcode, userBean.getBspUser(), false,
                                    manifestRecordsFromFile);
                            manifestSessionDao.persist(manifestSession);
                        }
                    }
                }
            }
        }
        return manifestSession;
    }

    /**
     * Looks for the file in Google Storage using filename and returns the cell data for each sheet.
     * Returns null if the manifest file doesn't exist or could not be read. This is not an error.
     * Errors are put in the MessageCollection.
     */
    public Map<String, List<List<String>>> readFileAsCellGrid(String filename, MessageCollection messages) {
        byte[] spreadsheet = googleBucketDao.download(filename, messages);
        if (spreadsheet != null && spreadsheet.length > 0) {
            return MayoManifestImportProcessor.parseAsCellGrid(spreadsheet, messages);
        }
        return null;
    }

    /**
     * Makes the tubes, rack, RCT ticket, and samples. If the manifest is missing, a warning is issued and
     * tube and rack vessels are persisted, but not the samples, since sample name comes from the manifest.
     */
    public void lookupOrMakeVesselsAndSamples(String packageBarcode, String rackBarcode,
            Map<String, String> rackScan, String filename, boolean overwrite, MessageCollection messages) {

        // If the ManifestSession exists, makes Metadata maps.
        Multimap<String, Metadata> sampleMetadata = HashMultimap.create();
        Map<String, VesselPosition> manifestPositions = new HashMap<>();
        Map<String, String> sampleToTube = new HashMap<>();
        Map<String, String> tubeToSample = new HashMap<>();
        ManifestSession manifestSession = lookupOrMakeManifestSession(packageBarcode, filename, overwrite, messages);
        if (manifestSession != null) {
            for (ManifestRecord manifestRecord : manifestSession.findRecordsByKey(rackBarcode, Metadata.Key.BOX_ID)) {
                String sampleName = manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID);
                sampleMetadata.putAll(sampleName, manifestRecord.getMetadata());

                String tubeBarcode = manifestRecord.getValueByKey(Metadata.Key.MATRIX_ID);
                String positionName = manifestRecord.getValueByKey(Metadata.Key.WELL);
                VesselPosition position = VesselPosition.getByName(positionName);
                if (position == null) {
                    messages.addError(UNKNOWN_WELL, positionName);
                } else {
                    manifestPositions.put(tubeBarcode, position);
                }
                sampleToTube.put(sampleName, tubeBarcode);
                tubeToSample.put(tubeBarcode, sampleName);
            }
            if (tubeToSample.isEmpty()) {
                messages.addError(RACK_NOT_IN_MANIFEST, rackBarcode);
            }
        } else {
            messages.addWarning(MISSING_MANIFEST, packageBarcode);
        }
        if (messages.hasErrors()) {
            return;
        }

        // Makes a reverse mapped rackScan for the non-blank tube barcodes.
        Map<String, VesselPosition> reverseRackScan = rackScan.entrySet().stream().
                filter(mapEntry -> StringUtils.isNotBlank(mapEntry.getValue())).
                collect(Collectors.toMap(Map.Entry::getValue, mapEntry -> VesselPosition.valueOf(mapEntry.getKey())));

        // The rack scan tube barcodes and positions must match the manifest, if it exists.
        if (manifestSession != null) {
            if (!CollectionUtils.isEqualCollection(reverseRackScan.keySet(), manifestPositions.keySet())) {
                String missingFromManifest = StringUtils.join(
                        CollectionUtils.subtract(reverseRackScan.keySet(), manifestPositions.keySet()), " ");
                if (StringUtils.isNotBlank(missingFromManifest)) {
                    messages.addError(TUBE_NOT_IN_MANIFEST, missingFromManifest);
                }
                String missingFromRackscan = StringUtils.join(
                        CollectionUtils.subtract(manifestPositions.keySet(), reverseRackScan.keySet()), " ");
                if (StringUtils.isNotBlank(missingFromRackscan)) {
                    messages.addError(TUBE_NOT_IN_RACKSCAN, missingFromRackscan);
                }
            } else {
                reverseRackScan.keySet().stream().
                        filter(tube -> !reverseRackScan.get(tube).equals(manifestPositions.get(tube))).
                        forEachOrdered(tube -> messages.addError(WRONG_POSITION, tube,
                                manifestPositions.get(tube), reverseRackScan.get(tube)));
            }
        }
        if (messages.hasErrors()) {
            return;
        }

        // Finds existing vessels.
        Map<String, BarcodedTube> tubeMap = new HashMap<>();
        RackOfTubes rack = null;
        for (LabVessel vessel : labVesselDao.findListByList(LabVessel.class, LabVessel_.label,
                new ArrayList<String>() {{
                    addAll(reverseRackScan.keySet());
                    add(rackBarcode);
                }})) {
            String barcode = vessel.getLabel();
            if (barcode.equals(rackBarcode)) {
                if (OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                    rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
                }
            } else {
                if (OrmUtil.proxySafeIsInstance(vessel, BarcodedTube.class)) {
                    tubeMap.put(barcode, OrmUtil.proxySafeCast(vessel, BarcodedTube.class));
                } else {
                    messages.addError(NOT_A_TUBE, barcode);
                }
            }
        }
        if (messages.hasErrors()) {
            return;
        }

        // Creates or updates tubes.
        List<Object> newEntities = new ArrayList<>();
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
        for (String positionName : rackScan.keySet()) {
            positionBarcodeList.add(Pair.of(VesselPosition.getByName(positionName), rackScan.get(positionName)));
        }
        String digest = TubeFormation.makeDigest(positionBarcodeList);
        TubeFormation tubeFormation = tubeFormationDao.findByDigest(digest);
        RackOfTubes.RackType rackType = (rack != null) ? rack.getRackType() : inferRackType(rackScan.keySet());
        if (tubeFormation == null) {
            tubeFormation = new TubeFormation(vesselPositionToTube, rackType);
            newEntities.add(tubeFormation);
        }
        if (rack == null) {
            rack = new RackOfTubes(rackBarcode, rackType);
            newEntities.add(rack);
        }
        tubeFormation.addRackOfTubes(rack);

        // If manifest is known, creates or updates samples.
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleMetadata.keySet());
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

        JiraTicket jiraTicket = makeOrUpdateRct(packageBarcode, rack, reverseRackScan.keySet(), sampleMap.keySet(),
                messages);
        if (jiraTicket != null) {
            newEntities.add(jiraTicket);
            labVesselDao.persistAll(newEntities);
        }
    }

    // Infers the rack type from the vessel positions in the rackScan.
    public RackOfTubes.RackType inferRackType(Collection<String> positionNames) {
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
    public boolean isMayoRack(LabVessel existingVessel) {
        return OrmUtil.proxySafeIsInstance(existingVessel, RackOfTubes.class) &&
                OrmUtil.proxySafeCast(existingVessel, RackOfTubes.class).getJiraTickets().stream().
                        filter(ticket -> ticket.getTicketName().
                                startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                        findFirst().isPresent();
    }

    /** Returns the existing tubes that are not Mayo uploaded tubes. */
    public List<LabVessel> nonMayoTubes(Collection<LabVessel> existingTubes) {
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
                        filter(rack -> isMayoRack(rack)).
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
            jiraTicket = rack.getJiraTickets().stream().
                    filter(t -> t.getTicketName().startsWith(CreateFields.ProjectType.RECEIPT_PROJECT.getKeyPrefix())).
                    sorted(Comparator.comparing(JiraTicket::getTicketName).reversed()).
                    findFirst().orElse(null);
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

            // Writes a comment and transition that depends on if samples are known.
            if (sampleNames.isEmpty()) {
                comment = String.format(ONLY_RECEIVED, packageBarcode, StringUtils.join(tubeBarcodes, ", "));
                // (no transition, already has status of Received)
            } else if (ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName().equals(issueStatus)) {
                comment = String.format(REACCESSIONED, packageBarcode,
                        sampleNames.stream().sorted().collect(Collectors.joining(", ")));
                // (no transition, already has status of Accessioned)
            } else {
                comment = String.format(ACCESSIONED, packageBarcode,
                        sampleNames.stream().sorted().collect(Collectors.joining(", ")));
                jiraIssue.postTransition(ManifestSessionEjb.JiraTransition.ACCESSIONED.getStateName(), comment);
            }
            jiraIssue.addComment(comment);

        } catch (Exception e) {
            logger.error(JIRA_PROBLEM, e);
            messages.addError(JIRA_PROBLEM, e.toString());
        }
        return jiraTicket;
    }

    private String urlLink(JiraTicket jiraTicket) {
        return "<a id=rctUrl href=" + jiraTicket.getBrowserUrl() + ">" + jiraTicket.getTicketName() + "</a>";
    }
}
