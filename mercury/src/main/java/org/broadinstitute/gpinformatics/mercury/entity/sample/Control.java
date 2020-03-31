package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a user defined control for Mercury.  Will encapsulate both the collaborator participant ID that is
 * associated with the control and an indicator of what kind of control (positive or negative) this is.
 *
 * @author Scott Matthews
 */
@Entity
@Audited
@Table(schema = "mercury", name = "mercury_control")
public class Control {

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

            for (ControlType type : values()) {
                if (StringUtils.equals(displayName, type.displayName)) {
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
            for (ControlState state : values()) {
                if (StringUtils.equals(displayName, state.displayName)) {
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
    private String collaboratorParticipantId;

    @Enumerated(EnumType.STRING)
    private ControlType type;

    @Enumerated(EnumType.STRING)
    @Index(name = "ix_mc_sample_key")
    private ControlState state = ControlState.ACTIVE;

    /**
     * Which aliquot's fingerprint to use for concordance checking.
     */
    @ManyToOne
    @JoinColumn(name = "CONCORDANCE_SAMPLE_ID")
    private MercurySample concordanceMercurySample;

    public Control() {
    }

    public Control(String collaboratorParticipantId, ControlType type) {
        this.collaboratorParticipantId = collaboratorParticipantId;
        this.type = type;
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public String getBusinessKey() {
        return getCollaboratorParticipantId();
    }

    public ControlType getType() {
        return type;
    }

    public ControlState getState() {
        return state;
    }

    public void setCollaboratorParticipantId(String collaboratorParticipantId) {
        this.collaboratorParticipantId = collaboratorParticipantId;
    }

    public void setType(ControlType type) {
        this.type = type;
    }

    public void setState(ControlState state) {
        this.state = state;
    }

    public boolean isActive() {
        return this.state == ControlState.ACTIVE;
    }

    public MercurySample getConcordanceMercurySample() {
        return concordanceMercurySample;
    }

    public void setConcordanceMercurySample(MercurySample concordanceMercurySample) {
        this.concordanceMercurySample = concordanceMercurySample;
    }
}
