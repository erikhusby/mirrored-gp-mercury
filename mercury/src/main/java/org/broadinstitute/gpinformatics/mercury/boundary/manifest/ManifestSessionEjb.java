package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

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
            // TODO the return value of this invocation is ignored here and elsewhere in the code base.
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            throw new InformaticsServiceException(e);
        }
        // TODO ManifestImportProcessor needs to return multiple ManifestRecords.

        ManifestSession manifestSession = new ManifestSession(researchProject, prefix, bspUser);
        // TODO Add manifest records from the spreadsheet parsing above.
        // The flush is required so the validation logic will see all the newly added records from the manifest that
        // was just parsed.
        manifestSessionDao.flush();
        manifestSession.validateManifest();
        return manifestSession;
    }

    public ManifestSession loadManifestSession(long id) {
        return manifestSessionDao.find(id);
    }
}
