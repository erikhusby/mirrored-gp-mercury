package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class TaskDao extends GenericDao {

    public List<Task> findTasksById(List<Long> taskIds) {
        return findListByList(Task.class, Task_.taskId, taskIds);
    }

    public Task findTaskById(long taskId) {
        return findSingle(Task.class, Task_.taskId, taskId);
    }
}
