package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;
import org.owasp.encoder.Encode;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            ConfigurableList.Row row = buildMetricsRow( labVessel, metricType, quantHeaders
                    , context );
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

        // Reset for next page of data
        isDataInitialized = false;
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
    private ConfigurableList.Row buildMetricsRow(LabVessel labVessel, LabMetric.MetricType metricType, Map<String,ConfigurableList.Header> quantHeaders, SearchContext context ) {

        ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );
        ConfigurableList.Cell resultCell;
        String mainHeaderName;
        String fullHeaderName;
        String barcode;
        String position;
        String metricValueString;
        BigDecimal metricValueNumber = null;
        String metricDate;

        Map<LabMetric.MetricType, Set<LabMetric>> metricGroups = cachedMetricData.get(labVessel.getLabel());
        Set<LabMetric> metrics = metricGroups.get(metricType);

        if( metrics == null || metrics.isEmpty() ) {
           return row;
        }

        Map<Date, LabMetric> sortedMapRunDateToMetric = buildMapRunDateToMetric(metrics);

        // Bail out if there are no metrics (unlikely - tubes accompany plate wells)
        if (CollectionUtils.isEmpty(sortedMapRunDateToMetric.values())) {
            return row;
        }

        // Create a map of each run and the associated metrics because ancestor/descendant metrics
        // can have more than a single metric, for example starting with a pond pico vessel
        // and showing the 8 catch pico metrics in a downstream pool
        // Need to get to cell if more than a single vessel metric is related to an ancestor
        Map<String,List<LabMetric>> metricRunMap = new LinkedHashMap<>();

        for( LabMetric labMetric : sortedMapRunDateToMetric.values() ) {

            if (labMetric.getLabMetricRun() == null) {
                // Concoct a (hopefully) unique header for metrics with no associated run
                // (TODO: Always create run per GPLIM-4339)
                mainHeaderName = metricType.getDisplayName() + " - "
                        + ColumnValueType.DATE.format(labMetric.getCreatedDate(), "");
            } else {
                mainHeaderName = labMetric.getLabMetricRun().getRunName();
            }

            List<LabMetric> multiMetricList = metricRunMap.get(mainHeaderName);
            if( multiMetricList == null ) {
                multiMetricList = new ArrayList<>();
                metricRunMap.put(mainHeaderName, multiMetricList);
            }
            multiMetricList.add(labMetric);

            // Add all column headers for this run name, but avoid hammering the hash lookup if headers are there already
            fullHeaderName = mainHeaderName + " " + MetricColumn.BARCODE.getDisplayName();
            if (!quantHeaders.containsKey(fullHeaderName)) {
                // Cells shown in order of enum: Barcode - Position - Value - Date
                for (MetricColumn metricColumn : MetricColumn.values()) {
                    fullHeaderName = mainHeaderName + " " + metricColumn.getDisplayName();
                    ConfigurableList.Header header
                            = new ConfigurableList.Header(fullHeaderName, mainHeaderName, metricColumn.getDisplayName());
                    quantHeaders.put(fullHeaderName, header);
                }
            }
        }

        // Metrics are aggregated and headers are created, build out cells
        for( Map.Entry<String,List<LabMetric>> metricEntry : metricRunMap.entrySet() ) {

            if( metricEntry.getValue() == null || metricEntry.getValue().isEmpty() ) {
                continue;
            } else if ( metricEntry.getValue().size() == 1 ) {
                LabMetric labMetric = metricEntry.getValue().get(0);
                barcode = Encode.forHtml(labMetric.getLabVessel().getLabel());
                position = labMetric.getVesselPosition();
                metricValueNumber = labMetric.getValue();
                metricValueString = ColumnValueType.TWO_PLACE_DECIMAL.format( labMetric.getValue(), "" )
                        + " " + labMetric.getUnits().getDisplayName();
                metricDate = ColumnValueType.DATE_TIME.format( labMetric.getCreatedDate(), "" );
            } else {
                if( context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB ) {
                    // Use a link to metrics drill down when ancestor or descendant has multiple vessel metrics
                    HashMap<String,String[]> terms = new HashMap<>();
                    List<String> barcodes = new ArrayList<>();
                    // This group will always be associated with a single run (or a null run)
                    String metricRunId = null;
                    for( LabMetric metric : metricEntry.getValue() ){
                        barcodes.add(metric.getLabVessel().getLabel());
                        // If no run, this term will be ignored when value is empty
                        metricRunId = metric.getLabMetricRun() == null ? ""
                                : metric.getLabMetricRun().getLabMetricRunId().toString();
                    }
                    terms.put("Barcode", barcodes.toArray(new String[0]));
                    terms.put("Metric Run ID", new String[]{metricRunId});
                    barcode = SearchDefinitionFactory.buildDrillDownLink("(Multiple Vessel Metrics)"
                            , ColumnEntity.LAB_METRIC
                            , "GLOBAL|GLOBAL_LAB_METRIC_SEARCH_INSTANCES|Metrics by Barcode and Run"
                            , terms, context );
                    position = metricValueString = metricDate = "---";
                } else {
                    // Text for Excel export
                    StringBuilder barcodeAppend = new StringBuilder();
                    StringBuilder positionAppend = new StringBuilder();
                    StringBuilder valueAppend = new StringBuilder();
                    metricDate = null;


                    for( LabMetric labMetric : metricEntry.getValue() ) {
                        barcodeAppend.append(labMetric.getLabVessel().getLabel())
                                .append("\n");
                        positionAppend.append(labMetric.getVesselPosition())
                                .append("\n");
                        valueAppend.append(ColumnValueType.TWO_PLACE_DECIMAL.format( labMetric.getValue(), "" ) + " "
                            + labMetric.getUnits().getDisplayName())
                                .append("\n");
                        // All should be same date
                        metricDate = ColumnValueType.DATE_TIME.format( labMetric.getCreatedDate(), "" );
                    }

                    barcode = Encode.forHtml(barcodeAppend.toString().trim());
                    position = positionAppend.toString().trim();
                    metricValueString = valueAppend.toString().trim();
                }

            }

            fullHeaderName = metricEntry.getKey() + " " + MetricColumn.BARCODE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), barcode, barcode, false);
            row.addCell(resultCell);

            fullHeaderName = metricEntry.getKey() + " " + MetricColumn.POSITION.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), position, position);
            row.addCell(resultCell);

            fullHeaderName = metricEntry.getKey() + " " + MetricColumn.VALUE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName),
                    metricValueNumber == null ? metricValueString : metricValueNumber, metricValueString);
            row.addCell(resultCell);

            fullHeaderName = metricEntry.getKey() + " " + MetricColumn.DATE.getDisplayName();
            resultCell = new ConfigurableList.Cell(quantHeaders.get(fullHeaderName), metricDate, metricDate);
            row.addCell(resultCell);
        }

        return row;
    }

    /**
     * Builds a map from run date to lab metric.  If there is only one metric per run, use it.
     * For multiples: prefer tube to well, else prefer well that has a decision.
     */
    @NotNull
    static TreeMap<Date, LabMetric> buildMapRunDateToMetric(Collection<LabMetric> metrics) {
        return metrics.stream().collect(Collectors.toMap(
                    LabMetric::getCreatedDate,
                    Function.identity(),
                    (existing, replacement) -> {
                        if (replacement.getLabVessel().getType() != LabVessel.ContainerType.PLATE_WELL) {
                            return replacement;
                        }
                        if (replacement.getName().getDecider() != null && replacement.getLabMetricDecision() != null) {
                            return replacement;
                        }
                        return existing;
                    },
                    TreeMap::new));
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }

}
