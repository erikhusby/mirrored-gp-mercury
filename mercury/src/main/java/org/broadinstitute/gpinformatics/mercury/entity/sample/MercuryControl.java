package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Scott Matthews
 *
 * Represents a user defined control for Mercury.  Will encapsulate both the collaborator sample ID that is associated
 * with the control and an indicator of what kind of control (positive or negative) this is.
 *
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MercuryControl {

    public enum CONTROL_TYPE {
        POSITIVE, NEGATIVE;
    }

    public enum CONTROL_STATE {
        ACTIVE, INACTIVE;
    }

    @Id
    @SequenceGenerator(name = "SEQ_MERCURY_CONTROL", schema = "mercury", sequenceName = "SEQ_MERCURY_CONTROL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MERCURY_CONTROL")
    private Long mercuryControlId;

    @Index(name = "ix_mc_sample_key")
    private String collaboratorSampleId;

    private CONTROL_TYPE type;

    @Index(name = "ix_mc_sample_key")
    private CONTROL_STATE state;

    public MercuryControl() {
    }

    public MercuryControl(String collaboratorSampleId, CONTROL_TYPE type) {
        this.collaboratorSampleId = collaboratorSampleId;
        this.type = type;
        state = CONTROL_STATE.ACTIVE;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public CONTROL_TYPE getType() {
        return type;
    }

    public CONTROL_STATE getState() {
        return state;
    }

    public void toggleState() {
        if(this.state == CONTROL_STATE.ACTIVE) {
            this.state = CONTROL_STATE.INACTIVE;
        } else {
            this.state = CONTROL_STATE.ACTIVE;
        }
    }
}
