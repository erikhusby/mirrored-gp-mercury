package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * @author Scott Matthews
 */
@Stateful
@RequestScoped
public class ControlEjb {

    @Inject
    private ControlDao controlDao;

    /**
     *
     * Used for updating an existing Control on control Edit
     *
     * @param passedControl either constructed or found entity
     * @param state The {@link org.broadinstitute.gpinformatics.mercury.entity.sample.Control.ControlState} to which
     *              an existing control is to be set
     */
    public void saveControl(Control passedControl, Control.ControlState state) {
        Control foundControl = controlDao.findByCollaboratorParticipantId(passedControl.getBusinessKey());

        if(null == foundControl) {
            foundControl = passedControl;
        }

        foundControl.setState(state);

        controlDao.persist(foundControl);
    }
}
