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

    public enum BarcodedTubeType implements VesselTypeGeometry {
        AbgeneTube96plugcap065("AbgeneTube96plugcap065", "2D Abgene Tube - 96 per rack format plug cap [0.65mL]"),
        AbgeneTube96screwcap05("AbgeneTube96screwcap05", "2D Abgene Tube - 96 per rack format screw cap [0.5mL]"),
        Aluminum_Pack("Aluminum_Pack", "Aluminum Pack"),
        CBSStraw_03("CBSStraw_03", "CBS Straw [0.3mL]"),
        Cell_Culture_Dish_100mm2("Cell_Culture_Dish_100mm2", "Cell Culture Dish 100mm2"),
        CentriCutieSC_5("CentriCutieSC_5", "CentriCutie Screw cap [5.0mL]"),
        Conical15("Conical15", "Conical [15mL]"),
        Conical50("Conical50", "Conical [50mL]"),
        Cryovial05("Cryovial05", "Cryo vial [0.5mL]"),
        Cryovial15("Cryovial15", "Cryo vial [1.5mL]"),
        Cryovial2018("Cryovial2018", "Cryo vial [2.0 (1.8)mL]"),
        Cryovial3028_Gates("Cryovial3028_Gates", "Cryo vial [3.0 (2.8)mL] (Gates)"),
        Cryovial45("Cryovial45", "Cryo vial [4.5mL]"),
        Cryovial5048("Cryovial5048", "Cryo vial [5.0 (4.8)mL]"),
        Cryovial010("Cryovial010", "Cryo vial [10.0 (9.8)mL]"),
        Cryovial_GSSR("Cryovial_GSSR", "Cryo vial [GSSR LABEL]"),
        EppendoffFliptop15("EppendoffFliptop15", "Eppendoff Flip-top [1.5mL]"),
        EppendorfFliptop15("EppendorfFliptop15", "Eppendoff Flip-top [1.5mL]"),
        EppendorfFliptop15_Gates("EppendorfFliptop15_Gates", "Eppendorf Flip-top [1.5mL] (Gates)"),
        EppendorfFliptop20("EppendorfFliptop20", "Eppendorf Flip-top [2.0mL]"),
        EppendorfScrewcap15("EppendorfScrewcap15", "Eppendorf Screw cap [1.5mL]"),
        EppendorfScrewcap20("EppendorfScrewcap20", "Eppendorf Screw cap [2.0mL]"),
        FTAPaper("FTAPaper", "FTA Paper"),
        FilterPaper("FilterPaper", "Filter Paper"),
        Flask("Flask", "Flask"),
        Flask_T25("Flask_T25", "Flask T25"),
        Flask_T75("Flask_T75", "Flask T75"),
        Flask_T150("Flask_T150", "Flask T150"),
        FluidX_10mL("FluidX_10mL", "FluidX [10mL]"),
        FluidX_6mL("FluidX_6mL", "FluidX [6mL]"),
        MatrixTube("Tube", "Matrix Tube [0.75mL]"),
        MatrixTubeSC05("MatrixTubeSC05", "Matrix Tube Screw cap [0.5mL]"),
        MatrixTubeSC14("MatrixTubeSC14", "Matrix Tube Screw cap [1.4mL]"),
        MatrixTube040("MatrixTube040", "Matrix Tube [0.40mL]"),
        MatrixTube075("MatrixTube075", "Matrix Tube [0.75mL]"),
        MatrixTube14("MatrixTube14", "Matrix Tube [1.4mL]"),
        MatrixTube2mL("MatrixTube2mL", "Matrix Tube [2.0mL]"),
        None("None", "None"),
        OCTTissueVessel("OCTTissueVessel", "OCT Tissue Vessel"),
        Oragene("Oragene", "Oragene Kit"),
        OrageneTube("Oragene", "Oragene Kit"),
        PaxgeneTissueContainer("PaxgeneTissueContainer", "Paxgene Tissue Container"),
        Petri("Petri", "Petri Dish"),
        Sarstedt_Tube_2mL("Sarstedt_Tube_2mL", "Sarstedt Tube [2mL]"),
        Slide("Slide", "Slide"),
        Slide_Gates("Slide_Gates", "Slide (Gates)"),
        Slide_Large_Gates("Slide_Large_Gates", "Slide Large (Gates)"),
        SpinColumn("SpinColumn", "Spin Column Tube"),
        StripTube("StripTube", "Strip Tube"),
        TissueCassette("TissueCassette", "Tissue Cassette"),
        Tissue_Culture_Plate_6("Tissue_Culture_Plate_6", "Tissue Culture Plate 6-well"),
        Tissue_Culture_Plate_12("Tissue_Culture_Plate_12", "Tissue Culture Plate 12-well"),
        Tissue_Culture_Plate_24("Tissue_Culture_Plate_24", "Tissue Culture Plate 24-well"),
        VacutainerBloodTube3("VacutainerBloodTube3", "Vacutainer Blood Tube [3mL]"),
        VacutainerBloodTube6("VacutainerBloodTube6", "Vacutainer Blood Tube [6mL]"),
        VacutainerBloodTube10("VacutainerBloodTube10", "Vacutainer Blood Tube [10mL]"),
        VacutainerBloodTubeBlueTigerTop8("VacutainerBloodTubeBlueTigerTop8", "Vacutainer Blood Tube Blue Tiger-Top [8mL]"),
        VacutainerBloodTubeEDTA_3("VacutainerBloodTubeEDTA_3", "Vacutainer EDTA Tube Purple-Top [3mL]"),
        VacutainerBloodTubeEDTA_4("VacutainerBloodTubeEDTA_4", "Vacutainer EDTA Tube Purple-Top [4mL]"),
        VacutainerBloodTubeEDTA_7("VacutainerBloodTubeEDTA_7", "Vacutainer EDTA Tube Purple-Top [7mL]"),
        VacutainerBloodTubeEDTA_10("VacutainerBloodTubeEDTA_10", "Vacutainer EDTA Tube Purple-Top [10mL]"),
        VacutainerBloodTubeGreenTigerTop8("VacutainerBloodTubeGreenTigerTop8", "Vacutainer Blood Tube Green Tiger-Top [8mL]"),
        VacutainerBloodTubeGreenTop10("VacutainerBloodTubeGreenTop10", "Vacutainer Blood Tube Green Top [10mL]"),
        VacutainerBloodTubePaxgene("VacutainerBloodTubePaxgene", "Vacutainer PAXGene Tube [10mL]"),
        VacutainerBloodTubeRedTigerTopSST10("VacutainerBloodTubeRedTigerTopSST10", "Vacutainer Blood Tube Red Tiger-Top SST [10mL]"),
        VacutainerBloodTubeRedTopClot10("VacutainerBloodTubeRedTopClot10", "Vacutainer Blood Tube Red-Top Clot [10mL]"),
        VacutainerBloodTubeYellowTop10("VacutainerBloodTubeYellowTop10", "Vacutainer Blood Tube Yellow Top [10mL]"),
        VacutainerCPTTube4("VacutainerCPTTube4", "Vacutainer CPT Tube [4mL]"),
        VacutainerCPTTube8("VacutainerCPTTube8", "Vacutainer CPT Tube [8mL]"),
        Voucher_Bag("Voucher_Bag", "Voucher Bag");

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

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public VesselGeometry getVesselGeometry() {
            return VesselGeometry.TUBE;
        }

        @Override
        public boolean isBarcoded() {
            return true;
        }

        private static final Map<String, BarcodedTubeType> MAP_NAME_TO_TYPE =
                new HashMap<>(BarcodedTubeType.values().length);
        private static final Map<String, BarcodedTubeType> MAP_DISPLAY_NAME_TO_TYPE =
                new HashMap<>(BarcodedTubeType.values().length);

        static {
            for (BarcodedTubeType barcodedTubeType : BarcodedTubeType.values()) {
                MAP_NAME_TO_TYPE.put(barcodedTubeType.automationName, barcodedTubeType);
                MAP_DISPLAY_NAME_TO_TYPE.put(barcodedTubeType.displayName, barcodedTubeType);
            }
        }

        /**
         * Returns the tube type for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the BarcodedTubeType or null
         */
        public static BarcodedTubeType getByAutomationName(String automationName) {
            if (MAP_NAME_TO_TYPE.containsKey(automationName)) {
                return MAP_NAME_TO_TYPE.get(automationName);
            } else {
                // If match failed try matching the display name.
                return MAP_DISPLAY_NAME_TO_TYPE.get(automationName);
            }
        }

        public static BarcodedTubeType getByDisplayName(String displayName) {
            return MAP_DISPLAY_NAME_TO_TYPE.get(displayName);
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
