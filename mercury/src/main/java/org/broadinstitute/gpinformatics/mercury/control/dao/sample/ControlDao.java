package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines database interactions associated with {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control} entities
 *
 * @author Scott Matthews
 */
@Stateful
@RequestScoped
public class ControlDao extends GenericDao {

    /**
     * Finds an <u>active</u> control entity based on the collaborator participant ID
     *
     * @param collaboratorParticipantId collaborator participant ID associated with the control that we wish to find.
     *
     * @return a single {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control} instance that relates to the query
     */
    public Control findByCollaboratorParticipantId(String collaboratorParticipantId) {
        return findSingle(Control.class, new ControlIdCallback(this, Control.ControlState.ACTIVE,
                collaboratorParticipantId));
    }

    /**
     * Finds an <u>inactive</u> control entity based on the collaborator participant ID
     *
     * @param collaboratorParticipantId collaborator participant ID associated with the control that we wish to find.
     *
     * @return a single {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control} instance that relates to the query
     */
    public Control findInactiveByCollaboratorParticipantId(String collaboratorParticipantId) {
        return findSingle(Control.class, new ControlIdCallback(this, Control.ControlState.INACTIVE,
                collaboratorParticipantId));
    }


    /**
     * Finds all controls within the system that are currently in an active state
     *
     * @return a list of all {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control}s in the system that have a
     *         {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlState} of ACTIVE regardless of the control type
     */
    public List<Control> findAllActive() {
        return findAll(Control.class,
                              new ControlTypeCallback(this, Control.ControlState.ACTIVE, null));
    }

    /**
     * Finds all controls within the system that are currently in an inactive state
     *
     * @return a list of all {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control}s in the system that have a
     *         {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlState} of INACTIVE regardless of the control type
     */
    public List<Control> findAllInactive() {
        return findAll(Control.class,
                              new ControlTypeCallback(this, Control.ControlState.INACTIVE, null));
    }

    /**
     * Helper method to find all controls in an Active state based on a given type of control
     *
     * @param type the {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} by which to limit the search of active control types
     *
     * @return a List of all controls saved in the system that have a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlState} of
     *         Active and a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} that matches the type given
     */
    public List<Control> findAllActiveControlsByType(Control.ControlType type) {
        return findAll(Control.class,
                              new ControlTypeCallback(this, Control.ControlState.ACTIVE, type));

    }

    /**
     * Helper method to find all controls in an inactive state based on a given type of control
     *
     * @param type the {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} by which to limit the search of inactive control types
     *
     * @return a List of all controls saved in the system that have a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlState} of
     *         Inactive and a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} that matches the type given
     */
    public List<Control> findAllInactiveControlsByType(Control.ControlType type) {
        return findAll(Control.class,
                              new ControlTypeCallback(this, Control.ControlState.INACTIVE, type));
    }

    /**
     * Helper method to find all controls in the system based on a given type of control regardless of the current
     * state
     *
     * @param type the {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} by which to limit the search control types
     *
     * @return a List of all controls saved in the system that have a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlType} that matches
     *         the type given.  The state of the controls found are irrelevant to this search.
     */
    public List<Control> findAllControlsByType(Control.ControlType type) {
        List<Control> results = new LinkedList<>();

        results.addAll(findAll(Control.class,
                                      new ControlTypeCallback(this, Control.ControlState.ACTIVE, type)));
        results.addAll(findAll(Control.class,
                                      new ControlTypeCallback(this, Control.ControlState.INACTIVE,
                                                                            type)));
        return results;
    }

}
