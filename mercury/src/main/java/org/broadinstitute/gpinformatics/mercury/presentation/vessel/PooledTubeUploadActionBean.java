package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;

@UrlBinding(value = "/vessel/PooledTubeUpload.action")
public class PooledTubeUploadActionBean extends CoreActionBean {

    public static final String UPLOAD_TUBES = "uploadpooledTubes";
    private static final String SESSION_LIST_PAGE = "/vessel/pooledTubeUpload.jsp";
    private boolean overWriteFlag;

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Validate(required = true, on = UPLOAD_TUBES)
    private FileBean pooledTubesSpreadsheet;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

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

    public void setPooledTubesSpreadsheet(FileBean spreadsheet) {
        this.pooledTubesSpreadsheet = spreadsheet;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }
}


