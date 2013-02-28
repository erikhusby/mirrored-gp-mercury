package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercuryControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercuryControl;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

/**
 * Handles processing all interactions with the presentation tier with regards to Creating, Updating and retrieving
 * Mercury controls.
 *
 * @author Scott Matthews
 */
@UrlBinding(value = CollaboratorControlsActionBean.ACTIONBEAN_URL_BINDING)
public class CollaboratorControlsActionBean extends CoreActionBean {

    @Inject
    private MercuryControlDao mercuryControlDao;

    private static final String VIEW_PAGE               = "/resources/sample/view_control.jsp";
    private static final String CREATE_PAGE             = "/resources/sample/create_control.jsp";
    private static final String CONTROL_LIST_PAGE       = "/resources/sample/list_controls.jsp";
    private static final  String mercuryControlParameter = "sampleCollaboratorId";
    public static final  String ACTIONBEAN_URL_BINDING  = "/resources/sample/controls.action";
    private static final String CURRENT_OBJECT          = "Mercury Control";
    public static final  String CREATE_CONTROL          = CoreActionBean.CREATE + CURRENT_OBJECT;
    public static final  String EDIT_CONTROL            = CoreActionBean.EDIT + CURRENT_OBJECT;

    private String  createControlType;
    private boolean editControlInactiveState;

    private MercuryControl workingControl;
    List<MercuryControl> positiveControls;
    List<MercuryControl> negativeControls;
    private String controlReference;

    /**
     * Called before the execution of all Mercury Control related actions (except for the list page), this method
     * is responsible for making sure that there is a non null MercuryControl entity ready to interact with.
     *
     * If an existing entity can be found, this method will load it.  Otherwise, an empty entity is loaded instead
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"!" + LIST_ACTION})
    public void init() {

        controlReference = getContext().getRequest().getParameter(mercuryControlParameter);

        if (StringUtils.isNotBlank(controlReference)) {
            workingControl = mercuryControlDao.findBySampleId(controlReference);
        } else {
            workingControl = new MercuryControl();
        }
    }

    /**
     * This method is responsible for processing a user's request to view the Mercury Control List page.  It is
     * responsible for loading (if available) the list of positive and negative controls stored within Mercury
     *
     * @return an instance of a Stripes resolution that will forward the users request to the control list page
     */
    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        positiveControls = mercuryControlDao.findAllActiveControlsByType(MercuryControl.ControlType.POSITIVE);
        negativeControls = mercuryControlDao.findAllActiveControlsByType(MercuryControl.ControlType.NEGATIVE);

        return new ForwardResolution(CONTROL_LIST_PAGE);
    }

    /**
     * This method is responsible for processing a user's request to view the details of a single Mercury Control.
     *
     * @return an instance of a Stripes resolution that will forward the user's request to the view page
     */
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method is responsible for processing a users request to create a new Mercury Control.
     *
     * @return an instance of a Stripes resolution that will forward the users request to the create page
     */
    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_CONTROL);
        return new ForwardResolution(CREATE_PAGE);
    }

    /**
     * This method is responsible for processing a users request to edit the details of a single Mercury Control.
     *
     * @return an instance of a Stripes resolution that will forward the users request to the edit page
     */
    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_CONTROL);
        return new ForwardResolution(CREATE_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void makeControlInactiveValidation(ValidationErrors errors) {

        MercuryControl negativeVersion = mercuryControlDao.findInactiveBySampleId(controlReference);

        /*
        GPLIM-983:  Editing inactive controls was not a part of this user story.  The following logic can change once a
        user can view and revive inactive controls
         */
        if (editControlInactiveState && negativeVersion != null) {
            errors.add("controlName", new SimpleError("You cannot deactivate this control at this time.  There is " +
                                                              "already an inactive control that matches this."));
        }

    }

    /**
     * This method is responsible for processing a users request to save details of a single Mercury Control that they
     * have just created/modified
     *
     * @return an instance of a Stripes resolution that will redirect the users request to an appropriate page based on
     *         the users action
     */
    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {

        if (isCreating()) {
            /*
            If we are creating a new Control, the only value that has to be updated on the existing control object is the type
             */
            workingControl.setType(createControlType);
        } else {
            /*
            If we are editing an existing control, the only value that can be updated on the existing control object
            is the state.
             */
            workingControl.setState(editControlInactiveState ? MercuryControl.ControlState.INACTIVE
                                                             : MercuryControl.ControlState.ACTIVE);
        }

        mercuryControlDao.persist(workingControl);

        StringBuilder confirmMessage = new StringBuilder();

        confirmMessage.append(workingControl.getType().getDisplayName());
        confirmMessage.append(" Control " + workingControl.getCollaboratorSampleId());

        // Adjust the confirmation test based on the action the user took.
        if (isCreating()) {
            confirmMessage.append(" has been created");
        } else {
            confirmMessage.append(" has been updated");
        }
        addMessage(confirmMessage.toString());

        String destination;

        /*
            If a user has edited the State to be inactive, the control is no longer visible.  Therefore, going to the
            view page is both futile and confusing.  Take the user back to the List page in this case.
         */
        if (editControlInactiveState) {
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

    public String getPositiveTypeValue() {
        return MercuryControl.ControlType.POSITIVE.getDisplayName();
    }

    public String getNegativeTypeValue() {
        return MercuryControl.ControlType.NEGATIVE.getDisplayName();
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
