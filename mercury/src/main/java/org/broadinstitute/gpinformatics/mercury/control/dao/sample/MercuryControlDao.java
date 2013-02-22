package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines database interactions associated with {@link MercuryControl} entities
 *
 * @author Scott Matthews
 */
@Stateful
@RequestScoped
public class MercuryControlDao extends GenericDao {

    /**
     * Finds an <u>active</u> control entity based on the sample ID
     *
     * @param sampleId collaborator sample ID associated with the control that we wish to find.
     *
     * @return a single {@link MercuryControl} instance that relates to the query
     */
    public MercuryControl findBySampleId(String sampleId) {
        return findSingle(MercuryControl.class, new MercuryControlIdCallback(this, MercuryControl.CONTROL_STATE.ACTIVE,
                                                                                    sampleId));
    }

    /**
     * Finds all <u>active</u> controls within the system that are stored as positive
     *
     * @return a list of all {@link MercuryControl}s in the system that not only have  a
     *         {@link MercuryControl.CONTROL_STATE} of ACTIVE but also have a
     *         {@link MercuryControl.CONTROL_TYPE} of POSITIVE
     */
    public List<MercuryControl> findAllActivePositiveControls() {
        return findAllActiveControlsByType(MercuryControl.CONTROL_TYPE.POSITIVE);
    }

    /**
     * Finds all <u>active</u> controls within the system that are stored as Negative
     *
     * @return a list of all {@link MercuryControl}s in the system that not only have  a
     *         {@link MercuryControl.CONTROL_STATE} of ACTIVE but also have a
     *         {@link MercuryControl.CONTROL_TYPE} of NEGATIVE
     */
    public List<MercuryControl> findAllActiveNegativeControls() {
        return findAllActiveControlsByType(MercuryControl.CONTROL_TYPE.NEGATIVE);
    }

    /**
     * Finds all <u>inactive</u> controls within the system that are stored as positive
     *
     * @return a list of all {@link MercuryControl}s in the system that not only have  a
     *         {@link MercuryControl.CONTROL_STATE} of INACTIVE but also have a
     *         {@link MercuryControl.CONTROL_TYPE} of POSITIVE
     */
    public List<MercuryControl> findAllInactivePositiveControls() {
        return findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE.POSITIVE);
    }

    /**
     * Finds all <u>inactive</u> controls within the system that are stored as Negative
     *
     * @return a list of all {@link MercuryControl}s in the system that not only have  a
     *         {@link MercuryControl.CONTROL_STATE} of INACTIVE but also have a
     *         {@link MercuryControl.CONTROL_TYPE} of NEGATIVE
     */
    public List<MercuryControl> findAllInactiveNegativeControls() {
        return findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE.NEGATIVE);
    }

    /**
     * Finds all controls within the system that are currently in an active state
     *
     * @return a list of all {@link MercuryControl}s in the system that have a
     *         {@link MercuryControl.CONTROL_STATE} of ACTIVE regardless of the control type
     */
    public List<MercuryControl> findAllActive() {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE, null));
    }

    /**
     * Finds all controls within the system that are currently in an inactive state
     *
     * @return a list of all {@link MercuryControl}s in the system that have a
     *         {@link MercuryControl.CONTROL_STATE} of INACTIVE regardless of the control type
     */
    public List<MercuryControl> findAllInactive() {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE, null));
    }

    /**
     * Helper method to find all controls in an Active state based on a given type of control
     *
     * @param type the {@link MercuryControl.CONTROL_TYPE} by which to limit the search of active control types
     *
     * @return a List of all controls saved in the system that have a {@link MercuryControl.CONTROL_STATE} of
     *         Active and a {@link MercuryControl.CONTROL_TYPE} that matches the type given
     */
    private List<MercuryControl> findAllActiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE, type));

    }

    /**
     * Helper method to find all controls in an inactive state based on a given type of control
     *
     * @param type the {@link MercuryControl.CONTROL_TYPE} by which to limit the search of inactive control types
     *
     * @return a List of all controls saved in the system that have a {@link MercuryControl.CONTROL_STATE} of
     *         Inactive and a {@link MercuryControl.CONTROL_TYPE} that matches the type given
     */
    private List<MercuryControl> findAllInactiveControlsByType(MercuryControl.CONTROL_TYPE type) {
        return findAll(MercuryControl.class,
                              new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE, type));
    }

    /**
     * Helper method to find all controls in the system based on a given type of control regardless of the current
     * state
     *
     * @param type the {@link MercuryControl.CONTROL_TYPE} by which to limit the search control types
     *
     * @return a List of all controls saved in the system that have a {@link MercuryControl.CONTROL_TYPE} that matches
     *         the type given.  The state of the controls found are irrelevant to this search.
     */
    private List<MercuryControl> findAllControlsByType(MercuryControl.CONTROL_TYPE type) {
        List<MercuryControl> results = new LinkedList<MercuryControl>();

        results.addAll(findAll(MercuryControl.class,
                                      new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.ACTIVE, type)));
        results.addAll(findAll(MercuryControl.class,
                                      new MercuryControlTypeCallback(this, MercuryControl.CONTROL_STATE.INACTIVE,
                                                                            type)));
        return results;
    }

}
