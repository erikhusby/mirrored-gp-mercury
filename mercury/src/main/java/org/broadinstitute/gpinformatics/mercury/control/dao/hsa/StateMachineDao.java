package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class StateMachineDao extends GenericDao {

    public List<FiniteStateMachine> findByStatus(final Status status) {
        return findAll(FiniteStateMachine.class, new GenericDaoCallback<FiniteStateMachine>() {
            @Override
            public void callback(CriteriaQuery<FiniteStateMachine> criteriaQuery, Root<FiniteStateMachine> root) {
                criteriaQuery.where(getCriteriaBuilder().equal(root.get(FiniteStateMachine_.status), status));
            }
        });
    }

    public FiniteStateMachine findByIdentifier(String name) {
        return findSingle(FiniteStateMachine.class, FiniteStateMachine_.stateMachineName, name);
    }
}
