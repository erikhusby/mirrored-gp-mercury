package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
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
     * Gathers sample metadata and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return sample metadata
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for sample metadata.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        // Append headers for sample metadata.
        for(Map.Entry<Metadata.Key, ConfigurableList.Header> valueHeaderEntry: METADATA_HEADERS.entrySet() ){
            headerGroup.addHeader(valueHeaderEntry.getValue());
        }

        // Populate rows with any available sample metadata.
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

            Set<MercurySample> samples = labVessel.getMercurySamples();
            for(MercurySample sample : samples) {
                Set<Metadata> metadata = sample.getMetadata();
                if( metadata != null && !metadata.isEmpty() ) {
                    addMetadataToRow( metadata, row );
                }
            }
            metricRows.add(row);
        }

        return metricRows;
    }

    /**
     * Adds sample metadata to the plugin row. Framework quietly ignores empty cells.
     * @param metadata All sample metadata as supplied from LabVessel
     * @param row Reference to the current plugin row
     */
    private void addMetadataToRow( Set<Metadata> metadata, ConfigurableList.Row row ) {
        ConfigurableList.Cell cell;
        String value;

        for( Metadata meta : metadata ) {
            if( meta.getKey().getCategory() == Metadata.Category.SAMPLE ) {
                value = meta.getValue();
                cell = new ConfigurableList.Cell(METADATA_HEADERS.get(meta.getKey()), value, value);
                row.addCell(cell);
            }
        }
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull Map<String, Object> context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in LabVesselMetricPlugin");
    }
}
