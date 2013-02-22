package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

    /**
     * Helps define the type of control stored in the system.
     */
    public enum CONTROL_TYPE {
        POSITIVE, NEGATIVE
    }

    /**
     * Instead of deleting controls, this status defines whether a control can be utilized.
     */
    public enum CONTROL_STATE {

        /**
         * Controls marked with Active can be used and viewed within the application
         */
        ACTIVE,

        /**
         * Controls marked with Inactive are not to be used.  The main Mercury view of controls will not even list
         * them as an option
         */
        INACTIVE
    }

    @Id
    @SequenceGenerator(name = "SEQ_MERCURY_CONTROL", schema = "mercury", sequenceName = "SEQ_MERCURY_CONTROL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MERCURY_CONTROL")
    private Long mercuryControlId;

    @Index(name = "ix_mc_sample_key")
    private String collaboratorSampleId;

    @Enumerated(EnumType.STRING)
    private CONTROL_TYPE type;

    @Enumerated(EnumType.STRING)
    @Index(name = "ix_mc_sample_key")
    private CONTROL_STATE state;

    MercuryControl() {
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

    /**
     * easily flips the current state of a control from Active->Inactive or Vice Versa
     */
    public void toggleState() {
        if(this.state == CONTROL_STATE.ACTIVE) {
            this.state = CONTROL_STATE.INACTIVE;
        } else {
            this.state = CONTROL_STATE.ACTIVE;
        }
    }
}
