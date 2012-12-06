package org.broadinstitute.gpinformatics.mercury.presentation.batch;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.*;

@ManagedBean
@ViewScoped
public class BatchListBean {
    private List<LabBatch> batches;

    public void updateBatches(List<LabBatch> batches) {
        this.batches = batches;
    }

    public List<LabBatch> getBatches() {
        return batches;
    }

    public void setBatches(List<LabBatch> batches) {
        this.batches = batches;
    }

    public LabEvent getLatestEvent(LabBatch batch) {
        Map<Date, LabEvent> sortedEventsByDate = new TreeMap<Date, LabEvent>();
        LabEvent lastEvent = null;
        for (LabEvent event : batch.getLabEvents()) {
            sortedEventsByDate.put(event.getEventDate(), event);
        }
        List<LabEvent> sortedList = new ArrayList<LabEvent>(sortedEventsByDate.values());
        if (sortedList.size() > 0) {
            lastEvent = sortedList.get(sortedList.size() - 1);
        }
        return lastEvent;
    }
}
