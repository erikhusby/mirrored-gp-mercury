package org.broadinstitute.gpinformatics.mercury.presentation.batch;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * This is the bean class for the composite component that represents a list of batches.
 */
@ManagedBean
@ViewScoped
public class BatchListBean extends AbstractJsfBean implements Serializable {
    @Inject
    private LabBatchDAO labBatchDAO;
    private LabBatch selectedBatch;

    public LabBatch getSelectedBatch() {
        return selectedBatch;
    }

    public void setSelectedBatch(LabBatch selectedBatch) {
        this.selectedBatch = labBatchDAO.findByBusinessKey(selectedBatch.getBusinessKey());
    }

    /**
     * Gets the latest lab event from a batch.
     *
     * @param batch the lab batch to get the latest event from.
     * @return the latest event from the batch.
     */
    public LabEvent getLatestEvent(LabBatch batch) {
        LabEvent lastEvent = null;
        for (LabEvent event : batch.getLabEvents()) {
            if (lastEvent == null) {
                lastEvent = event;
            } else {
                if (lastEvent.getEventDate().before(event.getEventDate())) {
                    lastEvent = event;
                }
            }
        }
        return lastEvent;
    }

    /**
     * Used to load the entity back into the session if we have lost it. This is used to avoid lazy initialization
     * exceptions.
     *
     * @param batch    the batch that information is being loaded from
     * @param property the property of the batch we are trying to access.
     * @return the batch that is now loaded into the hibernate session.
     */
    public LabBatch safeLoad(LabBatch batch, String property) {
        if (!labBatchDAO.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(batch, property)) {
            batch = labBatchDAO.findByBusinessKey(batch.getBusinessKey());
        }
        return batch;
    }
}
