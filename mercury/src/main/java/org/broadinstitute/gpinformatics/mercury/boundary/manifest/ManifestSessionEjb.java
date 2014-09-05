package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequestScoped
@Stateful
/**
 * EJB for Buick manifest sessions used to manage sample registration.
 */
public class ManifestSessionEjb {

    private ManifestSessionDao manifestSessionDao;

    private ResearchProjectDao researchProjectDao;

    /**
     * For CDI
     */
    @SuppressWarnings("UnusedDeclaration")
    public ManifestSessionEjb() {
    }

    @Inject
    public ManifestSessionEjb(ManifestSessionDao manifestSessionDao, ResearchProjectDao researchProjectDao) {
        this.manifestSessionDao = manifestSessionDao;
        this.researchProjectDao = researchProjectDao;
    }

    /* package private */
    String extractPrefixFromFilename(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

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
        Collection<ManifestRecord> manifestRecords = manifestImportProcessor.getManifestRecords();
        ManifestSession manifestSession = new ManifestSession(researchProject, prefix, bspUser);
        manifestSessionDao.persist(manifestSession);
        manifestSession.addRecords(manifestRecords);
        manifestSession.validateManifest();
        return manifestSession;
    }

    public ManifestSession loadManifestSession(long manifestSessionId) {
        return manifestSessionDao.find(manifestSessionId);
    }

    public void acceptManifestUpload(long manifestSessionId) {
        ManifestSession manifestSession = loadManifestSession(manifestSessionId);
        if (manifestSession == null) {
            throw new InformaticsServiceException("Unrecognized Manifest Session ID: " + manifestSessionId);
        }
        manifestSession.acceptUpload();
    }

    @DaoFree
    public Collection<String> buildErrorMessagesForSession(@Nonnull ManifestSession session) {
        // todo wrap this method up in a transactional one that starts with the session id?
        Set<String> errorMessages = new HashSet<>();
        for (ManifestRecord manifestRecord : session.getRecords()) {
            ManifestRecord.Status status = manifestRecord.getStatus();
            if (isNotDuplicatedSample(session,manifestRecord.getSampleId()) && (status == ManifestRecord.Status.UPLOAD_ACCEPTED || status == ManifestRecord.Status.UPLOADED)) {
                errorMessages.add(ManifestRecord.ErrorStatus.MISSING_SAMPLE.formatMessage(ManifestSession.SAMPLE_ID_KEY,manifestRecord.getSampleId()));
            }
        }
        errorMessages.addAll(buildDuplicateSampleErrorMessages(session));
        return errorMessages;
    }

    /**
     * Returns whether or not the sampleId is a duplicated
     * sample within the given session
     */
    private boolean isNotDuplicatedSample(ManifestSession session, String sampleId) {
        return !session.getDuplicatedSampleIds().contains(sampleId);
    }

    private Collection<String> buildDuplicateSampleErrorMessages(ManifestSession session) {
        Set<String> errorMessages = new HashSet<>();
        for (String duplicateSampleId : session.getDuplicatedSampleIds()) {
            errorMessages.add(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(ManifestSession.SAMPLE_ID_KEY,duplicateSampleId));
        }
        return errorMessages;
    }

    public void accessionScan(long manifestSessionId, String collaboratorSampleId) {
        ManifestSession manifestSession = manifestSessionDao.find(manifestSessionId);
        ManifestRecord manifestRecord =
                manifestSession.getRecordWithMatchingValueForKey(Metadata.Key.SAMPLE_ID, collaboratorSampleId);

        // TODO make the entity type an enum to get rid of these Sample ID literals all over the place.
        if (manifestRecord == null) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(
                            "Sample ID", collaboratorSampleId));
        }

        if (manifestRecord.isQuarantined()) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage("Sample ID", collaboratorSampleId));
        }

        if (manifestRecord.getStatus() == ManifestRecord.Status.SCANNED) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_SCAN.formatMessage("Sample ID", collaboratorSampleId));
        }

        manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
    }
}
