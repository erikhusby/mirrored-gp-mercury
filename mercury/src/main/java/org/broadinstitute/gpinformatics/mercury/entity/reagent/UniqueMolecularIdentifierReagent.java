package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.Map;

/**
 * A control that is not significant enough to be registered as a sample.  An example is the controls used to fill a
 * fingerprinting plate to 48 or 96 wells.
 */
@Entity
@Audited
public class UniqueMolecularIdentifierReagent extends Reagent {

    public enum UMILocation {
        INLINE_FIRST_READ("Inline First Read"),
        BEFORE_FIRST_INDEX_READ("Before First Index Read"),
        AFTER_FIRST_INDEX_READ("After First Index Read"),
        BEFORE_SECOND_INDEX_READ("Before Second Index Read"),
        AFTER_SECOND_INDEX_READ("After Second Index Read"),
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

    private int length;

    public UniqueMolecularIdentifierReagent(UMILocation umiLocation, int length) {
        super(null, null, null);
        this.umiLocation = umiLocation;
        this.length = length;
    }

    public UMILocation getUmiLocation() {
        return umiLocation;
    }

    public void setUmiLocation(
            UMILocation umiLocation) {
        this.umiLocation = umiLocation;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
