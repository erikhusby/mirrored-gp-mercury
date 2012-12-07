package org.broadinstitute.gpinformatics.mercury.presentation.batch;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

@ManagedBean
@ViewScoped
public class BatchListBean implements Serializable {
    @Inject
    private LabBatchDAO labBatchDAO;
    private List<LabBatch> batches;
    private LabBatch selectedBatch;
    private Boolean showVesselView = false;

    public LabBatch getSelectedBatch() {
        selectedBatch = labBatchDAO.findByName(selectedBatch.getBatchName());
        return selectedBatch;
    }

    public void setSelectedBatch(LabBatch selectedBatch) {
        this.selectedBatch = selectedBatch;
    }

    public void updateBatches(List<LabBatch> batches) {
        this.batches = new ArrayList<LabBatch>();
        for (LabBatch batch : batches) {
            this.batches.add(labBatchDAO.findByName(batch.getBatchName()));
        }
    }

    public List<LabBatch> getBatches() {
        return batches;
    }

    public void setBatches(List<LabBatch> batches) {
        this.batches = batches;
    }

    public Boolean getShowVesselView() {
        return showVesselView;
    }

    public void setShowVesselView(Boolean showVesselView) {
        this.showVesselView = showVesselView;
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

    public void toggleVesselView(LabBatch batch) {
        selectedBatch = batch;
        showVesselView = !showVesselView;
    }

    public String getOpenCloseValue(Boolean shown) {
        String value = "Open";
        if (shown) {
            value = "Close";
        }
        return value;
    }
}
