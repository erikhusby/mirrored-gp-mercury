package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

@RequestScoped
@Stateful
/**
 * EJB for Buick manifest sessions used to manage sample registration.
 */
public class ManifestSessionEjb {

    /* package private */ static final String UNASSOCIATED_TUBE_SAMPLE_MESSAGE =
            "The given target sample id is not associated with the given target vessel";
    /* package private */ static final String SAMPLE_NOT_FOUND_MESSAGE = ":: This sample ID is not found.";
    private static final String SAMPLE_NOT_UNIQUE_MESSAGE = ":: This sample ID is not unique in Mercury";
    /* package private */ static final String SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE =
            ":: The sample found is not eligible for clinical work";
    /* package private */ static final String VESSEL_NOT_FOUND_MESSAGE = "::  The target vessel is not found";
    /* package private */ static final String VESSEL_USED_FOR_PREVIOUS_TRANSFER =
            ":: the target vessel has already been used for a tube transfer";
    private ManifestSessionDao manifestSessionDao;

    private ResearchProjectDao researchProjectDao;
    private MercurySampleDao mercurySampleDao;
    private LabVesselDao labVesselDao;

    /**
     * For CDI
     */
    @SuppressWarnings("UnusedDeclaration")
    public ManifestSessionEjb() {
    }

    @Inject
    public ManifestSessionEjb(ManifestSessionDao manifestSessionDao, ResearchProjectDao researchProjectDao,
                              MercurySampleDao mercurySampleDao, LabVesselDao labVesselDao) {
        this.manifestSessionDao = manifestSessionDao;
        this.researchProjectDao = researchProjectDao;
        this.mercurySampleDao = mercurySampleDao;
        this.labVesselDao = labVesselDao;
    }

    /* package private */

