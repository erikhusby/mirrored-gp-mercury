package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl_;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
* @author Scott Matthews
*         Date: 2/21/13
*         Time: 4:54 PM
*/
class MercuryControlTypeCallback implements GenericDao.GenericDaoCallback<MercuryControl> {

    private final MercuryControl.CONTROL_STATE callbackState;

    private final MercuryControl.CONTROL_TYPE callbackType;
    private MercuryControlDao mercuryControlDao;

    MercuryControlTypeCallback(MercuryControlDao mercuryControlDao,
                               @Nonnull MercuryControl.CONTROL_STATE callbackState,
                               @Nullable MercuryControl.CONTROL_TYPE callbackType) {
        this.mercuryControlDao = mercuryControlDao;
        this.callbackState = callbackState;
        this.callbackType = callbackType;
    }

    @Override
    public void callback(CriteriaQuery<MercuryControl> mercuryControlCriteriaQuery,
                         Root<MercuryControl> mercuryControlRoot) {
        CriteriaBuilder cbuilder = mercuryControlDao.getEntityManager().getCriteriaBuilder();

        Predicate clause =null;
        if(callbackType != null) {
            clause = cbuilder.and(cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState),
                                        cbuilder.equal(mercuryControlRoot.get(MercuryControl_.type),callbackType));
        } else {
            clause = cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState);
        }

        mercuryControlCriteriaQuery.where(clause);
    }
}
