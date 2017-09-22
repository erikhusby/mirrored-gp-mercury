package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class BillingSessionEtl extends GenericEntityEtl<BillingSession, BillingSession> {

    public BillingSessionEtl() {
    }

    @Inject
    public BillingSessionEtl(BillingSessionDao dao) {
        super(BillingSession.class, "billing_session", "athena.billing_session_aud", "billing_session_id", dao);
    }

    @Override
    Long entityId(BillingSession entity) {
        return entity.getBillingSessionId();
    }

    @Override
    Path rootId(Root<BillingSession> root) {
        return root.get(BillingSession_.billingSessionId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(BillingSession.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, BillingSession entity) {
        if (entity.getBilledDate() == null) {
            // We only want to ETL completed billing sessions, not ones that are in progress.
            return null;
        }
        return genericRecord(etlDateStr, isDelete,
                entity.getBillingSessionId(),
                format(entity.getBilledDate()),
                format(entity.getBillingSessionType().toString()));
    }
}