    /**
     * Helper method to extract just the file name (without extension) from a file path
     *
     * @param filename  full file path of a file for upload
     * @return  the file name without extension
     */
    String extractPrefixFromFilename(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    /**
     * Encapsulates the logic to process a clinical Manifest file for the purpose of starting the accessioning
     * process for a set of received samples
     *
     * @param researchProjectKey    The business key of an existing research project to which the created
     *                              accessioning session is to be associated
     * @param inputStream           File Input stream that contains the manifest being uploaded
     * @param pathToFile            Full path of the manifest as it was uploaded.  This is used to extract the
     *                              manifest name which will help to identify the accessioning session
     * @param bspUser               represents the user that is initiating the manifest upload
     *
     * @return  the newly created manifest session
     */
    public ManifestSession uploadManifest(String researchProjectKey, InputStream inputStream, String pathToFile,
                                          BSPUserList.QADudeUser bspUser) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject == null) {
            throw new InformaticsServiceException("Research Project '" + researchProjectKey + "' not found");
        }
        String prefix = extractPrefixFromFilename(pathToFile);
        ManifestImportProcessor manifestImportProcessor = new ManifestImportProcessor();
        try {
            // This is deliberately ignoring the unhelpful messages from the parser as appears to be the norm.
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
            List<String> messages = manifestImportProcessor.getMessages();
            if (!CollectionUtils.isEmpty(messages)) {
                String messageText = StringUtils.join(messages, ", ");
                throw new InformaticsServiceException("Error reading manifest file: %s", messageText);
            }
        } catch (ValidationException e) {
            throw new InformaticsServiceException(e);
        } catch (Exception e) {
            throw new InformaticsServiceException(
                    "Error reading manifest file '%s'.  Manifest files must be in the proper Excel format.",
                    e, FilenameUtils.getName(pathToFile));
        }
        Collection<ManifestRecord> manifestRecords = null;
        try {
            manifestRecords = manifestImportProcessor.getManifestRecords();
        } catch (ValidationException e) {
            throw new InformaticsServiceException(e);
        }
        ManifestSession manifestSession = new ManifestSession(researchProject, prefix, bspUser);
        manifestSessionDao.persist(manifestSession);
        manifestSession.addRecords(manifestRecords);
        manifestSession.validateManifest();
        return manifestSession;
    }

    /**
     * Encapsulates the logic to execute a users request to mark a given manifest as being accepted
     *
     * @param manifestSessionId     Database ID of the session which should be accepted
     */
    public void acceptManifestUpload(long manifestSessionId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);
        if (manifestSession == null) {
            throw new InformaticsServiceException("Unrecognized Manifest Session ID: " + manifestSessionId);
        }
        manifestSession.acceptUpload();
        manifestSessionDao.persist(manifestSession);
    }

    /**
     * Encapsulates the logic to provide a user with the status of an accessioning session at a given point in time
     * @param manifestSessionId     Database ID of the session which should be accepted
     */
    public ManifestStatus getSessionStatus(long manifestSessionId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);
        if(manifestSession == null ) {
            throw new InformaticsServiceException("The selected session was not found");
        }

        return manifestSession.generateSessionStatusForClose();

    }

    /**
     * Encapsulates the logic to perform when a user initiates a scan of a source clinical sample against an
     * accessioning session
     * @param manifestSessionId     Database ID of the session which should be accepted
     * @param collaboratorSampleId  sample identifier for a source clinical sample
     */
    public void accessionScan(long manifestSessionId, String collaboratorSampleId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);

        if(manifestSession == null ) {
            throw new InformaticsServiceException("The selected session was not found");
        }
        ManifestRecord manifestRecord =
                manifestSession.getRecordWithMatchingValueForKey(Metadata.Key.SAMPLE_ID, collaboratorSampleId);

        if (manifestRecord == null) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(
                            ManifestSession.SAMPLE_ID_KEY, collaboratorSampleId));
        }

        if (manifestRecord.isQuarantined()) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(ManifestSession.SAMPLE_ID_KEY, collaboratorSampleId));
        }

        if (manifestRecord.getStatus() == ManifestRecord.Status.SCANNED) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_SCAN.formatMessage(ManifestSession.SAMPLE_ID_KEY, collaboratorSampleId));
        }

        manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        manifestSessionDao.persist(manifestSession);
    }

    /**
     * Encapsulates the logic to perform when a user intends to close an accessioning session
     *
     * @param sessionId     Database ID of the session which should be accepted
     */
    public void closeSession(long sessionId) {

        ManifestSession manifestSession = manifestSessionDao.find(sessionId);

        manifestSession.completeSession();

        manifestSessionDao.persist(manifestSession);

    }

    /**
     * Encapsulates the logic needed to validate that a clinical source tube intended to be used for tube transfer
     * is eligible.
     *
     * @param manifestSessionId     Database ID of the session which should be accepted
     * @param sourceForTransfer     sample identifier for the source collaborator sample
     * @return  The record on the session associated with the source identifier
     */
    public ManifestRecord validateSourceTubeForTransfer(long manifestSessionId, String sourceForTransfer) {

        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);

        return manifestSession.findRecordForTransfer(sourceForTransfer);
    }

    public MercurySample validateTargetSample(String targetSampleKey) {
        Collection<MercurySample> targetSamples = mercurySampleDao.findBySampleKey(targetSampleKey);

        if (CollectionUtils.isEmpty(targetSamples)) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET,
                    ManifestSession.SAMPLE_ID_KEY, targetSampleKey, SAMPLE_NOT_FOUND_MESSAGE);
        }

        if (targetSamples.size() > 1) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET,
                    ManifestSession.SAMPLE_ID_KEY, targetSampleKey, SAMPLE_NOT_UNIQUE_MESSAGE);
        }

        MercurySample foundTarget = targetSamples.iterator().next();

        if (foundTarget.getMetadataSource() != MercurySample.MetadataSource.MERCURY) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.SAMPLE_ID_KEY,
                    targetSampleKey, SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE);
        }

        return foundTarget;
    }

    public LabVessel validateTargetSampleAndVessel(String targetSampleKey, String targetVesselLabel) {

        MercurySample foundSample = validateTargetSample(targetSampleKey);

        return findAndValidateTargetVessel(targetVesselLabel, foundSample);
    }

    private LabVessel findAndValidateTargetVessel(String targetVesselLabel, MercurySample foundSample) {
        LabVessel foundVessel = labVesselDao.findByIdentifier(targetVesselLabel);

        if (foundVessel == null) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                    targetVesselLabel, VESSEL_NOT_FOUND_MESSAGE);
        }

        if (foundVessel.hasBeenUsedForClinical()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                    targetVesselLabel, VESSEL_USED_FOR_PREVIOUS_TRANSFER);
        }

        /*
        since the scope of the upload happens just after Initial Tare, there should not be any other transfers.  For
        that reason, searching through the MercurySamples on the vessels instead of getSampleInstancesV2 should be
        sufficient
         */
        for (MercurySample mercurySample : foundVessel.getMercurySamples()) {
            if (mercurySample.equals(foundSample)) {
                return foundVessel;
            }
        }

        throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                targetVesselLabel, "::\n" + UNASSOCIATED_TUBE_SAMPLE_MESSAGE);
    }

    public void transferSample(long manifestSessionId, String sourceCollaboratorSample, String sampleKey,
                               String vesselLabel, BspUser user) {
        ManifestSession session = manifestSessionDao.find(manifestSessionId);

        if (session == null) {
            throw new InformaticsServiceException("The selected session was not found");
        }

        MercurySample targetSample = validateTargetSample(sampleKey);

        LabVessel targetVessel = findAndValidateTargetVessel(vesselLabel, targetSample);

        session.performTransfer(sourceCollaboratorSample, targetSample, targetVessel, user);
    }
}
