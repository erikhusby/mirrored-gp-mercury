package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.control.vessel.MetricReworkDisposition;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
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
        REPEAT(true),
        @Deprecated   // Prefer REPEAT with rework disposition and note
                BAD_TRIP(true),
        @Deprecated   // Prefer REPEAT with rework disposition and note
                OVER_THE_CURVE(true),
        @Deprecated   // Prefer REPEAT with rework disposition and note
                TEN_PERCENT_DIFF_REPEAT(true),
        FAIL(true),
        RISK(true),
        FAIL_ACCEPTANCE_CRITERIA(true);

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

    public enum NeedsReview {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        NeedsReview(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LAB_METRIC_DECISION", schema = "mercury", sequenceName = "SEQ_LAB_METRIC_DECISION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_METRIC_DECISION")
    private Long labMetricDecisionId;

    @Enumerated(EnumType.STRING)
    private Decision decision;

    @Enumerated(EnumType.STRING)
    private NeedsReview needsReview;

    private String overrideReason;

    private Date decidedDate;

    private Long deciderUserId;

    @Enumerated(EnumType.STRING)
    private MetricReworkDisposition reworkDisposition;

    private String note;

    /**
     * This is actually OneToOne, but using OneToMany to avoid N+1 selects
     * TODO JMS What use-case requires this to be bi-directional?  Always comes from LabMetric?
     */
    @OneToMany(mappedBy = "labMetricDecision")
    private Set<LabMetric> labMetrics = new HashSet<>();

    /** For JPA */
    protected LabMetricDecision() {
    }

    public LabMetricDecision(Decision decision, Date decidedDate, Long deciderUserId,
                             LabMetric labMetric) {
        this(decision, decidedDate, deciderUserId, labMetric, null);
    }

    public LabMetricDecision(Decision decision, Date decidedDate, Long deciderUserId,
            LabMetric labMetric, @Nullable String note) {
        this(decision, decidedDate, deciderUserId, labMetric, note, NeedsReview.FALSE);
    }

    public LabMetricDecision(Decision decision, Date decidedDate, Long deciderUserId,
                             LabMetric labMetric, @Nullable String note, NeedsReview needsReview) {
        this.decision = decision;
        this.decidedDate = decidedDate;
        this.deciderUserId = deciderUserId;
        this.note = note;
        this.needsReview = needsReview;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isNeedsReview() {
        return needsReview == NeedsReview.TRUE.TRUE;
    }

    public MetricReworkDisposition getReworkDisposition() {
        return reworkDisposition;
    }

    public void setReworkDisposition(MetricReworkDisposition reworkDisposition) {
        this.reworkDisposition = reworkDisposition;
    }

    public void setNeedsReview(
            NeedsReview needsReview) {
        this.needsReview = needsReview;
    }

    public LabMetric getLabMetrics() {
        if (labMetrics.isEmpty()) {
            return null;
        }
        return labMetrics.iterator().next();
    }
}
