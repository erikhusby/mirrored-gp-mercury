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
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SlurmController;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@UrlBinding(FiniteStateMachineActionBean.ACTION_BEAN_URL)
public class FiniteStateMachineActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(FiniteStateMachineActionBean.class);

    public static final String CREATE_MACHINE = CoreActionBean.CREATE + "Finite State Machine";
    public static final String CREATE_DEMULTIPLEX = CoreActionBean.CREATE + "Demultiplex Task";
    private static final String EDIT_MACHINE = CoreActionBean.EDIT + "Finite State Machine";
    public static final String MACHINE_PARAMETER = "machine";

    public static final String ACTION_BEAN_URL = "/hsa/workflows/dragen.action";

    private static final String WORKFLOW_CREATE_PAGE = "/hsa/workflows/create.jsp";
    private static final String DRAGEN_LIST_PAGE = "/hsa/workflows/list.jsp";
    private static final String DRAGEN_VIEW_PAGE = "/hsa/workflows/view.jsp";
    private static final String DEMULTIPLEX_CREATE_PAGE = "/hsa/workflows/create_demultiplex.jsp";

    public static final String CUSTOMIZE_ACTION = "customize";
    public static final String CREATE_DEMULTIPLEX_ACTION = "createDemultiplex";
    public static final String UPDATE_STATE_STATUS_ACTION = "updateStateStatus";
    public static final String UPDATE_TASK_STATUS_ACTION = "updateTaskStatus";

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private TaskDao taskDao;

    @Inject
    private SlurmController slurmController;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @ValidateNestedProperties({
            @Validate(field = "stateMachineName", label = "State Machine Name", required = true, maxlength = 255,
                    on = {SAVE_ACTION})
    })
    private FiniteStateMachine editFiniteStateMachine;

    @Validate(required = true, on = {EDIT_ACTION, VIEW_ACTION})
    private String finiteStateMachineKey;

    @Validate(required = true, on = {SAVE_ACTION})
    private String runName;

    private IlluminaSequencingRun illuminaSequencingRun;

    private List<FiniteStateMachine> allActiveMachines;

    private List<Long> selectedIds = new ArrayList<>();

    private String sampleIds;

    private Status overrideStatus;

    private Map<String, MercurySample> mapIdToMercurySample = new HashMap<>();

    public FiniteStateMachineActionBean() {
        super(CREATE_MACHINE, EDIT_MACHINE, MACHINE_PARAMETER);
    }

    /**
     * Initialize the machine with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        finiteStateMachineKey = getContext().getRequest().getParameter("finiteStateMachineKey");
        if (!StringUtils.isBlank(finiteStateMachineKey)) {
            editFiniteStateMachine = stateMachineDao.findById(FiniteStateMachine.class, Long.valueOf(finiteStateMachineKey));
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
        return new ForwardResolution(WORKFLOW_CREATE_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(DRAGEN_VIEW_PAGE);
    }

    @ValidationMethod(on = {SAVE_ACTION}, when = ValidationState.ALWAYS)
    public void validateRunExists() {
        illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        if (illuminaSequencingRun == null) {
            addValidationError("runName", "Failed to find sequencing run with name " + runName);
        }

        if (sampleIds != null) {
            String[] split = sampleIds.split("\\s+");
            mapIdToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(new HashSet<>(Arrays.asList(split)));

            for (String sampleId: split) {
                if (mapIdToMercurySample.get(sampleId) == null) {
                    addValidationError("sampleIds", "Failed to find Sample: " + sampleId);
                }
            }
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        MessageCollection messageCollection = new MessageCollection();

        try {
            FiniteStateMachine finiteStateMachine = finiteStateMachineFactory.createFiniteStateMachineForRun(
                    illuminaSequencingRun, null, runName, mapIdToMercurySample.keySet(), messageCollection);
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
            return new ForwardResolution(getContext().getSourcePage());
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        } else {
            addMessage(getSubmitString() + " '" + editFiniteStateMachine.getStateMachineName() + "' was successful");
        }
        return new RedirectResolution(FiniteStateMachineActionBean.class, LIST_ACTION);
    }

    @ValidationMethod(on = {UPDATE_TASK_STATUS_ACTION, UPDATE_STATE_STATUS_ACTION})
    public void validateUpdateStatus() {
        if (selectedIds == null || selectedIds.size() == 0) {
            addGlobalValidationError("Must check at least one row.");
        }
    }

    /**
     * Handle changing a task's state. Cancels a dragen job if its running and new status is cancelled
     * TODO Determine if its safe to actually cancel. See a few spots in the beginning where it says 'Do Not Interrupt'
     */
    @HandlesEvent(UPDATE_TASK_STATUS_ACTION)
    public Resolution updateTaskStatus() {
        List<Task> tasks = taskDao.findTasksById(selectedIds);
        for (Task task: tasks) {
            if (overrideStatus == Status.CANCELLED &&
                (task.getStatus() == Status.RUNNING || task.getStatus() == Status.QUEUED)
                && OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
                ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
                if (slurmController.cancelJob(String.valueOf(processTask.getProcessId()))) {
                    task.setStatus(overrideStatus);
                }
            } else {
                task.setStatus(overrideStatus);
            }
        }
        tasks.forEach(t -> t.setStatus(overrideStatus));
        taskDao.persistAll(tasks);
        addMessage(String.format("%d tasks updated to: %s.", tasks.size(), overrideStatus.getStatusName()));
        return new ForwardResolution(DRAGEN_VIEW_PAGE).addParameter("finiteStateMachineKey", finiteStateMachineKey);
    }

    // TODO On cancel, it should look for all active tasks and cancel the runs
    @HandlesEvent(UPDATE_STATE_STATUS_ACTION)
    public Resolution updateMachineStatus() {
        List<FiniteStateMachine> states = stateMachineDao.findStatesById(selectedIds);
        states.forEach(t -> t.setStatus(overrideStatus));
        stateMachineDao.persistAll(states);
        addMessage(String.format("%d states updated to: %s.", states.size(), overrideStatus.getStatusName()));
        return new ForwardResolution(DRAGEN_LIST_PAGE);
    }

    @HandlesEvent(CREATE_DEMULTIPLEX_ACTION)
    public Resolution createDemultiplex() {
        setSubmitString(CREATE_DEMULTIPLEX);
        return new ForwardResolution(DEMULTIPLEX_CREATE_PAGE);
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

    public List<Long> getSelectedIds() {
        return selectedIds;
    }

    public void setSelectedIds(List<Long> selectedIds) {
        this.selectedIds = selectedIds;
    }

    public Status getOverrideStatus() {
        return overrideStatus;
    }

    public void setOverrideStatus(Status overrideStatus) {
        this.overrideStatus = overrideStatus;
    }

    public String getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(String sampleIds) {
        this.sampleIds = sampleIds;
    }
}
