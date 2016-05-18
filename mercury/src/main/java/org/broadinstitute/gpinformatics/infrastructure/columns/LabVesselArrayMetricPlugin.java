package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches available lab metric and decision data for each page of a lab vessel search.
 */
public class LabVesselArrayMetricPlugin implements ListPlugin {

    public LabVesselArrayMetricPlugin() {}

    // Partially from edu.mit.broad.esp.web.stripes.infinium.ViewInfiniumChemPlateActionBean
    private enum VALUE_COLUMN_TYPE {
        CALL_RATE("Call Rate"),
        HET_PCT("Het %"),
        GENDER_CALC("Gender (Infinium / SQNM FP / FLDM FP / Reported)"),
        GENDER_CONCORDANCE("Gender Concordance Result"),
        BIRDSEED_CALL_RATE("Birdseed Call Rate"),
        BIRDSUITE_CALL_RATE("Birdsuite Call Rate"),
        FP_DISCORDANCE("FP Discordance (Inf<->FP)"),
        FP_SNPS("FP SNPs Called (Inf/FP)"),
        HAPMAP_CONCORDANCE_QC("HapMap Concordance QC Result"),
        HAPMAP_CONCORDANCE("HapMap Concordance"),
        CURRENT_STEP("Current Step"),
        STEP_DATE("Date"),
        COMMENTS("Comments");

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

        // Append headers for metric data of interest.
        for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
            headerGroup.addHeader(valueColumnType.getResultHeader());
        }

        if( !LabVesselSearchDefinition.isInfiniumSearch( context ) ) {
            return metricRows;
        }

        // Populate rows with any available metrics data.
        for( LabVessel labVessel : labVesselList ) {

            // This search only applies to DNA plate wells
            if(labVessel.getType() != LabVessel.ContainerType.PLATE_WELL ) {
                continue;
            }

            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

//            TODO JMS Gather array data elements
//            for(Set<[some_data]> data : labVessel.[fetcher.getdata]) {
//                if( [data] != null && ![data].isEmpty() ) {
//                    addMetricsToRow([data], row);
//                }
//            }

            // For now, fill with placeholders
            ConfigurableList.Cell valueCell;
            for( VALUE_COLUMN_TYPE valueColumnType : VALUE_COLUMN_TYPE.values() ){
                valueCell = new ConfigurableList.Cell(valueColumnType.getResultHeader(), "TBD", "TBD");
                row.addCell(valueCell);;
            }

            metricRows.add(row);
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
