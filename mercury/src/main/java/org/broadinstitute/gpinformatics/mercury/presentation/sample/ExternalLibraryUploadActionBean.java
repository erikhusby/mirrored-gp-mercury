package org.broadinstitute.gpinformatics.mercury.presentation.sample;

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
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;

import static org.broadinstitute.gpinformatics.mercury.presentation.sample.SampleVesselActionBean.UPLOAD_SAMPLES;

@UrlBinding(value = "/sample/ExternalLibraryUpload.action")
public class ExternalLibraryUploadActionBean extends CoreActionBean {
    private static final String SESSION_LIST_PAGE = "/sample/externalLibraryUpload.jsp";
    private boolean overWriteFlag;

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private FileBean samplesSpreadsheet;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Entry point for initial upload of spreadsheet.
     */
    @HandlesEvent(UPLOAD_SAMPLES)
    public Resolution uploadTubes() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            sampleInstanceEjb.doExternalUpload(samplesSpreadsheet.getInputStream(), overWriteFlag, messageCollection);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public void setSamplesSpreadsheet(FileBean spreadsheet) {
        this.samplesSpreadsheet = spreadsheet;
    }

    public void setOverWriteFlag(boolean overWriteFlag) {
        this.overWriteFlag = overWriteFlag;
    }

    public boolean isOverWriteFlag() {
        return overWriteFlag;
    }
}
