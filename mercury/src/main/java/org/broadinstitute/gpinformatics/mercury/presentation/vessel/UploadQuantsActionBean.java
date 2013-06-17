package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.QuantificationEJB;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@UrlBinding(value = "/view/uploadQuants.action")
public class UploadQuantsActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/vessel/upload_quants.jsp";
    public static final String UPLOAD_QUANT = "uploadQuant";

    @Inject
    private QuantificationEJB quantEJB;

    @Validate(required = true, on = "uploadQuant")
    private FileBean quantSpreadsheet;
    private LabMetric.MetricType quantType;
    private Set<LabMetric> labMetrics;

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

    public List<LabMetric.MetricType> getUploadEnabledMetricTypes() {
        return LabMetric.MetricType.getUploadSupportedMetrics();
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(UPLOAD_QUANT)
    public Resolution uploadQuant() {
        quantEJB.storeQuants(labMetrics);
        addMessage("Successfully uploaded quant.");
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = UPLOAD_QUANT)
    public void validateNoExistingQuants(ValidationErrors errors) {

        InputStream quantStream = null;

        try {
            quantStream = quantSpreadsheet.getInputStream();
            labMetrics = quantEJB.validateQuantsDontExist(quantStream, quantType);
        } catch (IOException e) {
            errors.add("quantSpreadsheet", new SimpleError("IO exception while parsing upload. " + e.getMessage()));
        } catch (InvalidFormatException e) {
            errors.add("quantSpreadsheet", new SimpleError("Invalid format exception while parsing upload. " + e.getMessage()));
        } catch (ValidationException e) {
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("Errors parsing uploaded file : <ul>");
            for (String error : e.getValidationMessages()) {
                errorBuilder.append("<li>");
                errorBuilder.append(error);
                errorBuilder.append("</li>");
            }
            errorBuilder.append("</ul>");
            errors.add("quantSpreadsheet", new SimpleError(errorBuilder.toString()));
        } finally {
            IOUtils.closeQuietly(quantStream);

            try {
                quantSpreadsheet.delete();
            } catch (IOException ex) {
                // If cannot delete, oh well.
            }
        }
    }
}
