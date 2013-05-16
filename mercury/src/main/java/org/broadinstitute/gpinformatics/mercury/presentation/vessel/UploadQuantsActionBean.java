package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.List;

@UrlBinding(value = "/view/uploadQuants.action")
public class UploadQuantsActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/vessel/upload_quants.jsp";
    public static final String UPLOAD_QUANT = "uploadQuant";

    @Validate(required = true, on = "uploadQuant")
    private FileBean quantSpreadsheet;
    private LabMetric.MetricType quantType;

    public FileBean getQuantSpreadsheet() {
        return quantSpreadsheet;
    }

    public void setQuantSpreadsheet(FileBean quantSpreadsheet) {
        this.quantSpreadsheet = quantSpreadsheet;
    }

    public LabMetric.MetricType getQuantType() {
        return quantType;
    }

    public void setQuantType(LabMetric.MetricType quantType) {
        this.quantType = quantType;
    }

    public List<LabMetric.MetricType> getUploadEnabledMetricTypes(){
        return LabMetric.MetricType.getUploadSupportedMetrics();
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(UPLOAD_QUANT)
    public Resolution uploadQuant() {
        //todo store the quants

        addMessage("Successfully uploaded quant.");
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = UPLOAD_QUANT)
    public void validateNoExistingQuants(ValidationErrors errors) {
        //todo validate that quants don't already exist
    }
}
