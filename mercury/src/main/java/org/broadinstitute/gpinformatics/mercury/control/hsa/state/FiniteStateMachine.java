package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Entity
@Audited
@Table(schema = "mercury")
public class FiniteStateMachine {

    public static final SimpleDateFormat DEMUX_FOLDER_FORMAT = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");

    @Id
    @SequenceGenerator(name = "SEQ_FINITE_STATE_MACHINE", schema = "mercury", sequenceName = "SEQ_FINITE_STATE_MACHINE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FINITE_STATE_MACHINE")
    @Column(name = "FINITE_STATE_MACHINE_ID")
    private Long finiteStateMachineId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "finiteStateMachine", orphanRemoval = true)
    private List<State> states;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "finiteStateMachine", orphanRemoval = true)
    private List<Transition> transitions;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Date dateQueued;

    private Date dateStarted;

    private Date dateCompleted;

    private String stateMachineName;

    public FiniteStateMachine() {
        dateQueued = new Date();
        status = Status.QUEUED;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public State getStartState() {
        return states.stream().filter(State::isStartState).findFirst().get();
    }

    public List<Transition> getTransitionsFromState(State state) {
        return transitions.stream()
                .filter(t -> t.getFromState().equals(state))
                .collect(Collectors.toList());
    }

    public int getNumberOfActiveIssues() {
        int ctr = 0;
        for (State state: getActiveStates()) {
            ctr += state.getTasksWithStatus(Status.SUSPENDED).size();
            ctr += state.getTasksWithStatus(Status.FAILED).size();
            if (state.getExitTask().isPresent()) {
                if (state.getExitTask().get().getStatus() == Status.SUSPENDED ||
                    state.getExitTask().get().getStatus() == Status.FAILED) {
                    ctr++;
                }
            }
        }
        return ctr;
    }

    public int getNumberOfTasksRunning() {
        int ctr = 0;
        for (State state: getActiveStates()) {
            ctr += state.getNumberOfRunningTasks();
        }
        return ctr;
    }

    public Long getFiniteStateMachineId() {
        return finiteStateMachineId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isComplete() {
        return states.stream().noneMatch(State::isAlive);
    }

    public Date getDateQueued() {
        return dateQueued;
    }

    public void setDateQueued(Date queuedTime) {
        this.dateQueued = queuedTime;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Date startedTime) {
        this.dateStarted = startedTime;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date completedTime) {
        this.dateCompleted = completedTime;
    }

    public String getStateMachineName() {
        return stateMachineName;
    }

    public void setStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
    }

    public List<State> getActiveStates() {
        return states.stream().filter(State::isAlive).collect(Collectors.toList());
    }

    public List<String> getActiveStateNames() {
        return states.stream().filter(State::isAlive).map(State::getStateName).collect(Collectors.toList());
    }

    public String getDateQueuedString() {
        return DEMUX_FOLDER_FORMAT.format(getDateQueued());
    }

    public boolean isAlive() {
        return getActiveStates().size() > 0;
    }

    public void removeState(State state) {
        states.remove(state);
        state.setFiniteStateMachine(null);
    }

    public <T extends State> Optional<T> getMostRecentCompleteStateOfType(Class<T> clazz) {
        return getStates().stream()
                .filter(state -> OrmUtil.proxySafeIsInstance(state, clazz) && state.isComplete())
                .map(state -> OrmUtil.proxySafeCast(state, clazz))
                .max(Comparator.comparing(State::getEndTime));
    }
}
