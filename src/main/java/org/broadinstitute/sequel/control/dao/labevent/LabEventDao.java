package org.broadinstitute.sequel.control.dao.labevent;

import org.broadinstitute.sequel.control.dao.ThreadEntityManager;
import org.broadinstitute.sequel.entity.labevent.LabEvent;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Data Access Object for LabEvents
 */
@Stateful
@RequestScoped
public class LabEventDao {
    @Inject
    private ThreadEntityManager threadEntityManager;

    public void persist(LabEvent labEvent) {
        this.threadEntityManager.getEntityManager().persist(labEvent);
    }

    public void flush() {
        this.threadEntityManager.getEntityManager().flush();
    }
}
