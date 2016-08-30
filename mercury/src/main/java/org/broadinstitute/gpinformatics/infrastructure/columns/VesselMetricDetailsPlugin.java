package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available lab metric detail data for each page of a lab vessel search. </br>
 * Headers are built from lab metric run names, oldest to newest, left to right. </br>
 * Because this plugin is cached in the search instance using the class name, the first metric type displayed populates the
 * underlying data set for all ancestor and descendant metrics.
 * Rows are built from cached data for each additional type requested.
 */
public class VesselMetricDetailsPlugin implements ListPlugin {

    public VesselMetricDetailsPlugin() {}

    // These detail columns are displayed for each metric type and run name
    public enum MetricColumn {
        BARCODE("Barcode"),
        POSITION("Position"),
        VALUE("Value"),
        DATE("Date");

        MetricColumn(String displayName){
            this.displayName = displayName;
        }

        private String displayName;

        public String getDisplayName(){
            return displayName;
        }
    }

    // Plugin instance is cached for a search, flag indicates all metric data for vessel list is populated
    private boolean isDataInitialized = false;
    // All cached metrics for the lab vessels mapped by vessel barcode
    private Map<String,Map<LabMetric.MetricType, Set<LabMetric>>> cachedMetricData;

    /**
     * Gathers metric data of interest for a specific metric type and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return LabMetrics data
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for LabMetrics and Decisions of interest.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {

        List<LabVessel> labVesselList = (List<LabVessel>) entityList;

        // First use of cached plugin gets and holds all metric data for each vessel
        if( !isDataInitialized ) {
            initializeData(labVesselList);
        }

        // The data rows for the metric type requested
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        // Headers for metric data of interest for the metric type requested
        Map<String,ConfigurableList.Header> quantHeaders = new LinkedHashMap<>();

        // The metric type of interest is obtained from the search term name
        LabMetric.MetricType metricType = LabMetric.MetricType.getByDisplayName(context.getSearchTerm().getName());

        // Populate rows with any available metrics data.
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = buildMetricsRow( labVessel, metricType, quantHeaders );
            metricRows.add(row);
        }

        // If no metrics were found, need default columns for empty cell placeholders
        if(quantHeaders.isEmpty() ) {
            String mainHeaderName = metricType.getDisplayName();
            for( MetricColumn metricColumn : MetricColumn.values() ) {
                String fullHeaderName = mainHeaderName + " " + metricColumn.getDisplayName();
                quantHeaders.put(fullHeaderName
                        , new ConfigurableList.Header(
                                fullHeaderName,
                                mainHeaderName,
                                metricColumn.getDisplayName()));
            }
        }

        // Append result list headers to search result header display list
        for (ConfigurableList.Header header : quantHeaders.values()) {
            headerGroup.addHeader(header);
        }

        return metricRows;
    }

    /**
     * Populate all metrics data for each vessel in the result list and cache it in this class instance
     * @param labVesselList Search result vessel list
     */
    private void initializeData( List<LabVessel> labVesselList ) {
        cachedMetricData = new HashMap<>();
        for (LabVessel labVessel : labVesselList) {
            cachedMetricData.put(labVessel.getLabel(), labVessel.getMetricsForVesselAndRelatives());
        }
        isDataInitialized = true;
    }

    /**
     * Looks up all metrics of type specified from the cached data for a lab vessel and builds the plugin row.
     * Framework quietly ignores empty cells.
     *
     * @param labVessel The vessel to getmetrics for
     * @param metricType The type as supplied from the user selected plugin column
     * @param quantHeaders The result header list to dynamically add columns to for each metric run and value
     * @return A row populated with any available metrics data for the vessel and type specified
     */
    private ConfigurableList.Row buildMetricsRow(LabVessel labVessel, LabMetric.MetricType metricType, Map<String,ConfigurableList.Header> quantHeaders ) {

        ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

        Map<LabMetric.MetricType, Set<LabMetric>> metricGroups = cachedMetricData.get(labVessel.getLabel());
        Set<LabMetric> metrics = metricGroups.get(metricType);

        if( metrics == null || metrics.isEmpty() ) {
           return row;
        }

        List<LabMetric> metricList = new ArrayList<>();

        // Interested in tubes only
        for( LabMetric labMetric : metrics ) {
            if( labMetric.getLabVessel().getType() != LabVessel.ContainerType.PLATE_WELL ) {
                metricList.add(labMetric);
            }
        }

        // Bail out if there are no metrics (unlikely - tubes accompany plate wells)
        if (CollectionUtils.isEmpty(metricList)) {
            return row;
        }

        // Sort oldest to newest so they display right to left
        Collections.sort( metricList );

        for( LabMetric labMetric : metricList ) {

            String mainHeaderName;
            if( labMetric.getLabMetricRun() == null ) {
                mainHeaderName = metricType.getDisplayName();
            } else {
                mainHeaderName = labMetric.getLabMetricRun().getRunName();
            }

            // Add column headers for this run name, but avoid hammering the hash lookup if headers are there already
            String fullHeaderName = mainHeaderName + " " + MetricColumn.BARCODE.getDisplayName();
            if( !quantHeaders.containsKey(fullHeaderName) ) {
                // Cells shown in order of enum: Barcode - Position - Value - Date
                for( MetricColumn metricColumn : MetricColumn.values() ) {
                    fullHeaderName = mainHeaderName + " " + metricColumn.getDisplayName();
                    quantHeaders.put(fullHeaderName, new ConfigurableList.Header(fullHeaderName, mainHeaderName, metricColumn.getDisplayName()));
                }
            }

            ConfigurableList.Cell resultCell;
            String barcode = labMetric.getLabVessel().getLabel();
            fullHeaderName = mainHeaderName + " " + MetricColumn.BARCODE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), barcode, barcode);
            row.addCell(resultCell);

            String position = labMetric.getVesselPosition();
            fullHeaderName = mainHeaderName + " " + MetricColumn.POSITION.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), position, position);
            row.addCell(resultCell);

            String metricValue = ColumnValueType.TWO_PLACE_DECIMAL.format( labMetric.getValue(), "" ) + " " + labMetric.getUnits().getDisplayName();
            fullHeaderName = mainHeaderName + " " + MetricColumn.VALUE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), metricValue, metricValue);
            row.addCell(resultCell);

            String metricDate = ColumnValueType.DATE_TIME.format( labMetric.getCreatedDate(), "" );
            fullHeaderName = mainHeaderName + " " + MetricColumn.DATE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), metricDate, metricDate);
            row.addCell(resultCell);

        }
        return row;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }

}
