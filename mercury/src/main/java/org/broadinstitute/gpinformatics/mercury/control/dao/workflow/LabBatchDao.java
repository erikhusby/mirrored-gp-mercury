package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LabBatchDao extends GenericDao {

    public LabBatch findByName(String batchName) {
        return findSingle(LabBatch.class, LabBatch_.batchName, batchName);
    }

    public List<LabBatch> findByListIdentifier(List<String> searchList) {
        return findListByList(LabBatch.class, LabBatch_.batchName, searchList);
    }

    public List<LabBatch> findByType(LabBatch.LabBatchType labBatchType) {
        return findList(LabBatch.class, LabBatch_.labBatchType, labBatchType);
    }

    public LabBatch findByBusinessKey(String businessKey) {
        return findByName(businessKey);
    }
}
