package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.hibernate.annotations.BatchSize;
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
import javax.persistence.OneToMany;
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
        Abgene96SlotRack("Abgene96SlotRack", "2D Abgene 96 Slot Rack [0.65 & 0.5ml]", VesselGeometry.G12x8, CanRackScan.TRUE),
        Box25x2("Box25x2", "Box (25x2)", VesselGeometry.G2x25_NUM),
        CBSStraw_Box("CBSStraw_Box", "CBS Straw box", VesselGeometry.G12x8),
        Conical15ml_10x4rack("Conical15ml_10x4rack", "15ml conical 10x4 rack", VesselGeometry.G4x10_NUM),
        Conical15ml_6x6box("Conical15ml_6x6box", "15ml conical 6x6 box", VesselGeometry.G6x6_NUM),
        Conical15ml_6x6rack("Conical15ml_6x6rack", "15ml conical 6x6 rack", VesselGeometry.G6x6_NUM),
        Conical50ml_3x4rack("Conical50ml_3x4rack", "50ml conical 3x4 rack", VesselGeometry.G4x3_NUM),
        Conical50ml_4x4rack("Conical50ml_4x4rack", "50ml conical 4x4 rack", VesselGeometry.G4x4_NUM),
        Conical50ml_8x12_quad_rack("Conical50ml_8x12_quad_rack", "50ml conical 8x12 quad rack", VesselGeometry.G12x8),
        Conical50ml_8x3rack("Conical50ml_8x3rack", "50ml conical 8x3 rack", VesselGeometry.G3x8_NUM),
        Eppendorf10x10Box("Eppendorf10x10Box", "Eppendorf 10x10 Box", VesselGeometry.G10x10_NUM),
        Eppendorf12x8Box("Eppendorf12x8Box", "Eppendorf 12x8 Box", VesselGeometry.G12x8),
        Eppendorf12x8BoxWell("Eppendorf12x8BoxWell", "Eppendorf 12x8 Box Well Format", VesselGeometry.G12x8),
        FTAPaperHolder("FTAPaperHolder", "FTAPaperHolder 8x12", VesselGeometry.G12x8),
        FiveInch6x6Box("FiveInch6x6Box", "5\" 6x6 box", VesselGeometry.G12x8),
        FlipperRackRow24("FlipperRackRow24", "Flipper Rack Row of 24", VesselGeometry.G24x1),
        FlipperRackRow8("FlipperRackRow8", "Flipper Rack Row of 8", VesselGeometry.G8x1),
        FluidX_4x6_Rack("FluidX_4x6_Rack", "FluidX 4x6 Rack", VesselGeometry.G6x4_ALPHANUM, true),
        FourInch3x5Box("FourInch3x5Box", "4\" 3x5 box", VesselGeometry.G5x3_NUM),
        FourInch7x7Box("FourInch7x7Box", "4\" 7x7 box", VesselGeometry.G7x7_NUM),
        HamiltonSampleCarrier24("HamiltonSampleCarrier24", "HamiltonSampleCarrier24", VesselGeometry.G24x1),
        HamiltonSampleCarrier32("HamiltonSampleCarrier32", "HamiltonSampleCarrier32", VesselGeometry.G32x1),
        Matrix48SlotRack2mL("Matrix48SlotRack2mL", "2D Matrix 48 Slot Rack [2mL]", VesselGeometry.G12x8),
        Matrix96("Matrix96", "2D Matrix 96 Slot Rack", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96Anonymous("Matrix96Anonymous", "2D Matrix 96 Slot Rack (anonymous)", VesselGeometry.G12x8),
        Matrix96SlotRack040("Matrix96SlotRack040", "2D Matrix 96 Slot Rack [0.40mL]", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRack075("Matrix96SlotRack075", "2D Matrix 96 Slot Rack [0.75ml]", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRack14("Matrix96SlotRack14", "2D Matrix 96 Slot Rack [1.4ml]", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRackSC05("Matrix96SlotRackSC05", "2D Matrix 96 Slot Rack [0.5ml SC]", VesselGeometry.G12x8, CanRackScan.TRUE),
        Matrix96SlotRackSC14("Matrix96SlotRackSC14", "2D Matrix 96 Slot Rack [1.0(1.4)ml SC]", VesselGeometry.G12x8, CanRackScan.TRUE),
        QiasymphonyCarrier24("QiasymphonyCarrier24", "QIAsymphony Carrier 24x4", VesselGeometry.G4x24),
        SlideBox_1x10("SlideBox_1x10", "Slide Box (1x10)", VesselGeometry.G1x10_NUM),
        SlideBox_1x25("SlideBox_1x25", "Slide Box (1x25)", VesselGeometry.G1x25_NUM),
        SlideBox_2x50("SlideBox_2x50", "Slide Box (2x50)", VesselGeometry.G2x50_NUM),
        SpinColumn96SlotRack("SpinColumn96SlotRack", "Spin Column 96 Slot Rack", VesselGeometry.G12x8),
        StripTubeRackOf12("StripTubeRackOf12", "Strip Tube Rack Of 12", VesselGeometry.G12x8),
        TeFlow24("TeFlow24", "Te Flow 24", VesselGeometry.TEFLOW3x8, false),
        ThreeInch9x9box("ThreeInch9x9box", "3\" 9x9 box", VesselGeometry.G9x9_NUM),
        ThreeInch_FTA_Box("ThreeInch_FTA_Box", "Three Inch FTA Box", VesselGeometry.G1x100_NUM),
        TissueCassetteBox("TissueCassetteBox", "Tissue Cassette Box", VesselGeometry.G2x50_NUM),
        TissueCassetteBox_7x3("TissueCassetteBox_7x3", "Tissue Cassette Box 7x3", VesselGeometry.G3x7_NUM),
        TwoInch9x9box("TwoInch9x9box", "2\" 9x9 box", VesselGeometry.G9x9_NUM),
        Vacutainer12x6Rack("Vacutainer12x6Rack", "Vacutainer 12x6 Rack", VesselGeometry.G6x12_NUM),
        Voucher_Box("Voucher_Box", "Voucher box", VesselGeometry.G10x1_NUM);

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
        private static final Map<String, RackType> MAP_DISPLAY_NAME_TO_TYPE =
                new HashMap<>(RackType.values().length);

        private final String         displayName;
        private final String         automationName;
        private final VesselGeometry vesselGeometry;
        private boolean barcoded = true;
        private CanRackScan canRackScan = CanRackScan.FALSE;

        RackType(String automationName, String displayName, VesselGeometry vesselGeometry) {
            this.automationName = automationName;
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        RackType(String automationName, String displayName, VesselGeometry vesselGeometry, boolean barcoded) {
            this(automationName, displayName, vesselGeometry);
            this.barcoded = barcoded;
        }

        RackType(String automationName, String displayName, VesselGeometry vesselGeometry, CanRackScan canRackScan) {
            this(automationName, displayName, vesselGeometry);
            this.canRackScan = canRackScan;
        }

        static {
            for (RackType rackType : RackType.values()) {
                // name and automation name
                MAP_NAME_TO_RACK_TYPE.put(rackType.name(), rackType);
                MAP_DISPLAY_NAME_TO_TYPE.put(rackType.getDisplayName(), rackType);
            }
        }

        public static RackType getByName(String automationName) {
            return MAP_NAME_TO_RACK_TYPE.get(automationName);
        }

        public static RackType getByDisplayName(String displayName) {
            RackType type = MAP_DISPLAY_NAME_TO_TYPE.get(displayName);
            if( type == null ) {
                type = getByName(displayName);
            }
            return type;
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
     * Reagent additions and machine loaded events, i.e. not transfers
     */
    @OneToMany(mappedBy = "ancillaryInPlaceVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<LabEvent> ancillaryInPlaceEvents = new HashSet<>();

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

    public Set<LabEvent> getAncillaryInPlaceEvents() {
        return ancillaryInPlaceEvents;
    }

    @Override
    public Set<LabEvent> getInPlaceLabEvents() {
        Set<LabEvent> allInPlaceEvents = new HashSet<>();
        allInPlaceEvents.addAll(super.getInPlaceLabEvents());
        allInPlaceEvents.addAll(ancillaryInPlaceEvents);
        return allInPlaceEvents;
    }
}
