package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabMetricProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Stateful
@RequestScoped
public class QuantificationEJB {

    @Inject
    private LabVesselDao labVesselDao;

    public Set<LabMetric> validateQuantsDontExist(
            InputStream quantSpreadsheet,
            LabMetric.MetricType metricType) throws ValidationException, IOException, InvalidFormatException {
        try {
            List<String> validationErrors = new ArrayList<>();

            // Create a POI backed excel spreadsheet parser to handle this upload.
            LabMetricProcessor labMetricProcessor = new LabMetricProcessor(labVesselDao, metricType);
            PoiSpreadsheetParser.processSingleWorksheet(quantSpreadsheet, labMetricProcessor);

            // Get the metrics that were read in from the spreadsheet.
            Set<LabMetric> labMetrics = labMetricProcessor.getMetrics();

            for (LabMetric metric : labMetrics) {
                LabVessel labVessel = metric.getLabVessel();

                // Do not need to add an error for no lab vessel because the parser will already have that.
                if (labVessel != null) {
                    for (LabMetric persistedMetric : labVessel.getMetrics()) {
                        if (persistedMetric.getName() == metricType) {
                            validationErrors.add("Lab metric " + metric.getName().getDisplayName()
                                                 + " already exists for lab vessel "
                                                 + metric.getLabVessel().getLabel());
                        }
                    }
                }
            }
            for (String message : labMetricProcessor.getMessages()) {
                validationErrors.add(message);
            }
            if (!validationErrors.isEmpty()) {
                throw new ValidationException("Error during upload validation : ", validationErrors);
            }

            return labMetrics;
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e);
        }
    }

    public void storeQuants(Set<LabMetric> labMetrics) {
        List<LabVessel> vessels = new ArrayList<>();
        for (LabMetric labMetric : labMetrics) {
            labMetric.getLabVessel().addMetric(labMetric);
            vessels.add(labMetric.getLabVessel());
        }
        labVesselDao.persistAll(vessels);
    }
}
