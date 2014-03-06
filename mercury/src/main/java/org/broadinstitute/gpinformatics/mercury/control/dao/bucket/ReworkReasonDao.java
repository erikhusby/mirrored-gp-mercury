package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ReworkReasonDao extends GenericDao {

    public ReworkReason findById(Long reworkId) {
        return findById(ReworkReason.class, reworkId);
    }

    public List<ReworkReason> findAll() {
        return findAll(ReworkReason.class);
    }

    public ReworkReason findByReason(@Nonnull String reason) {
        return findSingle(ReworkReason.class, ReworkReason_.reason, reason);
    }
}
