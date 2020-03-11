package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury")
public class TaskDecision {

    public enum Decision {
        PASS(false),
        REWORK(true),
        TOP_OFF(true),
        OVERRIDE_TO_IN_SPEC(true),
        MARK_OUT_OF_SPEC(true),
        ;

        private boolean editable;

        private static Map<String, TaskDecision.Decision> MAP_NAME_TO_DECISION = new HashMap<>();

        Decision(boolean editable) {
            this.editable = editable;
        }

        public boolean isEditable() {
            return editable;
        }

        private static List<TaskDecision.Decision> editableDecisions = new ArrayList<>();
        static {
            for (TaskDecision.Decision decision : TaskDecision.Decision.values()) {
                if (decision.isEditable()) {
                    editableDecisions.add(decision);
                }
            }

            for (TaskDecision.Decision decision: TaskDecision.Decision.values()) {
                MAP_NAME_TO_DECISION.put(decision.name(), decision);
            }
        }

        public static List<TaskDecision.Decision> getEditableDecisions() {
            return editableDecisions;
        }

        public static TaskDecision.Decision getDecisionByName(String name) {
            return MAP_NAME_TO_DECISION.get(name);
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_TASK_DECISION", schema = "mercury", sequenceName = "SEQ_TASK_DECISION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TASK_DECISION")
    private Long taskDecisionId;

    @Enumerated(EnumType.STRING)
    private TaskDecision.Decision decision;

    /** This is actually OneToOne, but using OneToMany to avoid N+1 selects */
    @OneToMany(mappedBy = "taskDecision")
    private Set<Task> tasks = new HashSet<>();

    public TaskDecision() {
    }

    public TaskDecision(Decision decision, Task task, String overrideReason, Date decidedDate, Long deciderUserId, String note) {
        this.decision = decision;
        this.overrideReason = overrideReason;
        this.decidedDate = decidedDate;
        this.deciderUserId = deciderUserId;
        this.note = note;
        tasks.add(task);
    }

    private String overrideReason;

    private Date decidedDate;

    private Long deciderUserId;

    private String note;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public Date getDecidedDate() {
        return decidedDate;
    }

    public void setDecidedDate(Date decidedDate) {
        this.decidedDate = decidedDate;
    }

    public Long getDeciderUserId() {
        return deciderUserId;
    }

    public void setDeciderUserId(Long deciderUserId) {
        this.deciderUserId = deciderUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Task getTask() {
        if (tasks.isEmpty()) {
            return null;
        }
        return tasks.iterator().next();
    }
}
