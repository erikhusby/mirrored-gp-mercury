package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabMetricProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void storeQuants(Set<LabMetric> labMetrics, LabMetric.MetricType quantType,
            MessageCollection messageCollection) {
        List<LabVessel> vessels = new ArrayList<>();
        for (LabMetric labMetric : labMetrics) {
            labMetric.getLabVessel().addMetric(labMetric);
            vessels.add(labMetric.getLabVessel());
        }
        updateRisk(labMetrics, quantType, messageCollection);
        labVesselDao.persistAll(vessels);
    }

    /**
     * Update the risk associated with the ProductOrderSamples.
     */
    public void updateRisk(Set<LabMetric> labMetrics, LabMetric.MetricType quantType,
            MessageCollection messageCollection) {
        Map<ProductOrder, List<ProductOrderSample>> mapPdoToListPdoSamples = new HashMap<>();
        if (quantType == LabMetric.MetricType.INITIAL_PICO) {
            for (LabMetric localLabMetric : labMetrics) {
                if (localLabMetric.getLabMetricDecision() != null) {
                    for (SampleInstanceV2 sampleInstanceV2 : localLabMetric.getLabVessel().getSampleInstancesV2()) {
                        ProductOrderSample singleProductOrderSample = sampleInstanceV2.getSingleProductOrderSample();
                        if (singleProductOrderSample != null) {
                            ProductOrder productOrder = singleProductOrderSample.getProductOrder();
                            List<ProductOrderSample> productOrderSamples =
                                    mapPdoToListPdoSamples.get(productOrder);
                            if (productOrderSamples == null) {
                                productOrderSamples = new ArrayList<>();
                                mapPdoToListPdoSamples.put(productOrder, productOrderSamples);
                            }
                            productOrderSamples.add(singleProductOrderSample);
                        }
                    }
                }
            }
            int calcRiskCount = 0;
            for (Map.Entry<ProductOrder, List<ProductOrderSample>> pdoListPdoSamplesEntry :
                    mapPdoToListPdoSamples.entrySet()) {
                calcRiskCount += pdoListPdoSamplesEntry.getKey().calculateRisk(pdoListPdoSamplesEntry.getValue());
            }
            messageCollection.addInfo(calcRiskCount + " samples are on risk.");
        }
    }
}
