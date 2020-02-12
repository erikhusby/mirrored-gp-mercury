package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.GsUtilTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "FINITE_STATE_MACHINE")
    private FiniteStateMachine finiteStateMachine;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury", name = "sample_alignment_state"
            , joinColumns = {@JoinColumn(name = "ALIGNMENT_STATE")}
            , inverseJoinColumns = {@JoinColumn(name = "MERCURY_SAMPLE")})
    @BatchSize(size = 20)
    private Set<MercurySample> mercurySamples = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(schema = "mercury", name = "src_demultiplix_state"
            , joinColumns = {@JoinColumn(name = "DEMULTIPLEX_STATE")}
            , inverseJoinColumns = {@JoinColumn(name = "SEQUENCING_RUN_CHAMBER")})
    @BatchSize(size = 20)
    private Set<IlluminaSequencingRunChamber> sequencingRunChambers = new HashSet<>();

    private String stateName;

    private boolean alive;

    private boolean startState;

    private Date startTime;

    private Date endTime;

    public State(String stateName, FiniteStateMachine finiteStateMachine, Set<MercurySample> mercurySamples,
                 Set<IlluminaSequencingRunChamber> sequencingRunChambers) {
        this.stateName = stateName;
        this.finiteStateMachine = finiteStateMachine;

        for (MercurySample mercurySample: mercurySamples) {
            mercurySample.addState(this);
        }
        for (IlluminaSequencingRunChamber sequencingRunChamber: sequencingRunChambers) {
            sequencingRunChamber.addState(this);
        }
    }

    public State() {
    }

    public IlluminaSequencingRun getRun() {
        if (sequencingRunChambers != null && !sequencingRunChambers.isEmpty()) {
            SequencingRunChamber runChamber = sequencingRunChambers.iterator().next();
            if (OrmUtil.proxySafeIsInstance(runChamber, IlluminaSequencingRunChamber.class)) {
                IlluminaSequencingRunChamber illuminaSequencingRunChamber =
                        OrmUtil.proxySafeCast(runChamber, IlluminaSequencingRunChamber.class);
                return illuminaSequencingRunChamber.getIlluminaSequencingRun();
            }

        }
        return null;
    }

    public void addSequencingRunChamber(IlluminaSequencingRunChamber sequencingRunChamber) {
        sequencingRunChambers.add(sequencingRunChamber);
    }

    public Set<IlluminaSequencingRunChamber> getSequencingRunChambers() {
        return sequencingRunChambers;
    }

    public void setSequencingRunChambers(
            Set<IlluminaSequencingRunChamber> sequencingRunChamberList) {
        this.sequencingRunChambers = sequencingRunChamberList;
    }

    public void removeRunChamber(IlluminaSequencingRunChamber sequencingRunChamber) {
        sequencingRunChambers.remove(sequencingRunChamber);
        sequencingRunChamber.getStates().remove(this);
    }

    public Set<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public void setMercurySamples(
            Set<MercurySample> mercurySamples) {
        this.mercurySamples = mercurySamples;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setState(this);
    }

    public void addTasks(Set<Task> tasks) {
        tasks.forEach(this::addTask);
    }

    public Set<Task> getTasks() {
        return tasks.stream()
                .filter(t -> t.getTaskActionTime() == Task.TaskActionTime.DEFAULT)
                .collect(Collectors.toSet());
    }

    public Set<Task> getTasksFromStatusList(List<Status> status) {
        return tasks.stream()
                .filter(t -> t.getTaskActionTime() == Task.TaskActionTime.DEFAULT &&
                             status.contains(t.getStatus()))
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

    public Long getStateId() {
        return stateId;
    }

    public Optional<Task> getExitTask() {
        return tasks.stream().filter(t -> t.getTaskActionTime() == Task.TaskActionTime.EXIT).findFirst();
    }

    public boolean isExitTaskPending() {
        return getExitTask().isPresent() && getExitTask().get().getStatus() == Status.QUEUED ||
               getExitTask().get().getStatus() == Status.RETRY;
    }

    public List<Task> getRunningTasks() {
        return getTasks().stream()
                .filter(t -> t.getStatus() == Status.RUNNING || t.getStatus() == Status.UNKNOWN)
                .collect(Collectors.toList());
    }

    public List<Task> getActiveTasks() {
        return getTasks().stream()
                .filter(t -> t.getStatus() == Status.RUNNING || t.getStatus() == Status.QUEUED || t.getStatus() == Status.UNKNOWN)
                .collect(Collectors.toList());
    }

    public boolean isStateOnEnter() {
        if (getStartTime() == null) {
            return true;
        }

        Optional<Task> first = getTasks().stream()
                .filter(t -> t.getStatus() != Status.QUEUED ||
                             !OrmUtil.proxySafeIsInstance(t, ProcessTask.class) ||
                             OrmUtil.proxySafeCast(t, ProcessTask.class).getProcessId() != null)
                .findFirst();
        return !first.isPresent();
    }

    public boolean isMainTasksComplete() {
        return getTasks().stream().allMatch(t -> t.getStatus() == Status.COMPLETE || t.getStatus() == Status.CANCELLED
         || t.getStatus() == Status.IGNORE);
    }

    public long getNumberOfRunningTasks() {
        return getTasks().stream().filter(t -> t.getStatus() == Status.RUNNING).count();
    }

    public long getNumberOfQueuedTasks() {
        return getTasks().stream().filter(t -> t.getStatus() == Status.QUEUED).count();
    }

    public long getNumberOfFailedTasks() {
        return getTasks().stream().filter(t -> t.getStatus() == Status.FAILED).count();
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

    public boolean isUpload() {
        return getTasks().stream().anyMatch(t -> OrmUtil.proxySafeIsInstance(t, GsUtilTask.class));
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

    public void addSample(MercurySample mercurySample) {
        this.mercurySamples.add(mercurySample);
    }

    public boolean doesContainSample(MercurySample mercurySample) {
        if (getMercurySamples().contains(mercurySample)) {
            return true;
        } else {
            for (LabVessel labVessel: mercurySample.getLabVessel()) {
                for (SampleInstanceV2 sampleInstanceV2: labVessel.getSampleInstancesV2()) {
                    if (sampleInstanceV2.getRootOrEarliestMercurySample().equals(mercurySample)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void removeSample(MercurySample mercurySample) {
        mercurySamples.remove(mercurySample);
    }

    public static class StateStartComparator implements Comparator<State> {

        @Override
        public int compare(State a, State b) {
            if (a.getStartTime() != null && b.getStartTime() != null) {
                return a.getStartTime().compareTo(b.getStartTime());
            } else if (a.getEndTime() != null && b.getEndTime() != null) {
                return a.getEndTime().compareTo(b.getEndTime());
            } else {
                return a.getStateId().compareTo(b.getStateId());
            }
        }
    }
}
