package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.workrequest.kit.ReceptacleType;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A piece of plastic that holds tubes.  Can be reused to hold different sets of tubes.
 * Compare to TubeFormation.
 */
@Entity
@Audited
public class RackOfTubes extends LabVessel {
    private static final Log log = LogFactory.getLog(RackOfTubes.class);

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "racksOfTubes")
    private Set<TubeFormation> tubeFormations = new HashSet<>();

    public enum RackType implements VesselTypeGeometry {
        Abgene96SlotRack("Abgene96SlotRack", VesselGeometry.G12x8, CanRackScan.TRUE),
        CBSStraw_Box("CBSStraw_Box", VesselGeometry.G12x8, null),
        Conical15ml_10x5rack("Conical15ml_10x5rack", VesselGeometry.G4x10_NUM, null),
        Conical15ml_6x6box("Conical15ml_6x6box", VesselGeometry.G6x6_NUM, null),
        Conical15ml_6x6rack("Conical15ml_6x6rack", VesselGeometry.G6x6_NUM, null),
        Conical50ml_3x4rack("Conical50ml_3x4rack", VesselGeometry.G4x3_NUM, null),
        Conical50ml_4x4rack("Conical50ml_4x4rack", VesselGeometry.G4x4_NUM, null),
        Conical50ml_8x12_quad_rack("Conical50ml_8x12_quad_rack", VesselGeometry.G12x8, null),
        Conical50ml_8x3rack("Conical50ml_8x3rack", VesselGeometry.G3x8_NUM, null),
        Eppendorf10x10Box("Eppendorf10x10Box", VesselGeometry.G10x10_NUM, null),
        Eppendorf12x8Box("Eppendorf12x8Box", VesselGeometry.G12x8, null),
        Eppendorf12x8BoxWell("Eppendorf12x8BoxWell", VesselGeometry.G12x8, null),
        FlipperRackRow8("FlipperRackRow8", VesselGeometry.G8x1, false),
        FlipperRackRow24("FlipperRackRow24", VesselGeometry.G24x1, false),
        FluidX_4x6_Rack("FluidX_4x6_Rack", VesselGeometry.G6x4_ALPHANUM, true),
        FourInch3x5Box("FourInch3x5Box", VesselGeometry.G5x3_NUM, null),
        FourInch7x7Box("FourInch7x7Box", VesselGeometry.G7x7_NUM, null),
        QiasymphonyCarrier24("QiasymphonyCarrier24", VesselGeometry.G4x24, null),
        HamiltonSampleCarrier24("HamiltonSampleCarrier24", VesselGeometry.G24x1, null),
        HamiltonSampleCarrier32("HamiltonSampleCarrier32", VesselGeometry.G32x1, null),
        Matrix48SlotRack2mL("Matrix48SlotRack2mL", VesselGeometry.G12x8, null),
        Matrix96("Matrix96", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96Anonymous("Matrix96Anonymous", VesselGeometry.G12x8, false),
        StripTubeRackOf12("StripTubeRackOf12", VesselGeometry.G12x8, null),
        Matrix96SlotRack040("Matrix96SlotRack040", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRack075("Matrix96SlotRack075", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRack14("Matrix96SlotRack14", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRackSC05("Matrix96SlotRackSC05", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRackSC14("Matrix96SlotRackSC14", VesselGeometry.G12x8, CanRackScan.TRUE),
        SlideBox_1x10("SlideBox_1x10", VesselGeometry.G1x10_NUM, null),
        SlideBox_1x25("SlideBox_1x25", VesselGeometry.G1x25_NUM, null),
        SlideBox_2x50("SlideBox_2x50", VesselGeometry.G2x50_NUM, null),
        TeFlow24("TeFlow24", VesselGeometry.TEFLOW3x8, false),
        ThreeInch9x9box("ThreeInch9x9box", VesselGeometry.G9x9_NUM, null),
        ThreeInch_FTA_Box("ThreeInch_FTA_Box", VesselGeometry.G1x100_NUM, null),
        TissueCassetteBox("TissueCassetteBox", VesselGeometry.G2x50_NUM, null),
        TissueCassetteBox_7x3("TissueCassetteBox_7x3", VesselGeometry.G3x7_NUM, null),
        TwoInch9x9box("TwoInch9x9box", VesselGeometry.G9x9_NUM, null),
        Vacutainer12x6Rack("Vacutainer12x6Rack", VesselGeometry.G12x6_NUM, null),
        Voucher_Box("Voucher_Box", VesselGeometry.G10x1_NUM, null),
        FTAPaperHolder("FTAPaperHolder", VesselGeometry.G12x8, null);

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

        RackType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        RackType(String displayName, VesselGeometry vesselGeometry, boolean barcoded) {
            this(displayName, vesselGeometry, null);
            this.barcoded = barcoded;
        }

        RackType(String displayName, VesselGeometry vesselGeometry, CanRackScan canRackScan) {
            this(displayName, vesselGeometry);
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

        public List<BarcodedTube.BarcodedTubeType> getAllowedChildTypes() {
            // get the receptacle type
            ReceptacleType receptacleType = ReceptacleType.findByName(this.name());

            // for all the allowed tube types grab the applicable BarcodedTubeType objects
            List<BarcodedTube.BarcodedTubeType> typesAllowed = new ArrayList<>();
            for (ReceptacleType childReceptacleType : receptacleType.getChildReceptacleTypes()) {
                try {

                    BarcodedTube.BarcodedTubeType allowedType =
                            BarcodedTube.BarcodedTubeType.valueOf(childReceptacleType.getName());
                    typesAllowed.add(allowedType);
                } catch (IllegalArgumentException exception) {
                    log.error("Illegal Argument Exception when attempting to parse tube receptacle type of " + childReceptacleType.getName(), exception);
                }
            }
            return typesAllowed;
        }
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
