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
 * Fetches available sample metadata for each page of a mercury sample search.
 */
public class SampleMetadataPlugin implements ListPlugin {

    public SampleMetadataPlugin() {}

    /**
     * Gathers sample metadata from mercury samples and associates with row in search results.
     * @param entityList  List of MercurySample entities for which to return sample metadata
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for sample metadata.
     * @return A list of rows, each corresponding to a MercurySample row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<MercurySample> sampleList = (List<MercurySample>) entityList;
        List<ConfigurableList.Row> metricRows = new ArrayList<>();

        MetadataPluginHelper.buildHeaders(headerGroup);

        // Populate rows with any available sample metadata.
        for( MercurySample sample : sampleList ) {

            // Collect cell values for each metadata type
            Map<Metadata.Key,List<String>> rowData = MetadataPluginHelper.buildRowDataHolder();

            MetadataPluginHelper.addSampleMetadataToRowData( sample, rowData );

            ConfigurableList.Row row = MetadataPluginHelper.buildRow(sample.getMercurySampleId().toString(), rowData );

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
