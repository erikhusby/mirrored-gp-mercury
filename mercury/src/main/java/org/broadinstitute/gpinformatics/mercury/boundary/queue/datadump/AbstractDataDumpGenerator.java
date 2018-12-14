package org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataDumpGenerator {

    protected abstract Object[] extractData(SampleData sampleData);

    protected abstract BSPSampleSearchColumn[] getSearchColumns();

    private List<Object[]> generateData(QueueGrouping queueGrouping) {
        List<QueueEntity> queuedEntities = queueGrouping.getQueuedEntities();
        List<LabVessel> labVessels = new ArrayList<>(queuedEntities.size());
        for (QueueEntity queuedEntity : queuedEntities) {
            labVessels.add(queuedEntity.getLabVessel());
        }
        return generateData(labVessels);
    }

    private Map<String, SampleData> loadData(List<MercurySample> mercurySamples) {
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
        return sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, getSearchColumns());
    }

    private List<Object[]> generateData(List<LabVessel> labVessels) {

        List<Object[]> rows = new ArrayList<>();
        List<MercurySample> mercurySamples = new ArrayList<>();
        Map<Long, String> labVesselIdToSampleId = new HashMap<>();
        Map<Long, MercurySample> labVesselIdToMercurySample = new HashMap<>();

        loadMercurySampleInformation(labVessels, mercurySamples, labVesselIdToSampleId, labVesselIdToMercurySample);

        Map<String, SampleData> sampleIdToSampleData = loadData(mercurySamples);

        for (LabVessel labVessel : labVessels) {
            rows.add(extractData(sampleIdToSampleData.get(labVesselIdToSampleId.get(labVessel.getLabVesselId()))));
        }

        return rows;
    }

    public static void loadMercurySampleInformation(List<LabVessel> labVessels, List<MercurySample> mercurySamples,
                                                    Map<Long, String> labVesselIdToSampleId,
                                                    Map<Long, MercurySample> labVesselIdToMercurySample) {
        for (LabVessel labVessel : labVessels) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample mercurySample = sampleInstanceV2.getNearestMercurySample();
                mercurySamples.add(mercurySample);
                labVesselIdToSampleId.put(labVessel.getLabVesselId(), mercurySample.getSampleKey());
                labVesselIdToMercurySample.put(labVessel.getLabVesselId(), mercurySample);
            }
        }
    }

    private String[] generateHeaderRow(BSPSampleSearchColumn[] picoQueueDataDump) {
        String[] headers = new String[getSearchColumns().length];
        int index = 0;
        for (BSPSampleSearchColumn bspSampleSearchColumn : picoQueueDataDump) {
            headers[index] = bspSampleSearchColumn.columnName();
        }

        return headers;
    }

    public Object[][] generateSpreadsheet(QueueGrouping queueGrouping) {
        List<Object[]> rows = generateData(queueGrouping);
        Object[] headerRow = generateHeaderRow(getSearchColumns());
        Object[][] sheet = new Object[rows.size() + 1][headerRow.length];
        int currentRow = 0;
        sheet[currentRow++] = headerRow;
        for (Object[] row : rows) {
            sheet[currentRow++] = row;
        }

        return sheet;
    }

    public Object[][] generateSpreadsheet(Collection<QueueGrouping> queueGroupings) {

        List<LabVessel> labVessels = new ArrayList<>();
        for (QueueGrouping queueGrouping : queueGroupings) {
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                labVessels.add(queueEntity.getLabVessel());
            }
        }

        List<Object[]> rows = generateData(labVessels);
        Object[] headerRow = generateHeaderRow(getSearchColumns());
        Object[][] sheet = new Object[rows.size() + 1][headerRow.length];
        int currentRow = 0;
        sheet[currentRow++] = headerRow;
        for (Object[] row : rows) {
            sheet[currentRow++] = row;
        }

        return sheet;
    }
}
