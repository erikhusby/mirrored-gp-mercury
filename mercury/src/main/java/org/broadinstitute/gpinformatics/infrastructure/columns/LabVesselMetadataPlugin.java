package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches available sample metadata for each page of a lab vessel search.
 */
public class LabVesselMetadataPlugin implements ListPlugin {

    public LabVesselMetadataPlugin() {}

    /**
     * Gathers sample metadata from current and ancestor lab vessels and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return sample metadata
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for sample metadata.
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        MetadataPluginHelper.buildHeaders(headerGroup);

        // Populate rows with any available sample metadata.
        for( LabVessel labVessel : labVesselList ) {

            // Collect cell values for each metadata type in case multiple samples in vessel ancestry
            Map<Metadata.Key,List<String>> rowData = MetadataPluginHelper.buildRowDataHolder();

            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
                if( sample != null ) {
                    Set<Metadata> metadata = sample.getMetadata();
                    if( metadata != null && !metadata.isEmpty() ) {
                        MetadataPluginHelper.addMetadataToRowData( metadata, rowData );
                    }
                }
            }

            ConfigurableList.Row row = MetadataPluginHelper.buildRow(labVessel.getLabel(), rowData );

            metricRows.add(row);
        }

        return metricRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in "
                                                + getClass().getSimpleName() );
    }

}
