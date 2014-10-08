package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control_;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * ControlIdCallback assists the MercuryControlDao in its queries.
 *
 * This class will create the predicates for the where in the Criteria query based on the desired control state and (if
 * one is passed in) the desired sample Id
 *
 * @author Scott Matthews
 */
class ControlIdCallback implements GenericDao.GenericDaoCallback<Control> {

    private final Control.ControlState state;
    private final String               id;
    private       ControlDao           controlDao;

    ControlIdCallback(ControlDao controlDao, @Nonnull Control.ControlState state, @Nullable String id) {
        this.controlDao = controlDao;
        this.state = state;
        this.id = id;
    }

    /**
     * Main implementation of the callback.  Creates the where statement for the CriteriaQuery.  The statement will
     * either just select the given state if the sample ID is null, or will build the where with the State AND the
     * sample ID
     *
     * @param controlCriteriaQuery CriteriaQuery object created by the GenericDao to which any dynamic query
     *                             building should be added
     * @param controlRoot          Root reference to the Control search to use as a reference for Joins
     *                             or fetches
     */
    @Override
    public void callback(CriteriaQuery<Control> controlCriteriaQuery, Root<Control> controlRoot) {
        CriteriaBuilder cbuilder = controlDao.getEntityManager().getCriteriaBuilder();

        Predicate clause = null;
        if (id != null) {
            clause = cbuilder.and(cbuilder.equal(controlRoot.get(Control_.state), state),
                                         cbuilder.equal(controlRoot.get(Control_.collaboratorParticipantId),
                                                               id));
        } else {
            clause = cbuilder.equal(controlRoot.get(Control_.state), state);
        }

        controlCriteriaQuery.where(clause);
    }
}
