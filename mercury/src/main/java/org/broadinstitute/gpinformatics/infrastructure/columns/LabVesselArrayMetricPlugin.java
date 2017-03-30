package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcFingerprint;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.InfiniumVesselTraversalEvaluator;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition.CHIP_EVENT_TYPES;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselArrayMetricPlugin implements ListPlugin {

    public LabVesselArrayMetricPlugin() {}

    // Partially from edu.mit.broad.esp.web.stripes.infinium.ViewInfiniumChemPlateActionBean
    private enum VALUE_COLUMN_TYPE {
        CALL_RATE("Call Rate"),
        HET_PCT("Het %"),
        VERSION("Version"),
        AUTOCALL_GENDER("Autocall Gender"),
        FP_GENDER("FP Gender"),
        REPORTED_GENDER("Reported Gender"),
        GENDER_CONCORDANCE_PF("Gender Concordance PF"),
        P95_GREEN("P95 Green"),
        P95_RED("P95 Red"),
        FINGERPRINT_CONCORDANCE("Fingerprint Concordance"),
        HAPLOTYPE_DIFF("Haplotype Difference"),
        HAPMAP_CONCORDANCE("HapMap Concordance");

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
        ArraysQcDao arraysQcDao = ServiceAccessUtility.getBean(ArraysQcDao.class);
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        // Append headers for metric data of interest.
        for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        if( !InfiniumVesselTraversalEvaluator.isInfiniumSearch( context ) ) {
            for (LabVessel labVessel : labVesselList) {
                metricRows.add(new ConfigurableList.Row(labVessel.getLabel()));
            }
            return metricRows;
        }

        // Map vessel parameters to chip wells.
        ArrayList<String> chipWellBarcodes = new ArrayList<>();
        Map<String, String> mapSourceToTargetBarcodes = new HashMap<>();
        for( LabVessel labVessel : labVesselList ) {
            // This search only applies to DNA plate wells
            if(labVessel.getType() != LabVessel.ContainerType.PLATE_WELL ) {
                continue;
            }
            for (Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions :
                    InfiniumVesselTraversalEvaluator.getChipDetailsForDnaWell(labVessel, CHIP_EVENT_TYPES, context).asMap().entrySet()) {
                String label = labVesselAndPositions.getKey().getLabel() + "_" +
                        labVesselAndPositions.getValue().iterator().next();
                chipWellBarcodes.add(label);
                mapSourceToTargetBarcodes.put(labVessel.getLabel(), label);
                break;
            }
        }

        // Fetch metrics, and allow random access.
        List<ArraysQc> arraysQcList = arraysQcDao.findByBarcodes(chipWellBarcodes);
        Map<String, ArraysQc> mapWellBarcodeToMetric = new HashMap<>();
        for (ArraysQc arraysQc : arraysQcList) {
            mapWellBarcodeToMetric.put(arraysQc.getChipWellBarcode(), arraysQc);
        }

        // Populate rows with any available metrics data.
        for( LabVessel labVessel : labVesselList ) {
            ArraysQc arraysQc = mapWellBarcodeToMetric.get(mapSourceToTargetBarcodes.get(labVessel.getLabel()));
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );
            metricRows.add(row);
            if (arraysQc == null) {
                continue;
            }

            String value = ColumnValueType.THREE_PLACE_DECIMAL.format(
                    arraysQc.getCallRate().multiply(BigDecimal.valueOf(100)), "");
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.CALL_RATE.getResultHeader(), value, value));

            value = ColumnValueType.THREE_PLACE_DECIMAL.format(arraysQc.getHetPct().multiply(BigDecimal.valueOf(100)), "");
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.HET_PCT.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getAnalysisVersion());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.VERSION.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getAutocallGender());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.AUTOCALL_GENDER.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getFpGender());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.FP_GENDER.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getReportedGender());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.REPORTED_GENDER.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getGenderConcordancePf());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.GENDER_CONCORDANCE_PF.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getP95Green());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.P95_GREEN.getResultHeader(),
                    value, value));

            value = String.valueOf(arraysQc.getP95Red());
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.P95_RED.getResultHeader(),
                    value, value));

            ArraysQcFingerprint arraysQcFingerprint = null;
            if (!arraysQc.getArraysQcFingerprints().isEmpty()) {
                arraysQcFingerprint = arraysQc.getArraysQcFingerprints().iterator().next();
            }

            if (arraysQcFingerprint == null) {
                value = null;
            } else {
                value = ColumnValueType.THREE_PLACE_DECIMAL.format(arraysQcFingerprint.getLodExpectedSample(), "");
            }
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.FINGERPRINT_CONCORDANCE.getResultHeader(),
                    value, value));

            if (arraysQcFingerprint == null) {
                value = null;
            } else {
                value = String.valueOf(Math.abs(arraysQcFingerprint.getHaplotypesConfidentlyChecked() -
                        arraysQcFingerprint.getHaplotypesConfidentlyMatchin()));
            }
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.HAPLOTYPE_DIFF.getResultHeader(),
                    value, value));

            value = null;
            for (ArraysQcGtConcordance arraysQcGtConcordance: arraysQc.getArraysQcGtConcordances()) {
                if (arraysQcGtConcordance.getVariantType().equals("SNP")) {
                    value = ColumnValueType.THREE_PLACE_DECIMAL.format(
                            arraysQcGtConcordance.getGenotypeConcordance().multiply(BigDecimal.valueOf(100)), "");
                }
            }
            row.addCell(new ConfigurableList.Cell(VALUE_COLUMN_TYPE.HAPMAP_CONCORDANCE.getResultHeader(),
                    value, value));
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
