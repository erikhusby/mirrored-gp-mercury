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
        Well40("Well40",     "Well [40uL]"),
        Well50("Well50",     "Well [50uL]"),
        Well150("Well150",   "Well [150uL]"),
        Well200("Well200",   "Well [200uL]"),
        Well500("Well500",   "Well [500uL]"),
        Well800("Well800",   "Well [800uL]"),
        Well1000("Well1000", "Well [1000uL]"),
        Well1200("Well1200", "Well [1200uL]"),
        Well2000("Well2000", "Well [2000uL]"),
        Well5000("Well5000", "Well [5000uL]"),
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
                // Make display name case insensitive, uL/ul in BSP all over the map
                MAP_DISPLAY_NAME_TO_TYPE.put(wellType.displayName.toUpperCase(), wellType);
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
            return MAP_DISPLAY_NAME_TO_TYPE.get(displayName.toUpperCase());
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLATE")
    private StaticPlate plate;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;
    
    public PlateWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        super(staticPlate.getLabel() +
                (staticPlate.getPlateType().name().startsWith("InfiniumChip") ? "_" : "") +
                vesselPosition);
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
