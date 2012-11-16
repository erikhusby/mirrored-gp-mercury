package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Database interactions involving Billing Sessions
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class BillingSessionDao extends GenericDao {

    public List<BillingSession> findAll() {
        return findAll(BillingSession.class);
    }
}
