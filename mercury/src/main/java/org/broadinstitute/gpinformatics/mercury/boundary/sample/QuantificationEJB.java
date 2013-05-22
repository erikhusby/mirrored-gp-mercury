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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Stateful
@RequestScoped
public class QuantificationEJB {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private LabMetricParser labMetricParser = new LabMetricParser(labVesselDao);

    public Set<LabMetric> validateQuantsDontExist(InputStream quantSpreadsheet, LabMetric.MetricType metricType) throws ValidationException,
            IOException, InvalidFormatException {
        try {
            List<String> validationErrors = new ArrayList<String>();
            labMetricParser.parseMetrics(quantSpreadsheet, metricType);
            for (LabMetric metric : labMetricParser.getMetrics()) {
                LabVessel labVessel = metric.getLabVessel();
                if (labVessel != null) {
                    for (LabMetric persistedMetric : labVessel.getMetrics()) {
                        if (persistedMetric.getName().equals(metricType)) {
                            validationErrors.add("Lab metric " + metric.getName().getDisplayName()
                                    + " already exists for lab vessel " + metric.getLabVessel().getLabel());
                        }
                    }
                } else {
                    validationErrors.add("Could not find lab vessel for metric.");
                }
            }
            if(validationErrors.size() > 0){
               throw new ValidationException("Error during upload validation : ", validationErrors);
            }
            return labMetricParser.getMetrics();
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e);
        }
    }

    public void storeQuants(Set<LabMetric> labMetrics) {
        List<LabVessel> vessels = new ArrayList<LabVessel>();
        for(LabMetric labMetric : labMetrics){
            labMetric.getLabVessel().addMetric(labMetric);
            vessels.add(labMetric.getLabVessel());
        }
        labVesselDao.persistAll(vessels);
    }
}
