package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AlignmentStateDao extends GenericDao {

    public AlignmentState findByIdentifier(String stateName) {
        return findSingle(AlignmentState.class, AlignmentState_.stateName, stateName);
    }
}
