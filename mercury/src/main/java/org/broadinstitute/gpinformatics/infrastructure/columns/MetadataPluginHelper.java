package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Functionality shared between lab vessel and sample metadata row list plugins
 */
public class MetadataPluginHelper {

    // Build immutable headers for all sample metadata.
    private static Map<Metadata.Key, ConfigurableList.Header> METADATA_HEADERS;
    static {
        Map<Metadata.Key, ConfigurableList.Header> metadataHeaders = new LinkedHashMap<>();
        for( Metadata.Key key : Metadata.Key.values() ) {
            if(key.getCategory() == Metadata.Category.SAMPLE ) {
                metadataHeaders
                        .put(key, new ConfigurableList.Header(key.getDisplayName(), key.getDisplayName(), ""));
            }
        }
        METADATA_HEADERS = Collections.unmodifiableMap(metadataHeaders);
    }

    public static Map<Metadata.Key, ConfigurableList.Header> getMetaDataHeaders(){
        return METADATA_HEADERS;
    }

    /**
     * Append sample metadata headers to report header group.
     * @param headerGroup
     */
    public static void buildHeaders(ConfigurableList.HeaderGroup headerGroup) {
        for(Map.Entry<Metadata.Key, ConfigurableList.Header> valueHeaderEntry: METADATA_HEADERS.entrySet() ){
            headerGroup.addHeader(valueHeaderEntry.getValue());
        }
    }

    /**
     * Appends sample metadata values to the sample row. Framework quietly ignores empty cells.
     * @param metadata All sample metadata as supplied from LabVessel
     * @param rowData Reference to the current row data structure
     */
    public static void addMetadataToRowData( Set<Metadata> metadata, Map< Metadata.Key, List<String>> rowData ) {
        for( Metadata meta : metadata ) {
            if( rowData.containsKey(meta.getKey() ) ) {
                rowData.get(meta.getKey()).add(meta.getValue());
            }
        }
    }

    /**
     * Creates a data structure to hold multiple ancestor sample metadata values for a single result row.
     * @return
     */
    public static  Map< Metadata.Key, List<String>> buildRowDataHolder(){
        Map< Metadata.Key, List<String>> rowDataHolder = new HashMap<>();
        for( Metadata.Key key : METADATA_HEADERS.keySet() ) {
            rowDataHolder.put(key, new ArrayList<String>());
        }
        return rowDataHolder;
    }


    /**
     * Builds the list's row and populates cells with metadata values
     * @param rowId
     * @param rowData
     * @return
     */
    public static ConfigurableList.Row buildRow(String rowId, Map<Metadata.Key, List<String>> rowData) {
        ConfigurableList.Row row = new ConfigurableList.Row( rowId );

        // Ancestor sample metadata collected, create and append row cells
        StringBuilder valueHolder = new StringBuilder();
        for(  Map.Entry<Metadata.Key,List<String>> rowDataEntry : rowData.entrySet() ){
            // Re-use builder
            valueHolder.setLength(0);

            for( String value : rowDataEntry.getValue() ) {
                valueHolder.append(value).append(' ');
            }
            String value = valueHolder.toString().trim();
            ConfigurableList.Cell cell = new ConfigurableList.Cell(
                    METADATA_HEADERS.get(rowDataEntry.getKey()), value, value);
            row.addCell(cell);
        }

        return row;
    }
}
