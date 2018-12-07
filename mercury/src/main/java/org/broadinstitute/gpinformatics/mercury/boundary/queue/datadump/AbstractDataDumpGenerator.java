package org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDataDumpGenerator {

    private List<Object[]> generateData(QueueGrouping queueGrouping) {
        List<QueueEntity> queuedEntities = queueGrouping.getQueuedEntities();
        List<LabVessel> labVessels = new ArrayList<>(queuedEntities.size());
        for (QueueEntity queuedEntity : queuedEntities) {
            labVessels.add(queuedEntity.getLabVessel());
        }
        return generateData(labVessels);
    }

    protected abstract Object[] generateData(LabVessel labVessel);

    private List<Object[]> generateData(List<LabVessel> labVessel) {

        List<Object[]> rows = new ArrayList<>();
        for (LabVessel vessel : labVessel) {
            rows.add(generateData(vessel));
        }

        return rows;
    }

    protected abstract String[] generateHeaderRow();

    public Object[][] generateSpreadsheet(QueueGrouping queueGrouping) {
        List<Object[]> rows = generateData(queueGrouping);
        Object[] headerRow = generateHeaderRow();
        Object[][] sheet = new Object[rows.size() + 1][headerRow.length];
        int currentRow = 0;
        sheet[currentRow++] = headerRow;
        for (Object[] row : rows) {
            sheet[currentRow++] = row;
        }

        return sheet;
    }

    public Object[][] generateSpreadsheet(List<QueueGrouping> queueGroupings) {

        List<LabVessel> labVessels = new ArrayList<>();
        for (QueueGrouping queueGrouping : queueGroupings) {
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                labVessels.add(queueEntity.getLabVessel());
            }
        }

        List<Object[]> rows = generateData(labVessels);
        Object[] headerRow = generateHeaderRow();
        Object[][] sheet = new Object[rows.size() + 1][headerRow.length];
        int currentRow = 0;
        sheet[currentRow++] = headerRow;
        for (Object[] row : rows) {
            sheet[currentRow++] = row;
        }

        return sheet;
    }
}
