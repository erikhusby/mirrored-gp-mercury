package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabMetricParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@Stateful
@RequestScoped
public class QuantificationEJB {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private LabMetricParser labMetricParser = new LabMetricParser(labVesselDao);

    public void validateQuantsDontExist(InputStream quantSpreadsheet, LabMetric.MetricType metricType) throws ValidationException,
            IOException, InvalidFormatException {
        try {
            labMetricParser.processUploadFile(quantSpreadsheet);
            for (LabMetric metric : labMetricParser.getMetrics()) {
                LabVessel labVessel = metric.getLabVessel();
                if (labVessel != null) {
                    for (LabMetric persistedMetric : labVessel.getMetrics()) {
                        if (persistedMetric.getName().equals(metricType)) {
                            throw new ValidationException("Lab metric " + metric.getName().getDisplayName()
                                    + " already exists for lab vessel " + metric.getLabVessel().getLabel());
                        }
                    }
                } else {
                    throw new ValidationException("Could not find lab vessel for metric.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e);
        }
    }
}
