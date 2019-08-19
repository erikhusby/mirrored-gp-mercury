package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class State {

    @Id
    @SequenceGenerator(name = "SEQ_STATE", schema = "mercury", sequenceName = "SEQ_STATE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STATE")
    @Column(name = "STATE_ID")
    private Long stateId;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY, mappedBy = "state")
    private Set<Task> tasks = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "FINITE_STATE_MACHINE")
    private FiniteStateMachine finiteStateMachine;

    private String stateName;

    private boolean alive;

    private boolean startState;

    private Date startTime;

    private Date endTime;

    public State() {
    }

    public State(String stateName, FiniteStateMachine finiteStateMachine) {
        this.stateName = stateName;
        this.finiteStateMachine = finiteStateMachine;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setState(this);
    }

    public Set<Task> getTasks() {
        return tasks.stream()
                .filter(t -> t.getTaskActionTime() == Task.TaskActionTime.DEFAULT)
                .collect(Collectors.toSet());
    }

    public Set<Task> getTasksWithStatus(Status status) {
        return tasks.stream()
                .filter(t -> t.getTaskActionTime() == Task.TaskActionTime.DEFAULT &&
                             t.getStatus() == status)
                .collect(Collectors.toSet());
    }

    public void setTasks(Set<Task> tasks) {
        this.tasks = tasks;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String name) {
        this.stateName = name;
    }

    public boolean isStartState() {
        return startState;
    }

    public void setStartState(boolean start) {
        this.startState = start;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean active) {
        this.alive = active;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public FiniteStateMachine getFiniteStateMachine() {
        return finiteStateMachine;
    }

    public void setFiniteStateMachine(FiniteStateMachine finiteStateMachine) {
        this.finiteStateMachine = finiteStateMachine;
    }

    public void OnEnter() {

    }

    public Optional<Task> getExitTask() {
        return tasks.stream().filter(t -> t.getTaskActionTime() == Task.TaskActionTime.EXIT).findFirst();
    }

    public boolean isExitTaskPending() {
        return getExitTask().isPresent() && getExitTask().get().getStatus() == Status.QUEUED ||
               getExitTask().get().getStatus() == Status.RETRY;
    }

    public List<Task> getActiveTasks() {
        return getTasks().stream().filter(t -> t.getStatus() == Status.RUNNING).collect(Collectors.toList());
    }

    public boolean isMainTasksComplete() {
        return getTasks().stream().allMatch(t -> t.getStatus() == Status.COMPLETE || t.getStatus() == Status.CANCELLED);
    }

    public boolean isComplete() {
        boolean mainTasksComplete = isMainTasksComplete();
        boolean exitTasksComplete = !getExitTask().isPresent() || getExitTask().get().getStatus() == Status.COMPLETE;
        return mainTasksComplete && exitTasksComplete;
    }

    public void addExitTask(Task task) {
        task.setTaskActionTime(Task.TaskActionTime.EXIT);
        tasks.add(task);
        task.setState(this);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("stateId", stateId)
                .append("stateName", stateName)
                .append("finiteStateMachine", finiteStateMachine)
                .append("alive", alive)
                .append("tasks", tasks)
                .append("startState", startState)
                .append("startTime", startTime)
                .append("end", endTime)
                .toString();
    }
}
