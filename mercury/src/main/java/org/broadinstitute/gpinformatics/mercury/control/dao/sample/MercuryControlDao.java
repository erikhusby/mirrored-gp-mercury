package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 2/21/13
 *         Time: 11:16 AM
 */
@Stateful
@RequestScoped
public class MercuryControlDao extends GenericDao {

    public MercuryControl findBySampleId(String sampleId) {
        return findSingle(MercuryControl.class, MercuryControl_.collaboratorSampleId, sampleId,
                                 new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.ACTIVE));
    }

    public List<MercuryControl> findAllActivePositiveControls() {
        return findAllActiveControlsByType(MercuryControl.CONTROL_TYPE.POSITIVE);
    }

    public List<MercuryControl> findAllActiveNegativeControls() {
        return findAllActiveControlsByType(MercuryControl.CONTROL_TYPE.NEGATIVE);
    }

    public List<MercuryControl> findAllInactivePositiveControls() {
        return findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE.POSITIVE);
    }

    public List<MercuryControl> findAllInactiveNegativeControls() {
        return findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE.NEGATIVE);
    }

    public List<MercuryControl> findAllActive() {
        return findAll(MercuryControl.class, new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.ACTIVE));
    }

    public List<MercuryControl> findAllInactive() {
        return findAll(MercuryControl.class, new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.INACTIVE));
    }

    private List<MercuryControl> findAllActiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findListByList(MercuryControl.class, MercuryControl_.type,
                                     Collections.singletonList(type),
                                     new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.ACTIVE));
    }

    private List<MercuryControl> findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findListByList(MercuryControl.class, MercuryControl_.type,
                                     Collections.singletonList(type),
                                     new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.INACTIVE));
    }

    private List<MercuryControl> findAllControlsByType(MercuryControl.CONTROL_TYPE type) {
        List<MercuryControl> results = new LinkedList<MercuryControl>();

        results.addAll(findListByList(MercuryControl.class, MercuryControl_.type,
                                             Collections.singletonList(type),
                                             new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.ACTIVE)));
        results.addAll(findListByList(MercuryControl.class, MercuryControl_.type,
                                             Collections.singletonList(type),
                                             new MercuryControlStateCallback(MercuryControl.CONTROL_STATE.INACTIVE)));
        return results;
    }

    private class MercuryControlStateCallback implements GenericDaoCallback<MercuryControl> {

        private final MercuryControl.CONTROL_STATE callbackState;

        private MercuryControlStateCallback(MercuryControl.CONTROL_STATE callbackState) {
            this.callbackState = callbackState;
        }

        @Override
        public void callback(CriteriaQuery<MercuryControl> mercuryControlCriteriaQuery,
                             Root<MercuryControl> mercuryControlRoot) {
            CriteriaBuilder cbuilder = getEntityManager().getCriteriaBuilder();

            List<Predicate> predicates = new ArrayList<Predicate>();

            mercuryControlCriteriaQuery.where(cbuilder.equal(mercuryControlRoot.get(MercuryControl_.state),
                                                                    callbackState));
        }
    }
}
