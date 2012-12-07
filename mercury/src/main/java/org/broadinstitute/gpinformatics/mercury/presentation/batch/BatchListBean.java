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
    private LabBatch selectedBatch;

    public LabBatch getSelectedBatch() {
        return selectedBatch;
    }

    public void setSelectedBatch(LabBatch selectedBatch) {
        this.selectedBatch = labBatchDAO.findByName(selectedBatch.getBatchName());
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

    public LabBatch safeLoad(LabBatch batch, String property) {
        if (!labBatchDAO.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(batch, property)) {
            batch = labBatchDAO.findByName(batch.getBatchName());
        }
        return batch;
    }
}
