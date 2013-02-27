package org.broadinstitute.gpinformatics.mercury.entity.sample;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
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
 *
 * Represents a user defined control for Mercury.  Will encapsulate both the collaborator sample ID that is
 * associated
 * with the control and an indicator of what kind of control (positive or negative) this is.
 *
 * @author Scott Matthews
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MercuryControl {

    /**
     * Helps define the type of control stored in the system.
     */
    public enum ControlType {
        POSITIVE("Positive"), NEGATIVE("Negative");

        private final String displayName;

        private ControlType(String displayName) {
            this.displayName = displayName;
        }

        public static ControlType findByDisplayName(String displayName) {

            for(ControlType type:values()) {
                if(StringUtils.equals(displayName, type.displayName)) {
                    return type;
                }
            }
            throw new InformaticsServiceException("A matching control type was not found to match " + displayName);
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Instead of deleting controls, this status defines whether a control can be utilized.
     */
    public enum ControlState {

        /**
         * Controls marked with Active can be used and viewed within the application
         */
        ACTIVE("Active"),

        /**
         * Controls marked with Inactive are not to be used.  The main Mercury view of controls will not even list
         * them as an option
         */
        INACTIVE("Inactive");

        private final String displayName;

        private ControlState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ControlState findByDisplayName(String displayName) {
            for(ControlState state:values()) {
                if(StringUtils.equals(displayName, state.displayName)) {
                    return state;
                }
            }

            throw new InformaticsServiceException("No matching control state was found to match " + displayName);
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_MERCURY_CONTROL", schema = "mercury", sequenceName = "SEQ_MERCURY_CONTROL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MERCURY_CONTROL")
    private Long mercuryControlId;

    @Index(name = "ix_mc_sample_key")
    private String collaboratorSampleId;

    @Enumerated(EnumType.STRING)
    private ControlType type;

    @Enumerated(EnumType.STRING)
    @Index(name = "ix_mc_sample_key")
    private ControlState state = ControlState.ACTIVE;

    public MercuryControl() {
    }

    public MercuryControl(String collaboratorSampleId, ControlType type) {
        this.collaboratorSampleId = collaboratorSampleId;
        this.type = type;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public ControlType getType() {
        return type;
    }

    public ControlState getState() {
        return state;
    }

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public void setType(ControlType type) {
        this.type = type;
    }

    public void setType(String type) {
        this.type = ControlType.findByDisplayName(type);
    }

    public void setState(ControlState state) {
        this.state = state;
    }

    public void setState(boolean state) {
        this.state = state?ControlState.ACTIVE:ControlState.INACTIVE;
    }

    public boolean isInactive() {
        return this.state == ControlState.INACTIVE;
    }

    /**
     * easily flips the current state of a control from Active->Inactive or Vice Versa
     */
    public void toggleState() {
        if (this.state == ControlState.ACTIVE) {
            this.state = ControlState.INACTIVE;
        } else {
            this.state = ControlState.ACTIVE;
        }
    }
}
