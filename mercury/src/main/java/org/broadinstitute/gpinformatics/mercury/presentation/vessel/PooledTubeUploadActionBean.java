package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;


@UrlBinding(value = "/workflow/PooledTubeUpload.action")
public class PooledTubeUploadActionBean extends CoreActionBean {

    public static final String UPLOAD_TUBES = "uploadpooledTubes";
    private static final String SESSION_LIST_PAGE = "/workflow/pooledTubeUpload.jsp";
    private boolean overWriteFlag;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Validate(required = true, on = UPLOAD_TUBES)
    private FileBean pooledTubesSpreadsheet;

    /**
     * Entry point for initial upload of spreadsheet.
     */
    @HandlesEvent(UPLOAD_TUBES)
    public Resolution uploadTubes() {
        try {
            MessageCollection messageCollection = new MessageCollection();

            VesselPooledTubesProcessor vesselSpreadsheetProcessor = new VesselPooledTubesProcessor("Sheet1");

            PoiSpreadsheetParser.processSingleWorksheet(pooledTubesSpreadsheet.getInputStream(), vesselSpreadsheetProcessor);

            sampleInstanceEjb.verifySpreadSheet(vesselSpreadsheetProcessor,messageCollection, overWriteFlag);

            addMessages(messageCollection);

        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public void setPooledTubesSpreadsheet(FileBean spreadsheet) { this.pooledTubesSpreadsheet = spreadsheet; }

    public void setOverWriteFlag(boolean overWriteFlag) { this.overWriteFlag = overWriteFlag; }

}


