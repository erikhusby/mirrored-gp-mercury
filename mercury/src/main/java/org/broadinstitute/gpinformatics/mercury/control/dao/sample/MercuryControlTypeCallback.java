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
 * MercuryControlTypeCallback assists the MercuryControlDao in its' queries.
 *
 * This class will create the predicates for the where in the Criteria query based on the desired control state and (if
 * one is passed in) the desired type
 *
 * @author Scott Matthews
 */
class MercuryControlTypeCallback implements GenericDao.GenericDaoCallback<MercuryControl> {

    private final MercuryControl.ControlState callbackState;
    private final MercuryControl.ControlType callbackType;
    private final MercuryControlDao            mercuryControlDao;

    MercuryControlTypeCallback(@Nonnull MercuryControlDao mercuryControlDao,
                               @Nonnull MercuryControl.ControlState callbackState,
                               @Nullable MercuryControl.ControlType callbackType) {
        this.mercuryControlDao = mercuryControlDao;
        this.callbackState = callbackState;
        this.callbackType = callbackType;
    }

    /**
     *
     * Main implementation of the callback.  Creates the where statement for the CriteriaQuery.  The statement will
     * either just select the given state if the type is null, or will build the where with the State AND the
     * callback type
     *
     * @param mercuryControlCriteriaQuery CriteriaQuery object created by the GenericDao to which any dynamic query
     *                                    building should be added
     * @param mercuryControlRoot Root reference to the Mercury Control search to use as a reference for Joins or fetches
     */
    @Override
    public void callback(CriteriaQuery<MercuryControl> mercuryControlCriteriaQuery,
                         Root<MercuryControl> mercuryControlRoot) {
        CriteriaBuilder cbuilder = mercuryControlDao.getEntityManager().getCriteriaBuilder();

        Predicate clause;
        if (callbackType != null) {
            clause = cbuilder.and(cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState),
                                         cbuilder.equal(mercuryControlRoot.get(MercuryControl_.type), callbackType));
        } else {
            clause = cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), callbackState);
        }

        mercuryControlCriteriaQuery.where(clause);
    }
}
