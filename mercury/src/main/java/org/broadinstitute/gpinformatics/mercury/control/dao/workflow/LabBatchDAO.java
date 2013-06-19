package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@RequestScoped
public class LabBatchDAO extends GenericDao {

    public LabBatch findByName(String batchName) {
        return findSingle(LabBatch.class, LabBatch_.batchName, batchName);
    }

    public List<LabBatch> findByListIdentifier(List<String> searchList) {
        return findListByList(LabBatch.class, LabBatch_.batchName, searchList);
    }

    public LabBatch findByBusinessKey(String businessKey) {
        return findByName(businessKey);
    }
}
