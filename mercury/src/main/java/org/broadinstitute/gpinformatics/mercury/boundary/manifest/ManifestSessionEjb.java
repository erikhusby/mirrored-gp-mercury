package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
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

    static final String UNASSOCIATED_TUBE_SAMPLE_MESSAGE =
            "The given target sample id is not associated with the given target vessel";
    static final String SAMPLE_NOT_FOUND_MESSAGE = ":: This sample ID is not found.";
    private static final String SAMPLE_NOT_UNIQUE_MESSAGE = ":: This sample ID is not unique in Mercury";
    static final String SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE =
            ":: The sample found is not eligible for clinical work";
    static final String VESSEL_NOT_FOUND_MESSAGE = "::  The target vessel is not found";
    static final String VESSEL_USED_FOR_PREVIOUS_TRANSFER =
            ":: the target vessel has already been used for a tube transfer";
    private ManifestSessionDao manifestSessionDao;

    private ResearchProjectDao researchProjectDao;
    private MercurySampleDao mercurySampleDao;
    private LabVesselDao labVesselDao;

    /**
     * For CDI.
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

    /**
     * Upload a clinical manifest file to begin the accessioning process for a set of received samples.
     *
     * @param researchProjectKey The business key of an existing research project to which the created
     *                           accessioning session is to be associated
     * @param inputStream        File Input stream that contains the manifest being uploaded
     * @param pathToFile         Full path of the manifest as it was uploaded.  This is used to extract the
     *                           manifest name which will help to identify the accessioning session
     * @param bspUser            represents the user that is initiating the manifest upload
     *
     * @return the newly created manifest session
     */
    public ManifestSession uploadManifest(String researchProjectKey, InputStream inputStream, String pathToFile,
                                          BSPUserList.QADudeUser bspUser) {

        ResearchProject researchProject = findResearchProject(researchProjectKey);
        ManifestImportProcessor manifestImportProcessor = new ManifestImportProcessor();

        try {
            List<String> messages = manifestImportProcessor.processSingleWorksheet(inputStream);

            if (!messages.isEmpty()) {
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
        Collection<ManifestRecord> manifestRecords;
        try {
            manifestRecords = manifestImportProcessor.getManifestRecords();
        } catch (ValidationException e) {
            throw new InformaticsServiceException(e);
        }
        ManifestSession manifestSession = new ManifestSession(researchProject, pathToFile, bspUser);
        // Persist here so an ID will be generated for the ManifestSession.  This ID is used for the
        // ManifestSession's name which is displayed on the UI.
        manifestSessionDao.persist(manifestSession);
        manifestSession.addRecords(manifestRecords);
        manifestSession.validateManifest();
        return manifestSession;
    }

    private ResearchProject findResearchProject(String researchProjectKey) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject == null) {
            throw new InformaticsServiceException(String.format("Research Project '%s' not found", researchProjectKey));
        }
        return researchProject;
    }

    private ManifestSession findManifestSession(long manifestSessionId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);
        if (manifestSession == null) {
            throw new InformaticsServiceException(String.format("Manifest Session '%s' not found", manifestSessionId));
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
     *
     * @param manifestSessionId    Database ID of the session
     * @param collaboratorSampleId sample identifier for a source clinical sample
     */
    public void accessionScan(long manifestSessionId, String collaboratorSampleId) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);

        manifestSession.accessionScan(collaboratorSampleId);
    }

    /**
     * Close the specified accessioning session.
     *
     * @param manifestSessionId Database ID of the session which should be accepted
     */
    public void closeSession(long manifestSessionId) {
        ManifestSession manifestSession = findManifestSession(manifestSessionId);
        manifestSession.completeSession();
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
        return manifestSession.findRecordForTransfer(sourceForTransfer);
    }

    /**
     * Encapsulates the logic needed to determine if a given mercury sample can be used for clinical work.
     *
     * @param targetSampleKey The sample Key for the target mercury sample for the tube transfer
     *
     * @return the desired mercury sample if it is both found and eligible
     */
    public MercurySample validateTargetSample(String targetSampleKey) {
        Collection<MercurySample> targetSamples = mercurySampleDao.findBySampleKey(targetSampleKey);

        // There should be one and only one target sample.
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

    /**
     * Encapsulates the logic to determine if the combination of a sample and lab vessel can be used in an initial
     * transfer of a collaborators sample for clinical work.
     *
     * @param targetSampleKey   The sample Key for the target mercury sample for the tube transfer
     * @param targetVesselLabel The label of the lab vessel that should be associated with the given mercury sample
     *
     * @return the referenced lab vessel if it is both found and eligible
     */
    public LabVessel validateTargetSampleAndVessel(String targetSampleKey, String targetVesselLabel) {

        MercurySample foundSample = validateTargetSample(targetSampleKey);
        return findAndValidateTargetVessel(targetVesselLabel, foundSample);
    }

    /**
     * Helper method to determine target vessel and Sample viability.  Extracts the logic of finding the lab vessel
     * to make this method available for re-use.
     * <p/>
     * {@link #validateTargetSampleAndVessel(String, String)}
     *
     * @param targetVesselLabel The label of the lab vessel that should be associated with the given mercury sample
     * @param foundSample       The target mercury sample for the tube transfer
     *
     * @return the referenced lab vessel if it is both found and eligible
     */
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
        // Since upload happens just after Initial Tare, there should not be any other transfers.  For that reason,
        // searching through the MercurySamples on the vessels instead of getSampleInstancesV2 should be sufficient.
        for (MercurySample mercurySample : foundVessel.getMercurySamples()) {
            if (mercurySample.equals(foundSample)) {
                return foundVessel;
            }
        }
        throw new TubeTransferException(ManifestRecord.ErrorStatus.INVALID_TARGET, ManifestSession.VESSEL_LABEL,
                targetVesselLabel, "::\n" + UNASSOCIATED_TUBE_SAMPLE_MESSAGE);
    }

    /**
     * Encapsulates the logic necessary to informatically mark all relevant entities as having completed the tube
     * transfer process.
     *
     * @param manifestSessionId        Database ID of the session which is affiliated with this transfer
     * @param sourceCollaboratorSample sample identifier for a source clinical sample
     * @param sampleKey                The sample Key for the target mercury sample for the tube transfer
     * @param vesselLabel              The label of the lab vessel that should be associated with the given mercury sample
     * @param user                     represents the user that is initiating the manifest upload
     */
    public void transferSample(long manifestSessionId, String sourceCollaboratorSample, String sampleKey,
                               String vesselLabel, BspUser user) {
        ManifestSession session = findManifestSession(manifestSessionId);
        MercurySample targetSample = validateTargetSample(sampleKey);

        LabVessel targetVessel = findAndValidateTargetVessel(vesselLabel, targetSample);
        session.performTransfer(sourceCollaboratorSample, targetSample, targetVessel, user);
    }
}
