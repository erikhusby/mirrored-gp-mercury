package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.Map;

@Entity
@Audited
/**
 * Represents a tube with a barcode.
 */
public class BarcodedTube extends LabVessel {

    public enum BarcodedTubeType {
        MatrixTube("Tube", "Matrix Tube"),
        OrageneTube("Oragene", "Oragene Tube"),
        VacutainerBloodTubeEDTA_4("VacutainerBloodTubeEDTA_4","Vacutainer Tube"),
        Cryovial2018("Cryovial2018", "Cryovial Tube");

        /**
         * The name that will be supplied by automation scripts.
         */
        private final String automationName;

        /**
         * The name to be displayed in UI.
         */
        private final String displayName;

        BarcodedTubeType(String automationName, String displayName) {
            this.automationName = automationName;
            this.displayName = displayName;
        }

        public String getAutomationName() {
            return automationName;
        }

        public String getDisplayName() {
            return displayName;
        }

        private static final Map<String, BarcodedTubeType> MAP_NAME_TO_TYPE =
                new HashMap<>(BarcodedTubeType.values().length);

        static {
            for (BarcodedTubeType barcodedTubeType : BarcodedTubeType.values()) {
                MAP_NAME_TO_TYPE.put(barcodedTubeType.automationName, barcodedTubeType);
            }
        }

        /**
         * Returns the tube type for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the BarcodedTubeType or null
         */
        public static BarcodedTubeType getByAutomationName(String automationName) {
            return MAP_NAME_TO_TYPE.get(automationName);
        }

    }

    @Enumerated(EnumType.STRING)
    private BarcodedTubeType tubeType;

    public BarcodedTube(String barcode, BarcodedTubeType tubeType) {
        super(barcode);
        if (barcode == null) {
            throw new IllegalArgumentException("Barcode must not be null when creating BarcodedTube.");
        }
        if (tubeType == null) {
            throw new IllegalArgumentException("BarcodedTubeType must not be null when creating BarcodedTube.");
        }
        this.tubeType = tubeType;
    }

    public BarcodedTube(String barcode) {
        this(barcode, BarcodedTubeType.MatrixTube);
    }

    public BarcodedTube(String barcode, String automationTubeType) {
        this(barcode, BarcodedTubeType.getByAutomationName(automationTubeType));
    }

    protected BarcodedTube() {
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.TUBE;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.TUBE;
    }

    public BarcodedTubeType getTubeType() {
        return tubeType;
    }

    public void setTubeType(BarcodedTubeType type) {
        tubeType = type;
    }
}
