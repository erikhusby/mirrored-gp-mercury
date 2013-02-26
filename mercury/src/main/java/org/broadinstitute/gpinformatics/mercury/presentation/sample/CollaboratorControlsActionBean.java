package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import clover.org.apache.commons.lang.StringUtils;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercuryControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 2/22/13
 *         Time: 4:32 PM
 */
@UrlBinding(value = CollaboratorControlsActionBean.ACTIONBEAN_URL_BINDING)
public class CollaboratorControlsActionBean extends CoreActionBean {

    @Inject
    private MercuryControlDao mercuryControlDao;

    private static final String VIEW_PAGE   = "/resources/sample/view_controls.jsp";
    //    private static final String EDIT_PAGE = "/resources/sample/edit_control.jsp";
    private static final String CREATE_PAGE = "/resources/sample/create_control.jsp";

    private static final String CONTROL_LIST_PAGE = "/resources/sample/list_controls.jsp";

    public final String mercuryControlParameter = "sampleCollaboratorId";

    public static final String ACTIONBEAN_URL_BINDING = "/resources/sample/controls.action";

    private static final String CURRENT_OBJECT = "Mercury Control";
    public static final  String CREATE_CONTROL = CoreActionBean.CREATE + CURRENT_OBJECT;
    public static        String EDIT_CONTROL   = CoreActionBean.EDIT + CURRENT_OBJECT;

    public final String positiveTypeValue = MercuryControl.ControlType.POSITIVE.getDisplayName();
    public final String negativeTypeValue = MercuryControl.ControlType.NEGATIVE.getDisplayName();

    public final String activeStateValue   = MercuryControl.ControlState.ACTIVE.getDisplayName();
    public final String inactiveStateValue = MercuryControl.ControlState.INACTIVE.getDisplayName();

    private String createControlType;
    private boolean editControlInactiveState;

    private MercuryControl workingControl;
    List<MercuryControl> positiveControls;
    List<MercuryControl> negativeControls;
    private String controlReference;

    @Before(stages = LifecycleStage.BindingAndValidation, on = {"!" + LIST_ACTION})
    public void init() {

        controlReference = getContext().getRequest().getParameter(mercuryControlParameter);

        if (StringUtils.isNotBlank(controlReference)) {
            workingControl = mercuryControlDao.findBySampleId(controlReference);
        } else {
            workingControl = new MercuryControl();
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        positiveControls = mercuryControlDao.findAllActivePositiveControls();
        negativeControls = mercuryControlDao.findAllActiveNegativeControls();

        return new ForwardResolution(CONTROL_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_CONTROL);
        return new ForwardResolution(CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_CONTROL);
        return new ForwardResolution(CREATE_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {

        if(isCreating()) {
            workingControl.setType(createControlType);
        } else {
            workingControl.setState(!editControlInactiveState);
        }

        mercuryControlDao.persist(workingControl);

        StringBuilder confirmMessage = new StringBuilder();

        confirmMessage.append(workingControl.getType().getDisplayName());
        confirmMessage.append(" Control " + workingControl.getCollaboratorSampleId());

        if(isCreating()) {
            confirmMessage.append(" has been created");
        } else {
            confirmMessage.append(" has been updated");
        }
        addMessage(confirmMessage.toString());

        String destination;
        if(editControlInactiveState) {
            destination = LIST_ACTION;
        } else {
            destination = VIEW_ACTION;
        }

        return new RedirectResolution(CollaboratorControlsActionBean.class, destination)
                       .addParameter(mercuryControlParameter, workingControl.getCollaboratorSampleId());
    }

    /* Accessors */
    public List<MercuryControl> getPositiveControls() {
        return positiveControls;
    }

    public List<MercuryControl> getNegativeControls() {
        return negativeControls;
    }

    public String getControlReference() {
        return controlReference;
    }

    public void setControlReference(String controlReference) {
        this.controlReference = controlReference;
    }

    public MercuryControl getWorkingControl() {
        return workingControl;
    }

    public void setWorkingControl(MercuryControl workingControl) {
        this.workingControl = workingControl;
    }

    public String getMercuryControlParameter() {
        return mercuryControlParameter;
    }

    public String getActiveStateValue() {
        return activeStateValue;
    }

    public String getInctiveStateValue() {
        return inactiveStateValue;
    }

    public String getPositiveTypeValue() {
        return positiveTypeValue;
    }

    public String getNegativeTypeValue() {
        return negativeTypeValue;
    }

    public String getCreateControlType() {
        return createControlType;
    }

    public void setCreateControlType(String createControlType) {
        this.createControlType = createControlType;
    }

    public boolean getEditControlInactiveState() {
        return editControlInactiveState;
    }

    public void setEditControlInactiveState(boolean editControlInactiveState) {
        this.editControlInactiveState = editControlInactiveState;
    }
}
