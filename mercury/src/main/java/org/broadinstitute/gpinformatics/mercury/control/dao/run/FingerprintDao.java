package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.Date;

/**
 * Data Access Object for Fingerprint entities.
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class FingerprintDao extends GenericDao {

    public Fingerprint findBySampleAndDateGenerated(MercurySample mercurySample, Date dateGenerated) {
        return findSingle(Fingerprint.class, (criteriaQuery, root) ->
                criteriaQuery.where(
                        getCriteriaBuilder().equal(root.get(Fingerprint_.mercurySample), mercurySample),
                        getCriteriaBuilder().equal(root.get(Fingerprint_.dateGenerated), dateGenerated)
                )
        );
    }

}
