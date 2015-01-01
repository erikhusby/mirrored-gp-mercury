package org.broadinstitute.gpinformatics.mercury.entity.vessel;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a decision based on a LabMetric, e.g. Pass / Fail / Proceed on Risk
 */
@Entity
@Audited
@Table(schema = "mercury")
public class LabMetricDecision {

    public enum Decision {
        PASS(false),
        RUN_FAILED(false),
        FAIL(true),
        RISK(true);

        private boolean editable;

        Decision(boolean editable) {
            this.editable = editable;
        }

        public boolean isEditable() {
            return editable;
        }

        private static List<Decision> editableDecisions = new ArrayList<>();
        static {
            for (Decision decision : Decision.values()) {
                if (decision.isEditable()) {
                    editableDecisions.add(decision);
                }
            }
        }

        public static List<Decision> getEditableDecisions() {
            return editableDecisions;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LAB_METRIC_DECISION", schema = "mercury", sequenceName = "SEQ_LAB_METRIC_DECISION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_METRIC_DECISION")
    private Long labMetricDecisionId;

    @Enumerated(EnumType.STRING)
    private Decision decision;

    private String overrideReason;

    private Date decidedDate;

    private Long deciderUserId;

    /** This is actually OneToOne, but using OneToMany to avoid N+1 selects */
    @OneToMany(mappedBy = "labMetricDecision")
    private Set<LabMetric> labMetrics = new HashSet<>();

    /** For JPA */
    protected LabMetricDecision() {
    }

    public LabMetricDecision(Decision decision, Date decidedDate, Long deciderUserId,
            LabMetric labMetric) {
        this.decision = decision;
        this.decidedDate = decidedDate;
        this.deciderUserId = deciderUserId;
        labMetrics.add(labMetric);
    }

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

    public void setDecidedDate(Date date) {
        this.decidedDate = date;
    }

    public Long getDeciderUserId() {
        return deciderUserId;
    }

    public void setDeciderUserId(Long deciderUserId) {
        this.deciderUserId = deciderUserId;
    }

    public LabMetric getLabMetrics() {
        if (labMetrics.isEmpty()) {
            return null;
        }
        return labMetrics.iterator().next();
    }
}
