package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.Map;

/**
 * A control that represents a Unique Molecular Identifier
 */
@Entity
@Audited
public class UMIReagent extends Reagent {

    public enum UMILocation {
        INLINE_FIRST_READ("Inline First Read"),
        AFTER_FIRST_INDEX_READ("After First Index Read"),
        INLINE_SECOND_READ("Inline Second Read");

        private final String displayName;

        private static final Map<String, UMILocation> MAP_NAME_TO_LOCATION =
                new HashMap<>(UMILocation.values().length);

        UMILocation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static UMILocation getByName(String locationName) {
            return MAP_NAME_TO_LOCATION.get(locationName);
        }

        static {
            for (UMILocation location : UMILocation.values()) {
                MAP_NAME_TO_LOCATION.put(location.getDisplayName(), location);
            }
        }
    }

    @Enumerated(EnumType.STRING)
    private UMILocation umiLocation;

    private Long umiLength;

    /** For JPA. */
    public UMIReagent() {
    }

    public UMIReagent(UMILocation umiLocation, Long umiLength) {
        super(null, null, null);
        this.umiLocation = umiLocation;
        this.umiLength = umiLength;
    }

    public UMILocation getUmiLocation() {
        return umiLocation;
    }

    public void setUmiLocation(
            UMILocation umiLocation) {
        this.umiLocation = umiLocation;
    }

    public Long getUmiLength() {
        return umiLength;
    }

    public void setUmiLength(Long umiLength) {
        this.umiLength = umiLength;
    }
}
