package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.QuantificationEJB;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@UrlBinding(value = "/view/uploadQuants.action")
public class UploadQuantsActionBean extends CoreActionBean {

    public enum QuantFormat {
        VARIOSKAN("Varioskan"),
        GENERIC("Generic");

        private String displayName;

        QuantFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final String VIEW_PAGE = "/vessel/upload_quants.jsp";
    public static final String UPLOAD_QUANT = "uploadQuant";
    public static final String SAVE_METRICS = "saveMetrics";

    @Inject
    private QuantificationEJB quantEJB;
    @Inject
    private VesselEjb vesselEjb;
    @Inject
    private LabMetricRunDao labMetricRunDao;
    @Inject
    private LabMetricDao labMetricDao;

    @Validate(required = true, on = "uploadQuant")
    private FileBean quantSpreadsheet;
    private LabMetric.MetricType quantType;
    private Set<LabMetric> labMetrics;
    private QuantFormat quantFormat;
    private LabMetricRun labMetricRun;
    private Long labMetricRunId;
    private List<Long> selectedMetrics = new ArrayList<>();
    private String overrideReason;
    private LabMetricDecision.Decision overrideDecision;

    // todo jmt search for previous runs (name, date range)

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labMetricRunId != null) {
            labMetricRun = labMetricRunDao.findById(LabMetricRun.class, labMetricRunId);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(UPLOAD_QUANT)
    public Resolution uploadQuant() {
        switch (quantFormat) {
        case VARIOSKAN:
            break;
        case GENERIC:
            quantEJB.storeQuants(labMetrics);
            break;
        }
        addMessage("Successfully uploaded quant.");
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = UPLOAD_QUANT)
    public void validateNoExistingQuants(ValidationErrors errors) {

        InputStream quantStream = null;

        try {
            quantStream = quantSpreadsheet.getInputStream();
            switch (quantFormat) {
            case VARIOSKAN:
                MessageCollection messageCollection = new MessageCollection();
                labMetricRun = vesselEjb.createVarioskanRun(quantStream, getQuantType(),
                        userBean.getBspUser().getUserId(), messageCollection);
                addMessages(messageCollection);
                break;
            case GENERIC:
                labMetrics = quantEJB.validateQuantsDontExist(quantStream, quantType);
                break;
            }
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

    @HandlesEvent(SAVE_METRICS)
    public Resolution saveMetrics() {
        List<LabMetric> selectedLabMetrics = labMetricDao.findListByList(LabMetric.class, LabMetric_.labMetricId,
                selectedMetrics);
        Date now = new Date();
        for (LabMetric selectedLabMetric : selectedLabMetrics) {
            LabMetricDecision labMetricDecision = selectedLabMetric.getLabMetricDecision();
            if (labMetricDecision.getDecision().isEditable()) {
                labMetricDecision.setDecidedDate(now);
                labMetricDecision.setDeciderUserId(userBean.getBspUser().getUserId());
                labMetricDecision.setDecision(overrideDecision);
                labMetricDecision.setOverrideReason(overrideReason);
            }
        }

        labMetricDao.flush();
        addMessage("Successfully saved metrics.");
        labMetricRun = labMetricRunDao.findById(LabMetricRun.class, labMetricRunId);
        return new ForwardResolution(VIEW_PAGE);
    }

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

    public QuantFormat getQuantFormat() {
        return quantFormat;
    }

    public void setQuantFormat(QuantFormat quantFormat) {
        this.quantFormat = quantFormat;
    }

    public LabMetricRun getLabMetricRun() {
        return labMetricRun;
    }

    public Long getLabMetricRunId() {
        return labMetricRunId;
    }

    public void setLabMetricRunId(Long labMetricRunId) {
        this.labMetricRunId = labMetricRunId;
    }

    public List<Long> getSelectedMetrics() {
        return selectedMetrics;
    }

    public void setSelectedMetrics(List<Long> selectedMetrics) {
        this.selectedMetrics = selectedMetrics;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public LabMetricDecision.Decision getOverrideDecision() {
        return overrideDecision;
    }

    public void setOverrideDecision(LabMetricDecision.Decision overrideDecision) {
        this.overrideDecision = overrideDecision;
    }

    public List<LabMetricDecision.Decision> getEditableDecisions() {
        return LabMetricDecision.Decision.getEditableDecisions();
    }

}
