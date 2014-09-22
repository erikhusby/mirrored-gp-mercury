package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselMetricPlugin implements ListPlugin {

    public LabVesselMetricPlugin() {}

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        /*
         * Build headers and declare cell variables only for metrics data of interest.
         */
        ConfigurableList.Header hdrInitPico =
                new ConfigurableList.Header(LabMetric.MetricType.INITIAL_PICO.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellInitPico;
        headerGroup.addHeader(hdrInitPico);

        ConfigurableList.Header hdrInitPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.INITIAL_PICO.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellInitPicoDec;
        headerGroup.addHeader(hdrInitPicoDec);

        ConfigurableList.Header hdrFingPico =
                new ConfigurableList.Header(LabMetric.MetricType.FINGERPRINT_PICO.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellFingPico;
        headerGroup.addHeader(hdrFingPico);

        ConfigurableList.Header hdrFingPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.FINGERPRINT_PICO.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellFingPicoDec;
        headerGroup.addHeader(hdrFingPicoDec);

        ConfigurableList.Header hdrShearPico =
                new ConfigurableList.Header(LabMetric.MetricType.SHEARING_PICO.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellShearPico;
        headerGroup.addHeader(hdrShearPico);

        ConfigurableList.Header hdrShearPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.SHEARING_PICO.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellShearPicoDec;
        headerGroup.addHeader(hdrShearPicoDec);

        ConfigurableList.Header hdrPondPico =
                new ConfigurableList.Header(LabMetric.MetricType.POND_PICO.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellPondPico;
        headerGroup.addHeader(hdrPondPico);

        ConfigurableList.Header hdrPondPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.POND_PICO.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellPondPicoDec;
        headerGroup.addHeader(hdrPondPicoDec);

        ConfigurableList.Header hdrCatchPico =
                new ConfigurableList.Header(LabMetric.MetricType.CATCH_PICO.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellCatchPico;
        headerGroup.addHeader(hdrCatchPico);

        ConfigurableList.Header hdrCatchPicoDec =
                new ConfigurableList.Header(LabMetric.MetricType.CATCH_PICO.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellCatchPicoDec;
        headerGroup.addHeader(hdrCatchPicoDec);

        ConfigurableList.Header hdrQpcr =
                new ConfigurableList.Header(LabMetric.MetricType.ECO_QPCR.getDisplayName(), "", "", "");
        ConfigurableList.Cell cellQpcr;
        headerGroup.addHeader(hdrQpcr);

        ConfigurableList.Header hdrQpcrDec =
                new ConfigurableList.Header(LabMetric.MetricType.ECO_QPCR.getDisplayName() + " Decision", "", "", "");
        ConfigurableList.Cell cellQpcrDec;
        headerGroup.addHeader(hdrQpcrDec);


        /*
         * Populate rows with any available metrics data.
         */
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabVesselId().toString() );

            Map<String, Set<LabMetric>> metricGroups = labVessel.getMetricsForVesselAndDescendants();

            for( Set<LabMetric> metrics : metricGroups.values() ) {

                if( metrics.isEmpty() ) {
                    continue;
                }

                cellCatchPico = cellCatchPicoDec = cellFingPico = cellFingPicoDec = cellInitPico
                        = cellInitPicoDec = cellPondPico = cellPondPicoDec = cellShearPico
                        = cellShearPicoDec = cellQpcr = cellQpcrDec = null;

                // Metrics are identical type in each set
                for( LabMetric metric : metrics ) {

                    String value = "";
                    LabMetricDecision decision;
                    switch (metric.getName()){
                        case CATCH_PICO:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellCatchPico = new ConfigurableList.Cell(hdrCatchPico, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellCatchPicoDec =
                                        new ConfigurableList.Cell(hdrCatchPicoDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellCatchPicoDec = new ConfigurableList.Cell(hdrCatchPicoDec, "(None)", "(None)");
                            }
                            break;
                        case FINGERPRINT_PICO:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellFingPico = new ConfigurableList.Cell(hdrFingPico, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellFingPicoDec =
                                        new ConfigurableList.Cell(hdrFingPicoDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellFingPicoDec = new ConfigurableList.Cell(hdrFingPicoDec, "(None)", "(None)");
                            }
                            break;
                        case INITIAL_PICO:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellInitPico = new ConfigurableList.Cell(hdrInitPico, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellInitPicoDec =
                                        new ConfigurableList.Cell(hdrInitPicoDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellInitPicoDec = new ConfigurableList.Cell(hdrInitPicoDec, "(None)", "(None)");
                            }
                            break;
                        case POND_PICO:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellPondPico = new ConfigurableList.Cell(hdrPondPico, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellPondPicoDec =
                                        new ConfigurableList.Cell(hdrPondPicoDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellPondPicoDec = new ConfigurableList.Cell(hdrPondPicoDec, "(None)", "(None)");
                            }
                            break;
                        case SHEARING_PICO:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellShearPico = new ConfigurableList.Cell(hdrShearPico, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellShearPicoDec =
                                        new ConfigurableList.Cell(hdrShearPicoDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellShearPicoDec = new ConfigurableList.Cell(hdrShearPicoDec, "(None)", "(None)");
                            }
                            break;
                        case ECO_QPCR:
                            value = value + metric.getValue().toPlainString() + " " + metric.getUnits().getDisplayName() + " ";
                            cellQpcr = new ConfigurableList.Cell(hdrQpcr, value, value);
                            decision = metric.getLabMetricDecision();
                            if (decision != null) {
                                cellQpcrDec =
                                        new ConfigurableList.Cell(hdrQpcrDec, decision.getDecision().toString(),
                                                decision.getDecision().toString());
                            } else {
                                cellQpcrDec = new ConfigurableList.Cell(hdrQpcrDec, "(None)", "(None)");
                            }
                            break;
                        default:
                            // Ignore any metrics data we're not interested in.
                    }
                }

                // Fill in null data
                if( cellCatchPico == null ) {
                    cellCatchPico = new ConfigurableList.Cell(hdrCatchPico, "", "");
                    cellCatchPicoDec = new ConfigurableList.Cell(hdrCatchPicoDec, "", "");
                }
                if( cellFingPico == null ) {
                    cellFingPico = new ConfigurableList.Cell(hdrFingPico, "", "");
                    cellFingPicoDec = new ConfigurableList.Cell(hdrFingPicoDec, "", "");
                }
                if( cellInitPico == null ) {
                    cellInitPico = new ConfigurableList.Cell(hdrInitPico, "", "");
                    cellInitPicoDec = new ConfigurableList.Cell(hdrInitPicoDec, "", "");
                }
                if( cellPondPico == null ) {
                    cellPondPico = new ConfigurableList.Cell(hdrPondPico, "", "");
                    cellPondPicoDec = new ConfigurableList.Cell(hdrPondPicoDec, "", "");
                }
                if( cellShearPico == null ) {
                    cellShearPico = new ConfigurableList.Cell(hdrShearPico, "", "");
                    cellShearPicoDec = new ConfigurableList.Cell(hdrShearPicoDec, "", "");
                }
                if( cellQpcr == null ) {
                    cellQpcr = new ConfigurableList.Cell(hdrQpcr, "", "");
                    cellQpcrDec = new ConfigurableList.Cell(hdrQpcrDec, "", "");
                }


                row.addCell(cellInitPico);
                row.addCell(cellInitPicoDec);
                row.addCell(cellFingPico);
                row.addCell(cellFingPicoDec);
                row.addCell(cellShearPico);
                row.addCell(cellShearPicoDec);
                row.addCell(cellPondPico);
                row.addCell(cellPondPicoDec);
                row.addCell(cellCatchPico);
                row.addCell(cellCatchPicoDec);
                row.addCell(cellQpcr);
                row.addCell(cellQpcrDec);
            }

            metricRows.add(row);
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull Map<String, Object> context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
