package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents additional 'data' that a message may need to convey
 * beyond normal transfers and reagents. e.g. DilutionFactor type
 * is used by BSP lims to determine correct pico quants for the
 * Cadence pico process.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "le_metadata")
public class LabEventMetadata {

    public enum LabEventMetadataType {
        DilutionFactor("DilutionFactor"),
        Volume("Volume"),  // volume is used for plate transfer because bettalims message doesn't permit well volumes.
        TaskId("TaskID");  // Use in arrays plating to pass BSP task ID from deck, to allow auto-export from BSP to GAP.

        private static final Map<String, LabEventMetadataType> MAP_NAME_TO_METADATA_TYPE =
                new HashMap<>(LabEventMetadataType.values().length);
        private final String displayName;

        LabEventMetadataType(String displayName) {
            this.displayName = displayName;
        }

        static {
            for (LabEventMetadataType metadataType : LabEventMetadataType.values()) {
                MAP_NAME_TO_METADATA_TYPE.put(metadataType.getDisplayName(), metadataType);
            }
        }

        public static LabEventMetadataType getByName(String positionName) {
            return MAP_NAME_TO_METADATA_TYPE.get(positionName);
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @SequenceGenerator(name = "SEQ_LAB_EVENT_METADATA", schema = "mercury", sequenceName = "SEQ_LAB_EVENT_METADATA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_EVENT_METADATA")
    @Id
    private Long labEventMetadataId;

    /**
     * The type of the value.
     */
    @Enumerated(EnumType.STRING)
    private LabEventMetadataType labEventMetadataType;

    private String value;

    /**
     * For JPA
     */
    public LabEventMetadata() {
    }

    public LabEventMetadata(
            LabEventMetadataType labEventMetadataType, String value) {
        this.labEventMetadataType = labEventMetadataType;
        this.value = value;
    }

    public Long getLabEventMetadataId() {
        return labEventMetadataId;
    }

    public LabEventMetadataType getLabEventMetadataType() {
        return labEventMetadataType;
    }

    public void setLabEventMetadataType(LabEventMetadataType labEventMetadataType) {
        this.labEventMetadataType = labEventMetadataType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}