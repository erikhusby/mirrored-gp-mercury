package org.broadinstitute.gpinformatics.athena.control.dao.work;

import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Date;
import java.util.List;

@Stateful
@RequestScoped
public class WorkCompleteMessageDao extends GenericDao {

    /**
     * Get all the messages that haven't been processed yet.
     * @return a list of all messages that haven't yet been processed.
     */
    public List<WorkCompleteMessage> getNewMessages() {
        // If the process date is null, then it hasn't been processed.
        return findList(WorkCompleteMessage.class, WorkCompleteMessage_.processDate, null);
    }

    /**
     * Mark a message as having been processed.
     * @param message the message to mark.
     */
    public void markMessageProcessed(WorkCompleteMessage message) {
        // If the process date is non-null, then it's been processed.
        message.setProcessDate(new Date());
    }
}
