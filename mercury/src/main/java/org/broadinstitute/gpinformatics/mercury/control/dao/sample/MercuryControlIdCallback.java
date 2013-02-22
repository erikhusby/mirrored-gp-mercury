package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl_;

import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
* @author Scott Matthews
*         Date: 2/21/13
*         Time: 4:54 PM
*/
class MercuryControlIdCallback implements GenericDao.GenericDaoCallback<MercuryControl> {

    private final MercuryControl.CONTROL_STATE callbackState;

    private final String callbackId;
    private MercuryControlDao mercuryControlDao;

    MercuryControlIdCallback(MercuryControlDao mercuryControlDao,
                             @Nonnull MercuryControl.CONTROL_STATE callbackState,
                             @Nonnull String callbackId) {
        this.mercuryControlDao = mercuryControlDao;
        this.callbackState = callbackState;
        this.callbackId = callbackId;
    }

    @Override
    public void callback(CriteriaQuery<MercuryControl> mercuryControlCriteriaQuery,
                         Root<MercuryControl> mercuryControlRoot) {
        CriteriaBuilder cbuilder = mercuryControlDao.getEntityManager().getCriteriaBuilder();

        Predicate clause =null;
        if(callbackId != null) {
            clause = cbuilder.and(cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState),
                                        cbuilder.equal(mercuryControlRoot.get(MercuryControl_.collaboratorSampleId),
                                                              callbackId));
        } else {
            clause = cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState);
        }

        mercuryControlCriteriaQuery.where(clause);
    }
}
