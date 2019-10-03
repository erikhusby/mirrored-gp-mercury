package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A listener for the ConfigurableList addRows method.  Fetches columns from BSP, or from database if CRSP.
 */
public class SampleDataFetcherAddRowsListener implements ConfigurableList.AddRowsListener {

    private final List<BSPSampleSearchColumn> bspSampleSearchColumns = new ArrayList<>();

    private boolean columnsInitialized;

    private Map<String, SampleData> mapIdToSampleData = new HashMap<>();

    public SampleDataFetcherAddRowsListener() {
    }

    @Override
    public void addRows(List<?> entityList, SearchContext context, Map<Integer,ColumnTabulation> nonPluginTabulations) {

        if (!columnsInitialized) {
           for (ColumnTabulation nonPluginTabulation : nonPluginTabulations.values()) {
                // todo jmt avoid all this casting
                SearchTerm searchTerm;
                if (nonPluginTabulation instanceof SearchInstance.SearchValue) {
                    searchTerm = ((SearchInstance.SearchValue) nonPluginTabulation).getSearchTerm();
                } else  {
                    searchTerm = ((SearchTerm) nonPluginTabulation);
                }
                SearchTerm.Evaluator<Object> addRowsListenerHelper = searchTerm.getAddRowsListenerHelper();
                if (addRowsListenerHelper != null) {
                    BSPSampleSearchColumn bspSampleSearchColumn =
                            (BSPSampleSearchColumn) addRowsListenerHelper.evaluate(null, null);
                    bspSampleSearchColumns.add(bspSampleSearchColumn);
                }
            }
            columnsInitialized = true;
        }

        List<MercurySample> samples = new ArrayList<>();
        if (!bspSampleSearchColumns.isEmpty()) {
            for (Object entity : entityList) {
                List<SampleInstanceV2> sampleInstances = DisplayExpression.rowObjectToExpressionObject(entity,
                        SampleInstanceV2.class, context);
                for (SampleInstanceV2 sampleInstanceV2 : sampleInstances) {
                    MercurySample mercurySample = sampleInstanceV2.getNearestMercurySample();
                    if (mercurySample != null) {
                        samples.add(mercurySample);
                    }
                }
            }
        }

        // Skip BSP call if no sample IDs or no BSP column data requested
        if (!samples.isEmpty() && !bspSampleSearchColumns.isEmpty()) {

            // Do lookup instead of CDI annotation.
            SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);

            // Needs sample ID in first position
            bspSampleSearchColumns.add( 0, BSPSampleSearchColumn.SAMPLE_ID );
            mapIdToSampleData = sampleDataFetcher.fetchSampleDataForSamples(samples,
                    bspSampleSearchColumns.toArray(new BSPSampleSearchColumn[bspSampleSearchColumns.size()]));
        }
    }

    @Override
    public void reset() {
        columnsInitialized = false;
        bspSampleSearchColumns.clear();
        mapIdToSampleData.clear();
    }

    public SampleData getSampleData(String sampleKey) {
        return mapIdToSampleData.get(sampleKey);
    }
}
