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
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.sample.SampleLooseVesselProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.SampleParentChildVesselProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.SampleVesselProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * Action bean to allow upload of a spreadsheet with sample IDs and tube barcodes.
 */
@UrlBinding(value = "/sample/SampleVessel.action")
public class SampleVesselActionBean extends CoreActionBean {

    public enum SpreadsheetType {
        PARENT_CHILD("Matrix Rack"),
        LOOSE("Loose Tubes (Cryovials, Vacutainers etc.)");

        private String displayName;

        SpreadsheetType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final String VIEW_PAGE = "/sample/upload_samples.jsp";
    public static final String UPLOAD_SAMPLES = "uploadSamples";

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private SpreadsheetType spreadsheetType;

    @Validate(required = true, on = UPLOAD_SAMPLES)
    private FileBean samplesSpreadsheet;

    @Inject
    private VesselEjb vesselEjb;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    public void setSamplesSpreadsheet(FileBean samplesSpreadsheet) {
        this.samplesSpreadsheet = samplesSpreadsheet;
    }

    public void setSpreadsheetType(SpreadsheetType spreadsheetType) {
        this.spreadsheetType = spreadsheetType;
    }

    @HandlesEvent(UPLOAD_SAMPLES)
    public Resolution uploadQuant() {
        try {
            MessageCollection messageCollection = new MessageCollection();
            SampleVesselProcessor sampleVesselProcessor = null;
            switch (spreadsheetType) {
                case PARENT_CHILD:
                    sampleVesselProcessor = new SampleParentChildVesselProcessor("Sheet1");
                    break;
                case LOOSE:
                    sampleVesselProcessor = new SampleLooseVesselProcessor("Sheet1");
                    break;
            }
            List<LabVessel> labVessels = vesselEjb.createSampleVessels(samplesSpreadsheet.getInputStream(),
                    userBean.getLoginUserName(), messageCollection, sampleVesselProcessor);
            addMessages(messageCollection);
            if (labVessels != null) {
                addMessage("Successfully uploaded " + labVessels.size() + " tubes.");
            }
        } catch (InvalidFormatException | IOException | ValidationException e) {
            addValidationError("samplesSpreadsheet", e.getMessage());
        }
        return new ForwardResolution(VIEW_PAGE);
    }

}
