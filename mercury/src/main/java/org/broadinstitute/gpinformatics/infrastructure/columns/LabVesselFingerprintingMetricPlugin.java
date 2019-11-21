package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FluidigmRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
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
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

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
        GENDER("Fluidigm Gender"),
        REPORTED_GENDER("Reported Gender"),
        GENDER_CONCORDANCE("Gender Concordance"),
        HAPMAP_CONCOORDANCE("HapMap LOD Score"),
        HAPMAP_CONCOORDANCE_QC("HapMap LOD Score QC Result"),
        TRY_COUNT("Try Count"),
        SAMPLE_ALIQUOT_ID("Sample Aliquot ID"),
        ROOT_SAMPLE_ID("Root Sample ID"),
        STOCK_SAMPLE_ID("Stock Sample ID"),
        COLLABORATOR_SAMPLE_ID("Collaborator Sample ID"),
        SAMPLE_TYPE("Sample Type"),
        PARTICIPANT_ID("Participant ID"),
        COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID"),
        PDO("PDO"),
        // todo jmt BSP Work Request
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
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;

        List<ConfigurableList.Row> metricRows = new ArrayList<>();
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);

        // Append headers for metric data of interest.
        for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        Map<StaticPlate, StaticPlate.TubeFormationByWellCriteria.Result> plateResultMap = new HashMap<>();

        Map<String, ConfigurableList.Row> mapPlateWellToRow = new HashMap<>();

        Map<LabVessel, List<LabMetric>> mapLabVesselToMetrics = new HashMap<>();
        for (LabVessel labVessel: labVesselList) {
            LabMetricRun labMetricRun =
                    labVessel.getMostRecentLabMetricRunForType(LabMetric.MetricType.CALL_RATE_Q20);

            mapLabVesselToMetrics = labMetricRun.getLabMetrics().stream()
                    .collect(groupingBy(LabMetric::getLabVessel));
        }

        for (LabVessel labVessel: labVesselList) {

            for (LabMetric labMetric: mapLabVesselToMetrics.get(labVessel)) {

                if (!mapPlateWellToRow.containsKey(labVessel.getLabel())) {
                    ConfigurableList.Row row = new ConfigurableList.Row(labVessel.getLabel());
                    mapPlateWellToRow.put(labVessel.getLabel(), row);
                    metricRows.add(row);
                }
                ConfigurableList.Row row = mapPlateWellToRow.get(labVessel.getLabel());

                if (!OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class)) {
                    throw new RuntimeException("Expect plate wells only for these lab metrics");
                }
                PlateWell plateWell = OrmUtil.proxySafeCast(labVessel, PlateWell.class);
                StaticPlate staticPlate = plateWell.getPlate();
                if (!plateResultMap.containsKey(staticPlate)) {
                    StaticPlate.TubeFormationByWellCriteria.Result traverserResult =
                            staticPlate.nearestFormationAndTubePositionByWell();
                    plateResultMap.put(staticPlate, traverserResult);
                }
                String reportedGender = null;
                // todo jmt the following should use standard result columns, to avoid code repetition
                String stockSampleId = null;
                String rootSampleId = null;
                String aliquotSampleId = null;
                String collaboratorSampleId = null;
                String sampleType = null;
                String patientId = null;
                String collaboratorParticipantId = null;
                String pdo = null;
                Set<SampleInstanceV2> sampleInstancesV2 = plateWell.getSampleInstancesV2();
                int tryCount = 0;
                for (SampleInstanceV2 sampleInstanceV2 : sampleInstancesV2) {
                    MercurySample rootSample = sampleInstanceV2.getRootOrEarliestMercurySample();
                    aliquotSampleId = sampleInstanceV2.getNearestMercurySampleName();
                    ProductOrderSample singleProductOrderSample = sampleInstanceV2.getSingleProductOrderSample();
                    if (singleProductOrderSample != null) {
                        pdo = singleProductOrderSample.getProductOrder().getBusinessKey();
                    }
                    if (rootSample != null) {
                        // todo jmt fetch in bulk?
                        SampleData sampleData = sampleDataFetcher.fetchSampleData(rootSample.getSampleKey());
                        if (sampleData != null) {
                            stockSampleId = sampleData.getStockSample();
                            rootSampleId = sampleData.getRootSample();
                            collaboratorSampleId = sampleData.getCollaboratorsSampleName();
                            sampleType = sampleData.getSampleType();
                            patientId = sampleData.getPatientId();
                            collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
                            reportedGender = sampleData.getGender();
                        }
                        Set<LabVessel> rootLabVessels = rootSample.getLabVessel();
                        if (!rootLabVessels.isEmpty()) {
                            LabVessel rootLabVessel = rootLabVessels.iterator().next();
                            List<LabEventType> fingerprintingFinalTransferEvents =
                                    Collections.singletonList(LabEventType.FINGERPRINTING_IFC_TRANSFER);
                            TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria =
                                    new TransferTraverserCriteria.VesselForEventTypeCriteria(
                                            fingerprintingFinalTransferEvents, true);
                            rootLabVessel.evaluateCriteria(eventTypeCriteria,
                                    TransferTraverserCriteria.TraversalDirection.Descendants);
                            for (Map.Entry<LabEvent, Set<LabVessel>> eventSetEntry : eventTypeCriteria
                                    .getVesselsForLabEventType().entrySet()) {
                                Set<LabVessel> targetLabVessels = eventSetEntry.getKey().getTargetLabVessels();
                                for (LabVessel targetLabVessel : targetLabVessels) {
                                    if (OrmUtil.proxySafeIsInstance(targetLabVessel, StaticPlate.class)) {
                                        StaticPlate chip = OrmUtil.proxySafeCast(targetLabVessel, StaticPlate.class);
                                        for (PlateWell chipPlateWell : chip.getContainerRole().getContainedVessels()) {
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

                // Add non-metric columns
                String value = ColumnValueType.UNSIGNED.format(tryCount, "");
                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.TRY_COUNT.getResultHeader(),
                        value, value));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.STOCK_SAMPLE_ID.getResultHeader(),
                        stockSampleId, stockSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.ROOT_SAMPLE_ID.getResultHeader(),
                        rootSampleId, rootSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SAMPLE_ALIQUOT_ID.getResultHeader(),
                        aliquotSampleId, aliquotSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.COLLABORATOR_SAMPLE_ID.getResultHeader(),
                        collaboratorSampleId, collaboratorSampleId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.SAMPLE_TYPE.getResultHeader(),
                        sampleType, sampleType));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.PARTICIPANT_ID.getResultHeader(),
                        patientId, patientId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.COLLABORATOR_PARTICIPANT_ID.getResultHeader(),
                        collaboratorParticipantId, collaboratorParticipantId));

                row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.PDO.getResultHeader(),
                        pdo, pdo));

                StaticPlate.TubeFormationByWellCriteria.Result result = plateResultMap.get(staticPlate);
                if (result != null && result.getTubeFormation() != null) {
                    Map<VesselPosition, VesselPosition> wellToTubePosition = result.getWellToTubePosition();

                    VesselPosition dnaPlateWell = wellToTubePosition.get(plateWell.getVesselPosition());
                    row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.DNA_PLATE_WELL.getResultHeader(),
                            dnaPlateWell.name(), dnaPlateWell.name()));
                }

                switch (labMetric.getName()) {
                case CALL_RATE_Q17:
                    parseCallRate(labMetric, row, VALUE_COLUMN_TYPE.Q17_CALL_RATE.getResultHeader(),
                            VALUE_COLUMN_TYPE.Q17_CALL_RATE_QC.getResultHeader(),
                            VALUE_COLUMN_TYPE.Q17_CALL_RATE_RATIO.getResultHeader());
                    break;
                case CALL_RATE_Q20:
                    parseCallRate(labMetric, row, VALUE_COLUMN_TYPE.Q20_CALL_RATE.getResultHeader(),
                            VALUE_COLUMN_TYPE.Q20_CALL_RATE_QC.getResultHeader(),
                            VALUE_COLUMN_TYPE.Q20_CALL_RATE_RATIO.getResultHeader());
                    break;
                case ROX_SAMPLE_RAW_DATA_MEAN:
                    String meanVal = ColumnValueType.TWO_PLACE_DECIMAL.format(labMetric.getValue(), "");
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_MEAN.getResultHeader(),
                            labMetric.getValue(), meanVal));
                    break;
                case ROX_SAMPLE_RAW_DATA_MEDIAN:
                    String medianVal = ColumnValueType.UNSIGNED.format(labMetric.getValue(), "");
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_MEDIAN.getResultHeader(),
                            labMetric.getValue(), medianVal));
                    break;
                case ROX_SAMPLE_RAW_DATA_STD_DEV:
                    String stdDevVal = ColumnValueType.UNSIGNED.format(labMetric.getValue(), "");
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.ROX_RAW_INTENSITY_STD_DEV.getResultHeader(),
                            labMetric.getValue(), stdDevVal));
                    break;
                case FLUIDIGM_GENDER:
                    FluidigmRunFactory.Gender gender =
                            FluidigmRunFactory.Gender.getByNumberOfXChromosomes(labMetric.getValue().intValue());
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.GENDER.getResultHeader(), gender.getSymbol(), gender.getSymbol()));
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.REPORTED_GENDER.getResultHeader(), reportedGender, reportedGender));
                    String genderConcordance = Objects.equals(reportedGender, gender.getSymbol()) ? "Pass" : "Fail";
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.GENDER_CONCORDANCE.getResultHeader(), genderConcordance, genderConcordance));
                    break;
                case HAPMAP_CONCORDANCE_LOD:
                    String lodScore = ColumnValueType.TWO_PLACE_DECIMAL.format(labMetric.getValue(), "");
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.HAPMAP_CONCOORDANCE.getResultHeader(), labMetric.getValue(), lodScore));
                    String lodDecision = labMetric.getLabMetricDecision().getDecision() ==
                            LabMetricDecision.Decision.PASS ? "Pass" : "Fail";
                    row.addCell(new ConfigurableList.Cell(
                            VALUE_COLUMN_TYPE.HAPMAP_CONCOORDANCE_QC.getResultHeader(), lodDecision, lodDecision));
                default:
                }
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
