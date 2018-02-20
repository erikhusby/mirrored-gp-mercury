package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselFingerprintingMetricPlugin implements ListPlugin {

    public LabVesselFingerprintingMetricPlugin() {}

    private enum VALUE_COLUMN_TYPE {
        DNA_PLATE_WELL("DNA Plate Well"),
        Q17_CALL_RATE("Q17 Call Rate"),
        Q17_CALL_RATE_RATIO("Q17 Call Rate Ratio"),
        Q17_CALL_RATE_QC("Q17 Call Rate QC Result"),
        Q20_CALL_RATE("Q20 Call Rate"),
        Q20_CALL_RATE_RATIO("Q20 Call Rate Ratio"),
        Q20_CALL_RATE_QC("Q20 Call Rate QC Result"),
        GENDER("Gender (Fluidigm / Reported)"),
        GENDER_CONCORDANCE_RESULT("Gender Concordance Result"),
        HAPMAP_CONCORDANCE("HapMap Concordance"),
        HAPMAP_CONCORDANCE_QC_RESULT("HapMap Concordance QC Result"),
        SAMPLE_ALIQUOT_ID("Sample Aliquot ID"),
        ROOT_SAMPLE_ID("Root Sample ID"),
        STOCK_SAMPLE_ID("Stock Sample ID"),
        COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID"),
        TRY_COUNT("Try Count"),
        ROX_RAW_INTENSITY_MEAN("ROX Raw Intensity Mean"),
        ROX_RAW_INTENSITY_STD_DEV("ROX Raw Intensity Std Dev"),
        ROX_RAW_INTENSITY_MEDIAN("ROX Raw Intensity Median");

        private String displayName;
        private ConfigurableList.Header resultHeader;

        VALUE_COLUMN_TYPE(String displayName ) {
            this.displayName = displayName;
            this.resultHeader = new ConfigurableList.Header(displayName, displayName, "");
        }

        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getResultHeader() {
            return resultHeader;
        }

    }

    /**
     * Gathers metric data of interest and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities (DNA plate wells) for which to return LabMetrics data
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for LabMetrics and Decisions of interest.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<PlateWell> labVesselList = (List<PlateWell>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);

        // Append headers for metric data of interest.
        for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        Map<StaticPlate, StaticPlate.TubeFormationByWellCriteria.Result> plateResultMap = new HashMap<>();
        for( PlateWell plateWell : labVesselList ) {
            StaticPlate staticPlate = plateWell.getPlate();
            if (!plateResultMap.containsKey(staticPlate)) {
                StaticPlate.TubeFormationByWellCriteria.Result traverserResult =
                        staticPlate.nearestFormationAndTubePositionByWell();
                plateResultMap.put(staticPlate, traverserResult);
            }
            String stockSampleId = null;
            String rootSampleId = null;
            String aliquotSampleId = null;
            String collaboratorParticipantId = null;
            Set<SampleInstanceV2> sampleInstancesV2 = plateWell.getSampleInstancesV2();
            int tryCount = 0;
            for (SampleInstanceV2 sampleInstanceV2: sampleInstancesV2) {
                MercurySample rootSample = sampleInstanceV2.getRootOrEarliestMercurySample();
                aliquotSampleId = sampleInstanceV2.getNearestMercurySampleName();
                if (rootSample != null) {
                    SampleData sampleData = sampleDataFetcher.fetchSampleData(rootSample.getSampleKey());
                    if (sampleData != null) {
                        stockSampleId = sampleData.getStockSample();
                        rootSampleId = sampleData.getRootSample();
                        collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
                    }
                    Set<LabVessel> rootLabVessels = rootSample.getLabVessel();
                    if (!rootLabVessels.isEmpty()) {
                        LabVessel rootLabVessel = rootLabVessels.iterator().next();
                        List<LabEventType> fingerprintingFinalTransferEvents =
                                Collections.singletonList(LabEventType.FINGERPRINTING_IFC_TRANSFER);
                        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria =
                                new TransferTraverserCriteria.VesselForEventTypeCriteria(fingerprintingFinalTransferEvents, true);
                        rootLabVessel.evaluateCriteria(eventTypeCriteria,
                                TransferTraverserCriteria.TraversalDirection.Descendants);
                        for (Map.Entry<LabEvent, Set<LabVessel>> entry: eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
                            Set<LabVessel> targetLabVessels = entry.getKey().getTargetLabVessels();
                            for (LabVessel labVessel: targetLabVessels) {
                                if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                                    StaticPlate chip = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                                    for (PlateWell chipPlateWell: chip.getContainerRole().getContainedVessels()) {
                                        if (!chipPlateWell.getMetrics().isEmpty()) {
                                            tryCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            StaticPlate.TubeFormationByWellCriteria.Result result = plateResultMap.get(staticPlate);
            if (result != null && result.getTubeFormation() != null) {
                Map<VesselPosition, VesselPosition> wellToTubePosition = result.getWellToTubePosition();
                ConfigurableList.Row row = new ConfigurableList.Row(plateWell.getLabel());

                VesselPosition dnaPlateWell = wellToTubePosition.get(plateWell.getVesselPosition());
                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.DNA_PLATE_WELL.getResultHeader(),
                        dnaPlateWell.name(), dnaPlateWell.name()));

                Set<LabMetric> metrics = plateWell.getMetrics();
                for (LabMetric metric: metrics) {
                    switch (metric.getName()) {
                    case CALL_RATE_Q17:
                        parseCallRate(metric, row, VALUE_COLUMN_TYPE.Q17_CALL_RATE.getResultHeader(),
                                VALUE_COLUMN_TYPE.Q17_CALL_RATE_QC.getResultHeader(),
                                VALUE_COLUMN_TYPE.Q17_CALL_RATE_RATIO.getResultHeader());
                        break;
                    case CALL_RATE_Q20:
                        parseCallRate(metric, row, VALUE_COLUMN_TYPE.Q20_CALL_RATE.getResultHeader(),
                                VALUE_COLUMN_TYPE.Q20_CALL_RATE_QC.getResultHeader(),
                                VALUE_COLUMN_TYPE.Q20_CALL_RATE_RATIO.getResultHeader());
                        break;
                    case ROX_SAMPLE_RAW_DATA_MEAN:
                        String meanVal = ColumnValueType.TWO_PLACE_DECIMAL.format(metric.getValue(), "");
                        row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_MEAN.getResultHeader(),
                                meanVal, meanVal));
                        break;
                    case ROX_SAMPLE_RAW_DATA_MEDIAN:
                        String medianVal = ColumnValueType.UNSIGNED.format(metric.getValue(), "");
                        row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_MEDIAN.getResultHeader(),
                                medianVal, medianVal));
                        break;
                    case ROX_SAMPLE_RAW_DATA_STD_DEV:
                        String stdDevVal = ColumnValueType.UNSIGNED.format(metric.getValue(), "");
                        row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_STD_DEV.getResultHeader(),
                                stdDevVal, stdDevVal));
                        break;
                    default:
                    }
                }

                String value = ColumnValueType.UNSIGNED.format(tryCount, "");
                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.TRY_COUNT.getResultHeader(),
                        value, value));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.STOCK_SAMPLE_ID.getResultHeader(),
                        stockSampleId, stockSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.ROOT_SAMPLE_ID.getResultHeader(),
                        rootSampleId, rootSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SAMPLE_ALIQUOT_ID.getResultHeader(),
                        aliquotSampleId, aliquotSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.COLLABORATOR_PARTICIPANT_ID.getResultHeader(),
                        collaboratorParticipantId, collaboratorParticipantId));

                metricRows.add(row);
            }
        }

        return metricRows;
    }

    private void parseCallRate(LabMetric metric, ConfigurableList.Row row, ConfigurableList.Header callRate,
                               ConfigurableList.Header callRateQc,
                               ConfigurableList.Header callRateRatio) {
        String value = ColumnValueType.TWO_PLACE_DECIMAL.format(metric.getValue(), "");
        row.addCell(new ConfigurableList.Cell(callRate, value, value));

        if (metric.getLabMetricDecision() != null) {
            value = metric.getLabMetricDecision().getDecision() == LabMetricDecision.Decision.PASS ? "Pass" : "Fail";
            row.addCell(new ConfigurableList.Cell(callRateQc, value, value));
        }

        Set<Metadata> metadataSet = metric.getMetadataSet();
        Metadata callsMeta = findMetadata(Metadata.Key.CALLS, metadataSet);
        Metadata totalCallsMeta = findMetadata(Metadata.Key.TOTAL_POSSIBLE_CALLS, metadataSet);
        if (callsMeta != null && totalCallsMeta != null) {
            value = callsMeta.getNumberValue().intValue() + "/" + totalCallsMeta.getNumberValue().intValue();
            row.addCell(new ConfigurableList.Cell(callRateRatio, value, value));
        }
    }

    private Metadata findMetadata(Metadata.Key key, Set<Metadata> metadataSet) {
        for (Metadata metadata: metadataSet) {
            if (metadata.getKey() == key) {
                return metadata;
            }
        }
        return null;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
