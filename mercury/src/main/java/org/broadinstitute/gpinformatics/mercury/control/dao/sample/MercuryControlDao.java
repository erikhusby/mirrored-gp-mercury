package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
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
        return findSingle(MercuryControl.class, new MercuryControlIdCallback(this, MercuryControl.CONTROL_STATE.ACTIVE,
                                                                                    sampleId));
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
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE,null));
    }

    public List<MercuryControl> findAllInactive() {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE, null));
    }

    private List<MercuryControl> findAllActiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE,type));

    }

    private List<MercuryControl> findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE, type));
    }

    private List<MercuryControl> findAllControlsByType(MercuryControl.CONTROL_TYPE type) {
        List<MercuryControl> results = new LinkedList<MercuryControl>();

        results.addAll(findAll(MercuryControl.class,
                                      new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE, type)));
        results.addAll(findAll(MercuryControl.class,
                                      new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE, type)));
        return results;
    }

}
