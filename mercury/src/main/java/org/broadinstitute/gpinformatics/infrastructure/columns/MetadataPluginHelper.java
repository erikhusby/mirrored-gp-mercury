package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

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
     * @param sample Sample returned as part of a search result
     * @param rowData Reference to the current row data structure
     */
    public static void addSampleMetadataToRowData( MercurySample sample, Map< Metadata.Key, List<String>> rowData ) {

        Set<Metadata> metadata = sample.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            // When metadata directly attached to sample, use it verbatim.
            fillInRowData( metadata, rowData );
        } else {
            // Metadata not directly attached to sample, try to get it from root sample.
            // Override material type with type inferred from transfer(s)
            for (LabVessel sampleVessel : sample.getLabVessel()) {
                for (SampleInstanceV2 sampleInstance : sampleVessel.getSampleInstancesV2()) {
                    MercurySample rootSample = sampleInstance.getRootOrEarliestMercurySample();
                    if (rootSample != null) {
                        metadata = rootSample.getMetadata();
                        if (metadata != null && !metadata.isEmpty()) {
                            fillInRowData( metadata, rowData );
                            // todo jmt get material type from sampleInstance
                            // todo jmt replace all of this code with DisplayExpression.METADATA?
                            // Replace material type with type from event
                            MaterialType materialType = sampleVessel.getLatestMaterialTypeFromEventHistory();
                            if (materialType != null && materialType != MaterialType.NONE) {
                                rowData.get(Metadata.Key.MATERIAL_TYPE).clear();
                                rowData.get(Metadata.Key.MATERIAL_TYPE).add(materialType.getDisplayName());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Convenience method to fill row cells with metadata values
     */
    private static void fillInRowData( Set<Metadata> metadata, Map< Metadata.Key, List<String>> rowData ) {
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
