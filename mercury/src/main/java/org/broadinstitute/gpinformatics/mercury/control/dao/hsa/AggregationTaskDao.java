package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AggregationTaskDao extends GenericDao {

    public List<AggregationTask> findByStatus(Status status) {
        return findList(AggregationTask.class, AggregationTask_.status, status);
    }
}
