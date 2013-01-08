package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a decision by a lab operator to rework a sample, usually due to a low quantification.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Rework {

    /** For JPA */
    Rework() {
    }

    public enum ReworkType {
        /** Lab calls this Type 1 - rework one sample, and hold up the rest of the batch */
        ONE_SAMPLE_HOLD_REST_BATCH,
        /** Lab calls this Type 2 - rework one sample, let the rest of the batch proceed */
        ONE_SAMPLE_RELEASE_REST_BATCH,
        /** Lab calls this Type 3 - rework all samples in the batch */
        ENTIRE_BATCH
    }

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_REWORK", schema = "mercury", sequenceName = "SEQ_REWORK")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK")
    private Long reworkId;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<LabVessel> reworkedLabVessels = new HashSet<LabVessel>();

    private String reworkMarkedStep; // need process too?

    private String reworkBackToStep; // need process too?

    @Enumerated(EnumType.STRING)
    private ReworkType reworkType;

    // todo jmt separate entity for rap sheet entries?
    private String comment;

    private Date dateMarked;

    private Long userId;

    // category? (reagent, automation hardware failure, bad hair day)

    public Rework(String reworkMarkedStep, String reworkBackToStep, ReworkType reworkType, String comment,
            Date dateMarked, Long userId) {
        this.reworkMarkedStep = reworkMarkedStep;
        this.reworkBackToStep = reworkBackToStep;
        this.reworkType = reworkType;
        this.comment = comment;
        this.dateMarked = dateMarked;
        this.userId = userId;
    }

    public String getReworkMarkedStep() {
        return reworkMarkedStep;
    }

    public String getReworkBackToStep() {
        return reworkBackToStep;
    }

    public ReworkType getReworkType() {
        return reworkType;
    }

    public void addReworkedLabVessel(LabVessel labVessel) {
        reworkedLabVessels.add(labVessel);
        labVessel.addRework(this);
    }

    public Set<LabVessel> getReworkedLabVessels() {
        return reworkedLabVessels;
    }

    public String getComment() {
        return comment;
    }

    public Date getDateMarked() {
        return dateMarked;
    }

    public Long getUserId() {
        return userId;
    }
}
