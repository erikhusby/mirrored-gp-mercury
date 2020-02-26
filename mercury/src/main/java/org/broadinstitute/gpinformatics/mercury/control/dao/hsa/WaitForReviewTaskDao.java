package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class WaitForReviewTaskDao extends GenericDao {

    public List<WaitForReviewTask> findAllByStatus(Status status) {
        return findList(WaitForReviewTask.class, WaitForReviewTask_.status, status);
    }
}
