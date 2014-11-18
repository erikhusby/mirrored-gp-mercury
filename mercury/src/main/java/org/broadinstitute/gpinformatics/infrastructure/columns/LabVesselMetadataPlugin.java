package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available sample metadata for each page of a lab vessel search.
 */
public class LabVesselMetadataPlugin implements ListPlugin {

    public LabVesselMetadataPlugin() {}

    // Build headers for all sample metadata.
    private static Map<Metadata.Key, ConfigurableList.Header> METADATA_HEADERS = new LinkedHashMap<>();
    static {
        for( Metadata.Key key : Metadata.Key.values() ) {
            if(key.getCategory() == Metadata.Category.SAMPLE ) {
                METADATA_HEADERS
                        .put(key, new ConfigurableList.Header(key.getDisplayName(), key.getDisplayName(), "", ""));
            }
        }
    }

    /**
     * Gathers sample metadata from current and ancestor lab vessels and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return sample metadata
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for sample metadata.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull Map<String, Object> context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        // Append sample metadata headers to report header group.
        for(Map.Entry<Metadata.Key, ConfigurableList.Header> valueHeaderEntry: METADATA_HEADERS.entrySet() ){
            headerGroup.addHeader(valueHeaderEntry.getValue());
        }

        // Populate rows with any available sample metadata.
        StringBuilder valueHolder = new StringBuilder();
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

            // Collect cell values for each metadata type in case multiple samples in vessel ancestry
            Map<Metadata.Key,List<String>> rowData = buildRowDataHolder();

            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
                Set<Metadata> metadata = sample.getMetadata();
                if( metadata != null && !metadata.isEmpty() ) {
                    addMetadataToRowData( metadata, rowData );
                }
            }

            // Ancestor sample metadata collected, create and append row cells
            for(  Map.Entry<Metadata.Key,List<String>> rowDataEntry : rowData.entrySet() ){
                // Re-use builder
                valueHolder.setLength(0);

                for( String value : rowDataEntry.getValue() ) {
                    valueHolder.append(value).append(' ');
                }

                ConfigurableList.Cell cell = new ConfigurableList.Cell(
                        METADATA_HEADERS.get(rowDataEntry.getKey()), valueHolder.toString(), valueHolder.toString());
                row.addCell(cell);
            }

            metricRows.add(row);
        }

        return metricRows;
    }

    /**
     * Appends sample metadata values to the sample row. Framework quietly ignores empty cells.
     * @param metadata All sample metadata as supplied from LabVessel
     * @param rowData Reference to the current row data structure
     */
    private void addMetadataToRowData( Set<Metadata> metadata, Map< Metadata.Key, List<String>> rowData ) {
        for( Metadata meta : metadata ) {
            if( rowData.containsKey(meta.getKey() ) ) {
                rowData.get(meta.getKey()).add(meta.getValue());
            }
        }
    }

    /**
     * Creates a data structure to hold multiple ancestor sample metadata values for a single lab vessel.
     * @return
     */
    private Map< Metadata.Key, List<String>> buildRowDataHolder(){
        Map< Metadata.Key, List<String>> rowDataHolder = new HashMap<>();
        for( Metadata.Key key : METADATA_HEADERS.keySet() ) {
            rowDataHolder.put(key, new ArrayList<String>());
        }
        return rowDataHolder;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull Map<String, Object> context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
