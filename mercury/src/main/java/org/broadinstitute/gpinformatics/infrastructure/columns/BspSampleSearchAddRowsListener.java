package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A listener for the ConfigurableList addRows method.  Fetches columns from BSP.
 */
public class BspSampleSearchAddRowsListener implements ConfigurableList.AddRowsListener {

    public static final String BSP_LISTENER = "BSP_LISTENER";

    private BSPSampleSearchColumn[] bspSampleSearchColumns;

    private BSPSampleSearchService bspSampleSearchService;

    private final Map<String, Map<BSPSampleSearchColumn, String>> mapSampleIdToColumns = new HashMap<>();

    public BspSampleSearchAddRowsListener(
            BSPSampleSearchColumn[] bspSampleSearchColumns,
            BSPSampleSearchService bspSampleSearchService) {
        this.bspSampleSearchColumns = bspSampleSearchColumns;
        this.bspSampleSearchService = bspSampleSearchService;
    }

    @Override
    public void addRows(List<?> entityList, Map<String, Object> context) {
        List<String> sampleIDs = new ArrayList<>();
        for (Object entity : entityList) {
            LabVessel labVessel = (LabVessel) entity;
            MercurySample mercurySample = labVessel.getMercurySamples().iterator().next();
            sampleIDs.add(mercurySample.getSampleKey());
        }
        List<Map<BSPSampleSearchColumn, String>> listMapColumnToValue = bspSampleSearchService.runSampleSearch(
                sampleIDs, bspSampleSearchColumns);
        for (Map<BSPSampleSearchColumn, String> mapColumnToValue : listMapColumnToValue) {
            mapSampleIdToColumns.put(mapColumnToValue.get(BSPSampleSearchColumn.SAMPLE_ID), mapColumnToValue);
        }
        context.put(BSP_LISTENER, this);
    }

    public String getColumn(String sampleKey, BSPSampleSearchColumn bspSampleSearchColumn) {
        return mapSampleIdToColumns.get(sampleKey).get(bspSampleSearchColumn);
    }
}
