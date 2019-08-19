package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    @JoinColumn(name = "state")
    private State state;

    @Enumerated(EnumType.STRING)
    private TaskActionTime taskActionTime;

    private String taskName;

    private Date startTime;

    private Date endTime;

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
}
