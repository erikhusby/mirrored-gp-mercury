package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalSampleFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Stateful
/**
 * EJB for Buick manifest sessions used to manage sample registration.
 */
public class ManifestSessionEjb {

    public static final String UNASSOCIATED_TUBE_SAMPLE_MESSAGE =
            "The given target sample id is not associated with the given target vessel.";
    static final String SAMPLE_NOT_FOUND_MESSAGE = "You must provide a valid target sample key.";
    public static final String SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE =
            "The sample found is not eligible for clinical work.";
    static final String VESSEL_NOT_FOUND_MESSAGE = "The target vessel was not found.";
    public static final String VESSEL_USED_FOR_PREVIOUS_TRANSFER =
            "The target vessel has already been used for a tube transfer.";
    static final String MANIFEST_SESSION_NOT_FOUND_FORMAT = "Manifest Session '%s' not found";
    public static final String MERCURY_SAMPLE_KEY = "Mercury sample key";
    static final String RESEARCH_PROJECT_NOT_FOUND_FORMAT = "Research Project '%s' not found: ";
    static final String SAMPLE_IDS_ARE_NOT_FOUND_MESSAGE = "Sample ids are not found: ";
    static final String RECEIPT_NOT_FOUND = "Unable to find receipt information: ";

    private ManifestSessionDao manifestSessionDao;

    private ResearchProjectDao researchProjectDao;

    private MercurySampleDao mercurySampleDao;

    private LabVesselDao labVesselDao;

    private UserBean userBean;

    private BSPUserList bspUserList;

    private JiraService jiraService;

    private static Log logger = LogFactory.getLog(ManifestSessionEjb.class);

    /**
     * For CDI.
     */
    @SuppressWarnings("UnusedDeclaration")
    public ManifestSessionEjb() {
    }

    @Inject
    public ManifestSessionEjb(ManifestSessionDao manifestSessionDao, ResearchProjectDao researchProjectDao,
                              MercurySampleDao mercurySampleDao, LabVesselDao labVesselDao, UserBean userBean,
                              BSPUserList bspUserList, JiraService jiraService) {
        this.manifestSessionDao = manifestSessionDao;
        this.researchProjectDao = researchProjectDao;
        this.mercurySampleDao = mercurySampleDao;
        this.labVesselDao = labVesselDao;
        this.userBean = userBean;
        this.bspUserList = bspUserList;
        this.jiraService = jiraService;
    }

    /**
     * Upload a clinical manifest file to begin the accessioning process for a set of received samples.
     *
     * @param researchProjectKey The business key of an existing research project to which the created
     *                           accessioning session is to be associated
     * @param inputStream        File Input stream that contains the manifest being uploaded
     * @param pathToFile         Full path of the manifest as it was uploaded.  This is used to extract the
     *                           manifest name which will help to identify the accessioning session
     * @param fromSampleKit      whether or not the samples are in containers from a Broad sample kit
     *
     * @return the newly created manifest session
     */
    public ManifestSession uploadManifest(String researchProjectKey, InputStream inputStream, String pathToFile,
                                          boolean fromSampleKit) {

        ResearchProject researchProject = findResearchProject(researchProjectKey);
        Collection<ManifestRecord> manifestRecords = importManifestRecords(inputStream);
        return createManifestSession(FilenameUtils.getBaseName(pathToFile), manifestRecords, researchProject,
                fromSampleKit);
    }

    /**
     * Create a new manifest session to begin the accessioning process for a set of received samples.
     *
     * @param manifestSessionName    the name to give the session
     * @param manifestRecords        the records for the session
     * @param researchProject        an existing research project to which the created accessioning session is to be
     *                               associated
     * @param fromSampleKit          whether or not the samples are in containers from a Broad sample kit
     *
     * @return the newly created manifest session
     */
    @Nonnull
    public ManifestSession createManifestSession(String manifestSessionName,
                                                 Collection<ManifestRecord> manifestRecords,
                                                 ResearchProject researchProject, boolean fromSampleKit) {
        ManifestSession manifestSession = new ManifestSession(researchProject, manifestSessionName,
                userBean.getBspUser(), fromSampleKit);
        manifestSession.addRecords(manifestRecords);
        manifestSession.validateManifest();

        // Persist here so an ID will be generated for the ManifestSession.  This ID is used for the
        // ManifestSession's name which is displayed on the UI.
        manifestSessionDao.persist(manifestSession);
        return manifestSession;
    }

