package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A listener for the ConfigurableList addRows method.  Fetches columns from BSP.
 */
public class BspSampleSearchAddRowsListener implements ConfigurableList.AddRowsListener {

    public static final String BSP_LISTENER = "BSP_LISTENER";

    private List<BSPSampleSearchColumn> bspSampleSearchColumns = new ArrayList<>();

    @SuppressWarnings("BooleanVariableAlwaysNegated")
    private boolean columnsInitialized;

    private BSPSampleSearchService bspSampleSearchService;

    private final Map<String, Map<BSPSampleSearchColumn, String>> mapSampleIdToColumns = new HashMap<>();

    public BspSampleSearchAddRowsListener(BSPSampleSearchService bspSampleSearchService) {
        this.bspSampleSearchService = bspSampleSearchService;
    }

    @Override
    public void addRows(List<?> entityList, Map<String, Object> context, List<ColumnTabulation> nonPluginTabulations) {

        List<String> sampleIDs = new ArrayList<>();
        for (Object entity : entityList) {
            LabVessel labVessel = (LabVessel) entity;
            Collection<MercurySample> mercurySamples = labVessel.getMercurySamples();
            if( !mercurySamples.isEmpty() ) {
                MercurySample mercurySample = mercurySamples.iterator().next();
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
        // TODO JMS Will new (as of 09/18/2014) sample data fetcher quietly ignore all BSP data?
        if( sampleIDs.isEmpty() || bspSampleSearchColumns.isEmpty() ) {
            listMapColumnToValue = new ArrayList<>();
        } else {
            // Needs sample ID in first position
            bspSampleSearchColumns.add( 0, BSPSampleSearchColumn.SAMPLE_ID );
            listMapColumnToValue = bspSampleSearchService.runSampleSearch(
                    sampleIDs, bspSampleSearchColumns.toArray(new BSPSampleSearchColumn[bspSampleSearchColumns.size()]));
        }
        for (Map<BSPSampleSearchColumn, String> mapColumnToValue : listMapColumnToValue) {
            mapSampleIdToColumns.put(mapColumnToValue.get(BSPSampleSearchColumn.SAMPLE_ID), mapColumnToValue);
        }
        context.put(BSP_LISTENER, this);
    }

    public String getColumn(String sampleKey, BSPSampleSearchColumn bspSampleSearchColumn) {
        return mapSampleIdToColumns.get(sampleKey).get(bspSampleSearchColumn);
    }
}
