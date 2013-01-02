package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a decision by a lab operator to rework a sample, usually due to a low quantification.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Rework {

    public enum ReworkType {
        /** Lab calls this Type 1 - rework one sample, and hold up the rest of the batch */
        ONE_SAMPLE_HOLD_REST_BATCH,
        /** Lab calls this Type 2 - rework one sample, let the rest of the batch proceed */
        ONE_SAMPLE_RELEASE_REST_BATCH,
        /** Lab calls this Type 3 - rework all samples in the batch */
        ENTIRE_BATCH
    }

    @Id
    @SequenceGenerator(name = "SEQ_REWORK", schema = "mercury", sequenceName = "SEQ_REWORK")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK")
    private Long reworkId;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel reworkedLabVessel;

    private String reworkMarkedStep;

    private String reworkBackToStep;

    @Enumerated(EnumType.STRING)
    private ReworkType reworkType;

    public Rework(LabVessel reworkedLabVessel, String reworkMarkedStep, String reworkBackToStep, ReworkType reworkType) {
        this.reworkedLabVessel = reworkedLabVessel;
        this.reworkMarkedStep = reworkMarkedStep;
        this.reworkBackToStep = reworkBackToStep;
        this.reworkType = reworkType;
    }

    public LabVessel getReworkedLabVessel() {
        return reworkedLabVessel;
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
}