    private Collection<ManifestRecord> importManifestRecords(InputStream inputStream) {
        ManifestImportProcessor manifestImportProcessor = new ManifestImportProcessor();
        List<String> messages;
        try {
            messages = manifestImportProcessor.processSingleWorksheet(inputStream);
        } catch (IOException | InvalidFormatException e) {
            throw new InformaticsServiceException(e.getMessage(), e);
        } catch (ValidationException e) {
            messages = e.getValidationMessages();
        }
        if (!messages.isEmpty()) {
            throw new InformaticsServiceException(StringUtils.join(messages, "\n"));
        }

        Collection<ManifestRecord> manifestRecords;
        try {
            manifestRecords = manifestImportProcessor.getManifestRecords();
        } catch (ValidationException e) {
            throw new InformaticsServiceException(StringUtils.join(e.getValidationMessages(), "\n"));
        }
        return manifestRecords;
    }

    private ResearchProject findResearchProject(String researchProjectKey) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject == null) {
            throw new InformaticsServiceException(String.format("Research Project '%s' not found", researchProjectKey));
        }
        if (researchProject.getRegulatoryDesignation() == ResearchProject.RegulatoryDesignation.RESEARCH_ONLY) {
            throw new InformaticsServiceException(
                    String.format("The selected Research Project cannot be used for accessioning " +
                                  "because its regulatory designation is %s.",
                            ResearchProject.RegulatoryDesignation.RESEARCH_ONLY));
        }
        return researchProject;
    }

    private ManifestSession findManifestSession(long manifestSessionId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);
        if (manifestSession == null) {
            throw new InformaticsServiceException(String.format(MANIFEST_SESSION_NOT_FOUND_FORMAT, manifestSessionId));
        }
        return manifestSession;
    }

    /**
     * Mark the specified manifest session as being accepted.
     *
     * @param manifestSessionId Database ID of the session
     */
    public void acceptManifestUpload(long manifestSessionId) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);
        manifestSession.acceptUpload();
    }

    /**
     * Calculate the current status of the specified ManifestSession.
     *
     * @param manifestSessionId Database ID of the manifest session
     *
     * @return the calculated status of the session
     */
    public ManifestStatus getSessionStatus(long manifestSessionId) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);
        return manifestSession.generateSessionStatusForClose();
    }

    /**
     * Scan the sample specified by collaborator barcode for the specified manifest session.
     * @param manifestSessionId    Database ID of the session
     * @param referenceSampleId sample identifier for a source clinical sample
     * @param barcode 2d vessel barcode.  Expected only if accessioning a sample kit
     */
    public void accessionScan(long manifestSessionId, String referenceSampleId, String barcode) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);

        Metadata.Key referenceScanKey =
                (manifestSession.isFromSampleKit())?Metadata.Key.BROAD_SAMPLE_ID:Metadata.Key.SAMPLE_ID;
        manifestSession.accessionScan(referenceSampleId, referenceScanKey);
        if(manifestSession.isFromSampleKit()) {
            manifestSession.getRecordWithMatchingValueForKey(referenceScanKey, referenceSampleId)
                    .addMetadata(Metadata.Key.BROAD_2D_BARCODE, barcode);
        }
    }

    /**
     * Close the specified accessioning session.
     *  @param manifestSessionId Database ID of the session which should be accepted
     *
     */
    public void closeSession(long manifestSessionId) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);
        manifestSession.completeSession();
        Set<String> accessionedSamples = new HashSet<>();
        long disambiguator = 1L;

        for (ManifestRecord record : manifestSession.getNonQuarantinedRecords()) {
            if (record.getStatus() == ManifestRecord.Status.ACCESSIONED) {
                if (manifestSession.isFromSampleKit()) {
                    transferSample(manifestSessionId, record.getValueByKey(Metadata.Key.SAMPLE_ID),
                            record.getSampleId(), record.getValueByKey(Metadata.Key.BROAD_2D_BARCODE), disambiguator++);
                }
                accessionedSamples.add(record.getSampleId());
            }
        }

        if (StringUtils.isNotBlank(manifestSession.getReceiptTicket())) {
            transitionReceiptTicket(manifestSession, accessionedSamples);
        }
    }

    /**
     * Validate that a clinical source tube intended to be used for tube transfer is eligible.
     *
     * @param manifestSessionId Database ID of the session which should be accepted
     * @param sourceForTransfer sample identifier for the source collaborator sample
     *
     * @return The record on the session associated with the source identifier
     */
    public ManifestRecord validateSourceTubeForTransfer(long manifestSessionId, String sourceForTransfer) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);
        return manifestSession.findRecordForTransferByKey(Metadata.Key.SAMPLE_ID, sourceForTransfer);
    }

    /**
     * Encapsulates the logic needed to determine if a given mercury sample can be used for clinical work.
     *
     * @param targetSampleKey The sample Key for the target mercury sample for the tube transfer
     *
     * @return the desired mercury sample if it is both found and eligible
     */
    public MercurySample findAndValidateTargetSample(String targetSampleKey) {
        MercurySample targetSample = mercurySampleDao.findBySampleKey(targetSampleKey);

        // There should be one and only one target sample.
        if (targetSample == null) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET,
                    MERCURY_SAMPLE_KEY, targetSampleKey, SAMPLE_NOT_FOUND_MESSAGE);
        }

        if(!targetSample.canSampleBeUsedForClinical()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSessionEjb.MERCURY_SAMPLE_KEY,
                    targetSample.getSampleKey(), ManifestSessionEjb.SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE);
        }

        return targetSample;
    }

    /**
     * Encapsulates the logic to determine if the combination of a sample and lab vessel can be used in an initial
     * transfer of a collaborators sample for clinical work.
     *
     * @param targetSampleKey   The sample Key for the target mercury sample for the tube transfer
     * @param targetVesselLabel The label of the lab vessel that should be associated with the given mercury sample
     *
     * @return the referenced lab vessel if it is both found and eligible
     */
    public LabVessel findAndValidateTargetSampleAndVessel(String targetSampleKey, String targetVesselLabel) {
        LabVessel foundVessel = labVesselDao.findByIdentifier(targetVesselLabel);
        if (foundVessel == null) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                    targetVesselLabel, VESSEL_NOT_FOUND_MESSAGE);
        }

        MercurySample foundSample = findAndValidateTargetSample(targetSampleKey);
        MercurySample.AccessioningCheckResult canBeAccessioned = foundSample.canSampleBeAccessionedWithTargetVessel(foundVessel);
        if(canBeAccessioned != MercurySample.AccessioningCheckResult.CAN_BE_ACCESSIONED) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                    foundVessel.getLabel(),
                    " " + ((canBeAccessioned == MercurySample.AccessioningCheckResult.TUBE_NOT_ASSOCIATED)?
                            ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE:
                            ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
        }
        return foundVessel;
    }

    /**
     * Encapsulates the logic necessary to informatically mark all relevant entities as having completed the tube
     * transfer process.
     *
     * @param manifestSessionId        Database ID of the session which is affiliated with this transfer
     * @param sourceCollaboratorSample sample identifier for a source clinical sample
     * @param sampleKey                The sample Key for the target mercury sample for the tube transfer
     * @param vesselLabel              The label of the lab vessel that should be associated with the given mercury sample
     */
    public void transferSample(long manifestSessionId, String sourceCollaboratorSample, String sampleKey,
                               String vesselLabel) {
        transferSample(manifestSessionId, sourceCollaboratorSample, sampleKey, vesselLabel, 1L);
    }

    /**
     * Encapsulates the logic necessary to informatically mark all relevant entities as having completed the tube
     * transfer process.
     *
     * @param manifestSessionId        Database ID of the session which is affiliated with this transfer
     * @param sourceCollaboratorSample sample identifier for a source clinical sample
     * @param sampleKey                The sample Key for the target mercury sample for the tube transfer
     * @param vesselLabel              The label of the lab vessel that should be associated with the given mercury sample
     * @param disambiguator            LabEvent disambiguator to avoid unique constraint errors when called in a tight loop
     */
    public void transferSample(long manifestSessionId, String sourceCollaboratorSample, String sampleKey,
                               String vesselLabel, long disambiguator) {
        ManifestSession session = findManifestSession(manifestSessionId);
        MercurySample targetSample = findAndValidateTargetSample(sampleKey);

        LabVessel targetVessel = findAndValidateTargetSampleAndVessel(sampleKey, vesselLabel);

        session.performTransfer(sourceCollaboratorSample, targetSample, targetVessel, userBean.getBspUser(),
                disambiguator);

        if (StringUtils.isNotBlank(session.getReceiptTicket())) {
            try {
                addReceiptEvent(sourceCollaboratorSample, targetSample, targetVessel, session);
            } catch (IOException e) {
                logger.error("Unable to access JIRA receipt information for " + session.getReceiptTicket());
            }
        }
    }

    private void addReceiptEvent(String sourceCollaboratorSample, MercurySample targetSample, LabVessel targetVessel,
                                 ManifestSession session) throws IOException {

        ManifestRecord sourceRecord;

        if (sourceCollaboratorSample != null) {
            sourceRecord = session.findRecordByKey(sourceCollaboratorSample, Metadata.Key.SAMPLE_ID);
        } else {
            sourceRecord = session.findRecordByKey(targetSample.getSampleKey(), Metadata.Key.BROAD_SAMPLE_ID);
        }

        JiraIssue receiptInfo=findJiraIssue(session);

        targetSample.addMetadata(
                Collections.singleton(new Metadata(Metadata.Key.RECEIPT_RECORD, session.getReceiptTicket())));

        int disambiguator = sourceRecord.getSpreadsheetRowNumber();
        BspUser bspUserByUsername = getBspUser(session, receiptInfo);
        targetVessel.setReceiptEvent(bspUserByUsername, receiptInfo.getCreated(), disambiguator,
            buildEventLocationName(session));
    }

    public BspUser getBspUser(ManifestSession session, JiraIssue receiptInfo) {
        BspUser bspUserByUsername = null;
        try {
            bspUserByUsername = bspUserList.getByUsername(receiptInfo.getReporter());
        } catch (IOException e) {
            logger.error("Unable to access JIRA receipt information for " + session.getReceiptTicket());

        } finally {
            if (bspUserByUsername == null) {
                bspUserByUsername = userBean.getBspUser();
                logger.error("The user that created the receipt ticket " + session.getReceiptTicket() +
                             " is not a Mercury user");
            }
        }
        return bspUserByUsername;
    }

    public JiraIssue findJiraIssue(ManifestSession session) {
        JiraIssue receiptInfo;
        BspUser bspUserByUsername=null;
        try {
            receiptInfo = jiraService.getIssueInfo(session.getReceiptTicket());
        } catch (IOException e) {
            throw new TubeTransferException(RECEIPT_NOT_FOUND + session.getReceiptTicket());
        }

        return receiptInfo;
    }

    public String buildEventLocationName(ManifestSession session) {
        return LabEvent.UI_EVENT_LOCATION + " Accessioning--" + session.getSessionName();
    }

    public void transitionReceiptTicket(JiraIssue receiptIssue, ManifestSession session, Set<String> accessionedSamples) {
        String comment = String.format("Session %s associated with Research Project %s has been Accessioned.  "
                                       + "Source samples include: %s", session.getSessionName(),
                session.getResearchProject().getBusinessKey(), StringUtils.join(accessionedSamples, ", "));
        try {
            receiptIssue.postTransition(JiraTransition.ACCESSIONED.getStateName(), comment);
            receiptIssue.addComment(comment);

        } catch (IOException e) {
            logger.error("Unable to transition receipt ticket " + session.getReceiptTicket() + " to Accessioned", e);
        }
    }

    private void transitionReceiptTicket(ManifestSession session, Set<String> accessionedSamples) {
        JiraIssue receiptIssue = new JiraIssue(session.getReceiptTicket(), jiraService);
        transitionReceiptTicket(receiptIssue, session, accessionedSamples);
    }

    /**
     * Creates a new manifest session for the given research project.
     *
     * @param researchProjectKey    the business key of the research project for these samples
     * @param sessionName           the name to give the manifest session
     * @param fromSampleKit         whether or not the samples are in tubes from a Broad sample kit
     * @param samples               Collection of samples to add to the manifest.
     * @return the newly created (and persisted) ManifestSession
     */
    public ManifestSession createManifestSession(String researchProjectKey, String sessionName,
                                                 boolean fromSampleKit, Collection<Sample> samples) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject == null) {
            throw new IllegalArgumentException(String.format(RESEARCH_PROJECT_NOT_FOUND_FORMAT, researchProjectKey));
        }
        ManifestSession manifestSession;
        try {
            validateSamplesAreAvailableForAccessioning(samples);
            Collection<ManifestRecord> manifestRecords = ClinicalSampleFactory.toManifestRecords(samples);
            manifestSession = new ManifestSession(researchProject, sessionName, userBean.getBspUser(), fromSampleKit,
                    manifestRecords);
        } catch (RuntimeException e) {
            throw new InformaticsServiceException(e);
        }
        return manifestSession;
    }

    /**
     * Validate that the collection of samples are available for accessioning.
     */
    private void validateSamplesAreAvailableForAccessioning(Collection<Sample> samples) {
        List<String> sampleIds=new ArrayList<>(samples.size());
        for (Sample sample : samples) {
            Metadata.Key metadataKey = Metadata.Key.BROAD_SAMPLE_ID;
            sampleIds.add(metadataKey.getValueFor(sample));
        }

        Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(sampleIds);

        if(!mercurySampleMap.keySet().containsAll(sampleIds)) {

            List<String> missingSampleIds = new ArrayList<>(sampleIds);
            missingSampleIds.removeAll(mercurySampleMap.keySet());

            throw new InformaticsServiceException(SAMPLE_IDS_ARE_NOT_FOUND_MESSAGE +
                                                  StringUtils.join(missingSampleIds, ", "));
        }
        for (MercurySample mercurySample : mercurySampleMap.values()) {
            if(!mercurySample.canSampleBeUsedForClinical()) {
                throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET,
                        ManifestSessionEjb.MERCURY_SAMPLE_KEY, mercurySample.getSampleKey(),
                        ManifestSessionEjb.SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE);
            }
            for (LabVessel labVessel : mercurySample.getLabVessel()) {
                MercurySample.AccessioningCheckResult accessioningCheckResult =
                        mercurySample.canSampleBeAccessionedWithTargetVessel(labVessel);
                if(accessioningCheckResult != MercurySample.AccessioningCheckResult.CAN_BE_ACCESSIONED) {
                    throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET,
                            ManifestSession.VESSEL_LABEL, labVessel.getLabel(),
                            " " + ((accessioningCheckResult == MercurySample.AccessioningCheckResult.TUBE_NOT_ASSOCIATED)?
                                    ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE:
                                    ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
                }
            }
        }
    }

    public void updateReceiptInfo(ManifestSession session, String receiptKey) throws IOException {
        String oldReceiptKey = session.getReceiptTicket();

        session.setReceiptTicket(receiptKey);
        String sourceBusinessKey = session.getResearchProject().getBusinessKey();

        JiraIssue researchProjectIssue = new JiraIssue(sourceBusinessKey, jiraService);
        researchProjectIssue.updateIssueLink(receiptKey, oldReceiptKey);
    }

    public void updateReceiptInfo(Long manifestSessionId, String receiptKey) throws IOException {
        ManifestSession session = manifestSessionDao.find(manifestSessionId);
        updateReceiptInfo(session, receiptKey);
    }


    /**
     * JIRA Transition states used by Receipt.
     */
    public enum JiraTransition {
        RECEIVED("Create"),
        ACCESSIONED("Accessioned");
        /**
         * The text that represents this transition state in JIRA.
         */
        private final String stateName;

        JiraTransition(String stateName) {
            this.stateName = stateName;
        }

        public String getStateName() {
            return stateName;
        }
    }
}
