package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.presentation.hsa.TaskDecision;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

@Entity
@Audited
@Table(schema = "mercury",
        uniqueConstraints = @UniqueConstraint(columnNames = {"TASK_NAME", "STATE"}))
public abstract class Task {

    public enum TaskActionTime {
        ENTRY, EXIT, DEFAULT
    }

    @Id
    @SequenceGenerator(name = "SEQ_TASK", schema = "mercury", sequenceName = "SEQ_TASK")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TASK")
    @Column(name = "TASK_ID")
    private Long taskId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @BatchSize(size = 20)
    @JoinColumn(name = "state")
    private State state;

    @Enumerated(EnumType.STRING)
    private TaskActionTime taskActionTime;

    /** This is actually OneToOne, but using ManyToOne to avoid N+1 selects */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "TASK_DECISION")
    private TaskDecision taskDecision;

    private String taskName;

    private Date startTime;

    private Date endTime;

    private Date queuedTime;

    private String errorMessage;

    public Task() {
        status = Status.QUEUED;
        taskActionTime = TaskActionTime.DEFAULT;
        startTime = new Date();
    }

    public Task(Status status) {
        this.status = status;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Date getQueuedTime() {
        return queuedTime;
    }

    public void setQueuedTime(Date queuedTime) {
        this.queuedTime = queuedTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public TaskActionTime getTaskActionTime() {
        return taskActionTime;
    }

    public void setTaskActionTime(TaskActionTime taskActionTime) {
        this.taskActionTime = taskActionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public TaskDecision getTaskDecision() {
        return taskDecision;
    }

    public void setTaskDecision(TaskDecision taskDecision) {
        this.taskDecision = taskDecision;
    }

    public boolean isComplete() {
        return getStatus() == Status.COMPLETE;
    }

    public boolean isOverrideOutOfSpec() {
        if (taskDecision == null) {
            return false;
        }
        return taskDecision.getDecision() == TaskDecision.Decision.OVERRIDE_TO_IN_SPEC;
    }
}
