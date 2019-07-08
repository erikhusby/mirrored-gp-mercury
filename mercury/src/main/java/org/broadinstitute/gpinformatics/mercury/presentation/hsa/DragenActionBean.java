package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import net.sourceforge.stripes.validation.ValidationState;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

@UrlBinding(DragenActionBean.ACTION_BEAN_URL)
public class DragenActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(DragenActionBean.class);

    public static final String CREATE_MACHINE = CoreActionBean.CREATE + "Finite State Machine";
    private static final String EDIT_MACHINE = CoreActionBean.EDIT + "Finite State Machine";
    public static final String MACHINE_PARAMETER = "machine";

    public static final String ACTION_BEAN_URL = "/hsa/dragen.action";

    private static final String ORDER_CREATE_PAGE = "/hsa/create.jsp";
    private static final String DRAGEN_LIST_PAGE = "/hsa/list.jsp";

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @ValidateNestedProperties({
            @Validate(field = "stateMachineName", label = "State Machine Name", required = true, maxlength = 255,
                    on = {SAVE_ACTION})
    })
    private FiniteStateMachine editFiniteStateMachine;

    @Validate(required = true, on = {EDIT_ACTION})
    private String finiteStateMachineKey;

    @Validate(required = true, on = {SAVE_ACTION})
    private String runName;

    private IlluminaSequencingRun illuminaSequencingRun;

    private List<FiniteStateMachine> allActiveMachines;

    public DragenActionBean() {
        super(CREATE_MACHINE, EDIT_MACHINE, MACHINE_PARAMETER);
    }

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        finiteStateMachineKey = getContext().getRequest().getParameter("finiteStateMachine");
        if (!StringUtils.isBlank(finiteStateMachineKey)) {
            editFiniteStateMachine = stateMachineDao.findByIdentifier(finiteStateMachineKey);
        } else {
            editFiniteStateMachine = new FiniteStateMachine();
        }
        allActiveMachines = stateMachineDao.findByStatus(Status.RUNNING);
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(DRAGEN_LIST_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_MACHINE);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @ValidationMethod(on = {SAVE_ACTION}, when = ValidationState.ALWAYS)
    public void validateRunExists() {
        illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        if (illuminaSequencingRun == null) {
            addValidationError("runName", "Failed to find sequencing run with name " + runName);
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        MessageCollection messageCollection = new MessageCollection();

        try {
            FiniteStateMachine finiteStateMachine =
                    finiteStateMachineFactory.createFiniteStateMachineForRun(illuminaSequencingRun, messageCollection);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        } else {
            addMessage(getSubmitString() + " '" + editFiniteStateMachine.getStateMachineName() + "' was successful");
        }
        return new RedirectResolution(DragenActionBean.class, LIST_ACTION);
    }

    public FiniteStateMachine getEditFiniteStateMachine() {
        return editFiniteStateMachine;
    }

    public void setEditFiniteStateMachine(
            FiniteStateMachine editFiniteStateMachine) {
        this.editFiniteStateMachine = editFiniteStateMachine;
    }

    public String getFiniteStateMachineKey() {
        return finiteStateMachineKey;
    }

    public void setFiniteStateMachineKey(String finiteStateMachineKey) {
        this.finiteStateMachineKey = finiteStateMachineKey;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public List<FiniteStateMachine> getAllActiveMachines() {
        return allActiveMachines;
    }

    public void setAllActiveMachines(List<FiniteStateMachine> allActiveMachines) {
        this.allActiveMachines = allActiveMachines;
    }
}
