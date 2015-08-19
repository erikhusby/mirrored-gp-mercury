package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A listener for the ConfigurableList addRows method.  Fetches columns from BSP.
 */
public class BspSampleSearchAddRowsListener implements ConfigurableList.AddRowsListener {

    private List<BSPSampleSearchColumn> bspSampleSearchColumns = new ArrayList<>();

    private boolean columnsInitialized;

    private final Map<String, Map<BSPSampleSearchColumn, String>> mapSampleIdToColumns = new HashMap<>();

    public BspSampleSearchAddRowsListener() {
    }

    @Override
    public void addRows(List<?> entityList, SearchContext context, List<ColumnTabulation> nonPluginTabulations) {

        List<String> sampleIDs = new ArrayList<>();
        for (Object entity : entityList) {
            LabVessel labVessel = (LabVessel) entity;
            for( MercurySample mercurySample : labVessel.getMercurySamples() ) {
                sampleIDs.add(mercurySample.getSampleKey());
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

        List<Map<BSPSampleSearchColumn, String>> listMapColumnToValue;

        // Skip BSP call if no sample IDs or no BSP column data requested
        if( sampleIDs.isEmpty() || bspSampleSearchColumns.isEmpty() ) {
            listMapColumnToValue = new ArrayList<>();
        } else {

            // Do lookup instead of CDI annotation.
            BSPSampleSearchService bspSampleSearchService = ServiceAccessUtility.getBean(BSPSampleSearchService.class);

            // Needs sample ID in first position
            bspSampleSearchColumns.add( 0, BSPSampleSearchColumn.SAMPLE_ID );
            listMapColumnToValue = bspSampleSearchService.runSampleSearch(
                    sampleIDs, bspSampleSearchColumns.toArray(new BSPSampleSearchColumn[bspSampleSearchColumns.size()]));
        }
        for (Map<BSPSampleSearchColumn, String> mapColumnToValue : listMapColumnToValue) {
            mapSampleIdToColumns.put(mapColumnToValue.get(BSPSampleSearchColumn.SAMPLE_ID), mapColumnToValue);
        }
    }

    @Override
    public void reset() {
        columnsInitialized = false;
        bspSampleSearchColumns.clear();
        mapSampleIdToColumns.clear();
    }

    public String getColumn(String sampleKey, BSPSampleSearchColumn bspSampleSearchColumn) {
        Map<BSPSampleSearchColumn,String> bspSampleSearchColumnStringMap = mapSampleIdToColumns.get(sampleKey);
        if( bspSampleSearchColumnStringMap == null ) {
            return "";
        } else {
            return mapSampleIdToColumns.get(sampleKey).get(bspSampleSearchColumn);
        }
    }
}
