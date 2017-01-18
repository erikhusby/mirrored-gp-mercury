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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A listener for the ConfigurableList addRows method.  Fetches columns from BSP.
 */
public class BspSampleSearchAddRowsListener implements ConfigurableList.AddRowsListener {

    private final List<BSPSampleSearchColumn> bspSampleSearchColumns = new ArrayList<>();

    private boolean columnsInitialized;

    private Map<String, SampleData> mapIdToSampleData = new HashMap<>();

    public BspSampleSearchAddRowsListener() {
    }

    @Override
    public void addRows(List<?> entityList, SearchContext context, List<ColumnTabulation> nonPluginTabulations) {

        List<MercurySample> samples = new ArrayList<>();
        for (Object entity : entityList) {
            LabVessel labVessel = (LabVessel) entity;
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
                if (mercurySample != null) {
                    samples.add(mercurySample);
                }
            }
        }
        if (!columnsInitialized) {
           for (ColumnTabulation nonPluginTabulation : nonPluginTabulations) {
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
