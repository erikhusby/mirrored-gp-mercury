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
 * MercuryControlIdCallback assists the MercuryControlDao in its queries.
 *
 * This class will create the predicates for the where in the Criteria query based on the desired control state and (if
 * one is passed in) the desired sample Id
 *
 * @author Scott Matthews
 */
class MercuryControlIdCallback implements GenericDao.GenericDaoCallback<MercuryControl> {

    private final MercuryControl.ControlState state;
    private final String                      id;
    private       MercuryControlDao           mercuryControlDao;

    MercuryControlIdCallback(MercuryControlDao mercuryControlDao,
                             @Nonnull MercuryControl.ControlState state,
                             @Nullable String id) {
        this.mercuryControlDao = mercuryControlDao;
        this.state = state;
        this.id = id;
    }

    /**
     * Main implementation of the callback.  Creates the where statement for the CriteriaQuery.  The statement will
     * either just select the given state if the sample ID is null, or will build the where with the State AND the
     * sample ID
     *
     * @param mercuryControlCriteriaQuery CriteriaQuery object created by the GenericDao to which any dynamic query
     *                                    building should be added
     * @param mercuryControlRoot          Root reference to the Mercury Control search to use as a reference for Joins
     *                                    or fetches
     */
    @Override
    public void callback(CriteriaQuery<MercuryControl> mercuryControlCriteriaQuery,
                         Root<MercuryControl> mercuryControlRoot) {
        CriteriaBuilder cbuilder = mercuryControlDao.getEntityManager().getCriteriaBuilder();

        Predicate clause = null;
        if (id != null) {
            clause = cbuilder.and(cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), state),
                                         cbuilder.equal(mercuryControlRoot.get(MercuryControl_.collaboratorSampleId),
                                                               id));
        } else {
            clause = cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state), state);
        }

        mercuryControlCriteriaQuery.where(clause);
    }
}
