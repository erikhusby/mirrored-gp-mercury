package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A piece of plastic that holds tubes.  Can be reused to hold different sets of tubes.
 * Compare to TubeFormation.
 */
@Entity
@Audited
public class RackOfTubes extends LabVessel {

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "racksOfTubes")
    private Set<TubeFormation> tubeFormations = new HashSet<>();

    public enum RackType implements VesselTypeGeometry {
        Abgene96SlotRack("Abgene96SlotRack", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        CBSStraw_Box("CBSStraw_Box", VesselGeometry.G12x8, null, null),
        Conical15ml_10x5rack("Conical15ml_10x5rack", VesselGeometry.G4x10_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical15}),
        Conical15ml_6x6box("Conical15ml_6x6box", VesselGeometry.G6x6_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical15}),
        Conical15ml_6x6rack("Conical15ml_6x6rack", VesselGeometry.G6x6_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical15}),
        Conical50ml_3x4rack("Conical50ml_3x4rack", VesselGeometry.G4x3_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical50}),
        Conical50ml_4x4rack("Conical50ml_4x4rack", VesselGeometry.G4x4_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical50}),
        Conical50ml_8x12_quad_rack("Conical50ml_8x12_quad_rack", VesselGeometry.G12x8, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical50}),
        Conical50ml_8x3rack("Conical50ml_8x3rack", VesselGeometry.G3x8_NUM, null,
                new BarcodedTube.BarcodedTubeType[]{BarcodedTube.BarcodedTubeType.Conical50}),
        Eppendorf10x10Box("Eppendorf10x10Box", VesselGeometry.G10x10_NUM, null, null),
        Eppendorf12x8Box("Eppendorf12x8Box", VesselGeometry.G12x8, null, null),
        Eppendorf12x8BoxWell("Eppendorf12x8BoxWell", VesselGeometry.G12x8, null, null),
        FlipperRackRow8("FlipperRackRow8", VesselGeometry.G8x1, false),
        FlipperRackRow24("FlipperRackRow24", VesselGeometry.G24x1, false),
        FluidX_4x6_Rack("FluidX_4x6_Rack", VesselGeometry.G6x4_ALPHANUM, true),
        FourInch3x5Box("FourInch3x5Box", VesselGeometry.G5x3_NUM, null, null),
        FourInch7x7Box("FourInch7x7Box", VesselGeometry.G7x7_NUM, null, null),
        QiasymphonyCarrier24("QiasymphonyCarrier24", VesselGeometry.G4x24, null, null),
        HamiltonSampleCarrier24("HamiltonSampleCarrier24", VesselGeometry.G24x1, null, null),
        HamiltonSampleCarrier32("HamiltonSampleCarrier32", VesselGeometry.G32x1, null, null),
        Matrix48SlotRack2mL("Matrix48SlotRack2mL", VesselGeometry.G12x8, null, null),
        Matrix96("Matrix96", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        Matrix96Anonymous("Matrix96Anonymous", VesselGeometry.G12x8, false),
        StripTubeRackOf12("StripTubeRackOf12", VesselGeometry.G12x8, null, null),
        Matrix96SlotRack040("Matrix96SlotRack040", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        Matrix96SlotRack075("Matrix96SlotRack075", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        Matrix96SlotRack14("Matrix96SlotRack14", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        Matrix96SlotRackSC05("Matrix96SlotRackSC05", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        Matrix96SlotRackSC14("Matrix96SlotRackSC14", VesselGeometry.G12x8, CanRackScan.TRUE, null),
        SlideBox_1x10("SlideBox_1x10", VesselGeometry.G1x10_NUM, null, null),
        SlideBox_1x25("SlideBox_1x25", VesselGeometry.G1x25_NUM, null, null),
        SlideBox_2x50("SlideBox_2x50", VesselGeometry.G2x50_NUM, null, null),
        TeFlow24("TeFlow24", VesselGeometry.TEFLOW3x8, false),
        ThreeInch9x9box("ThreeInch9x9box", VesselGeometry.G9x9_NUM, null, null),
        ThreeInch_FTA_Box("ThreeInch_FTA_Box", VesselGeometry.G1x100_NUM, null, null),
        TissueCassetteBox("TissueCassetteBox", VesselGeometry.G2x50_NUM, null, null),
        TissueCassetteBox_7x3("TissueCassetteBox_7x3", VesselGeometry.G3x7_NUM, null, null),
        TwoInch9x9box("TwoInch9x9box", VesselGeometry.G9x9_NUM, null, null),
        Vacutainer12x6Rack("Vacutainer12x6Rack", VesselGeometry.G12x6_NUM, null, null),
        Voucher_Box("Voucher_Box", VesselGeometry.G10x1_NUM, null, null),
        FTAPaperHolder("FTAPaperHolder", VesselGeometry.G12x8, null, null);

        public enum CanRackScan {
            TRUE(true),
            FALSE(false);
            private final boolean value;

            CanRackScan(boolean value) {
                this.value = value;
            }

            public boolean booleanValue() {
                return value;
            }
        }

        private static final Map<String, RackType> MAP_NAME_TO_RACK_TYPE =
                new HashMap<>(RackType.values().length);
        private final String         displayName;
        private final VesselGeometry vesselGeometry;
        private boolean barcoded = true;
        private CanRackScan canRackScan;
        private BarcodedTube.BarcodedTubeType[] allowedBarcodedTubeTypes;

        RackType(String displayName, VesselGeometry vesselGeometry, BarcodedTube.BarcodedTubeType[] allowedTubeTypes) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
            this.allowedBarcodedTubeTypes = allowedTubeTypes;
        }

        RackType(String displayName, VesselGeometry vesselGeometry, boolean barcoded) {
            this(displayName, vesselGeometry, null, null);
            this.barcoded = barcoded;
        }

        RackType(String displayName, VesselGeometry vesselGeometry, CanRackScan canRackScan,
                 BarcodedTube.BarcodedTubeType[] allowedTubeTypes) {
            this(displayName, vesselGeometry, allowedTubeTypes);
            this.canRackScan = canRackScan;
        }

        static {
            for (RackType rackType : RackType.values()) {
                MAP_NAME_TO_RACK_TYPE.put(rackType.name(), rackType);
            }
        }

        public static RackType getByName(String automationName) {
            return MAP_NAME_TO_RACK_TYPE.get(automationName);
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }

        @Override
        public boolean isBarcoded() {
            return barcoded;
        }

        public boolean isRackScannable() {
            return canRackScan == CanRackScan.TRUE;
        }

        public BarcodedTube.BarcodedTubeType[] getAllowedBarcodedTubeTypes() { return allowedBarcodedTubeTypes; }
    }

    @Enumerated(EnumType.STRING)
    private RackType rackType;

    /**
     * For JPA
     */
    protected RackOfTubes() {
    }

    public RackOfTubes(String label, RackType rackType) {
        super(label);
        this.rackType = rackType;
    }

    public RackOfTubes(String manufacturerBarcode, RackType rackType, String plateName) {
        this(manufacturerBarcode, rackType);
        this.name = plateName;
    }

    public RackType getRackType() {
        return rackType;
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return rackType.getVesselGeometry();
    }

    @Override
    public ContainerType getType() {
        return ContainerType.RACK_OF_TUBES;
    }

    public Set<TubeFormation> getTubeFormations() {
        return tubeFormations;
    }
}
