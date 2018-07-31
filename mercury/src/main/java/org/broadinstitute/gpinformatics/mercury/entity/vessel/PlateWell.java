package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.HashMap;
import java.util.Map;

@Entity
@Audited
public class PlateWell extends LabVessel {

    public enum WellType implements VesselTypeGeometry {
        Well2000("Well2000", "Well [2000uL]"),
        None("None", "None");

        /**
         * The name that will be supplied by automation scripts.
         */
        private final String automationName;

        /**
         * The name to be displayed in UI.
         */
        private final String displayName;

        WellType(String automationName, String displayName) {
            this.automationName = automationName;
            this.displayName = displayName;
        }



        public String getAutomationName() {
            return automationName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public VesselGeometry getVesselGeometry() {
            return VesselGeometry.WELL;
        }

        @Override
        public boolean isBarcoded() {
            return false;
        }

        private static final Map<String, WellType> MAP_NAME_TO_TYPE =
                new HashMap<>(WellType.values().length);
        private static final Map<String, WellType> MAP_DISPLAY_NAME_TO_TYPE =
                new HashMap<>(WellType.values().length);

        static {
            for (WellType wellType : WellType.values()) {
                MAP_NAME_TO_TYPE.put(wellType.automationName, wellType);
                MAP_DISPLAY_NAME_TO_TYPE.put(wellType.displayName, wellType);
            }
        }

        /**
         * Returns the well type for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the WellType or null
         */
        public static WellType getByAutomationName(String automationName) {
            if (MAP_NAME_TO_TYPE.containsKey(automationName)) {
                return MAP_NAME_TO_TYPE.get(automationName);
            } else {
                // If match failed try matching the display name.
                return MAP_DISPLAY_NAME_TO_TYPE.get(automationName);
            }
        }

        public static WellType getByDisplayName(String displayName) {
            return MAP_DISPLAY_NAME_TO_TYPE.get(displayName);
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLATE")
    private StaticPlate plate;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;
    
    public PlateWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        super(staticPlate.getLabel() + vesselPosition);
        this.plate = staticPlate;
        this.vesselPosition = vesselPosition;
    }

    public PlateWell() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.WELL;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.PLATE_WELL;
    }

    public StaticPlate getPlate() {
        return plate;
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }
}
