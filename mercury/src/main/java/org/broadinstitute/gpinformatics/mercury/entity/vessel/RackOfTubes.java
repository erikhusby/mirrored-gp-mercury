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

    /**
     * For JPA
     */
    protected RackOfTubes() {
    }

    public enum RackType implements VesselTypeGeometry {
        Abgene96SlotRack("Abgene96SlotRack", VesselGeometry.G12x8),
        CBSStraw_Box("CBSStraw_Box", VesselGeometry.G12x8),
        Conical15ml_10x5rack("Conical15ml_10x5rack", VesselGeometry.G4x10_NUM),
        Conical15ml_6x6box("Conical15ml_6x6box", VesselGeometry.G6x6_NUM),
        Conical15ml_6x6rack("Conical15ml_6x6rack", VesselGeometry.G6x6_NUM),
        Conical50ml_3x4rack("Conical50ml_3x4rack", VesselGeometry.G4x3_NUM),
        Conical50ml_4x4rack("Conical50ml_4x4rack", VesselGeometry.G4x4_NUM),
        Conical50ml_8x12_quad_rack("Conical50ml_8x12_quad_rack", VesselGeometry.G12x8),
        Conical50ml_8x3rack("Conical50ml_8x3rack", VesselGeometry.G3x8_NUM),
        Eppendorf10x10Box("Eppendorf10x10Box", VesselGeometry.G10x10_NUM),
        Eppendorf12x8Box("Eppendorf12x8Box", VesselGeometry.G12x8),
        FlipperRackRow24("FlipperRackRow24", VesselGeometry.G24x1, false),
        FourInch3x5Box("FourInch3x5Box", VesselGeometry.G5x3_NUM),
        FourInch7x7Box("FourInch7x7Box", VesselGeometry.G7x7_NUM),
        HamiltonSampleCarrier24("HamiltonSampleCarrier24", VesselGeometry.G24x1),
        HamiltonSampleCarrier32("HamiltonSampleCarrier32", VesselGeometry.G32x1),
        Matrix48SlotRack2mL("Matrix48SlotRack2mL", VesselGeometry.G12x8),
        Matrix96("Matrix96", VesselGeometry.G12x8),
        StripTubes("StripTubes", VesselGeometry.G12x8),
        Matrix96SlotRack040("Matrix96SlotRack040", VesselGeometry.G12x8),
        Matrix96SlotRack075("Matrix96SlotRack075", VesselGeometry.G12x8),
        Matrix96SlotRack14("Matrix96SlotRack14", VesselGeometry.G12x8),
        Matrix96SlotRackSC05("Matrix96SlotRackSC05", VesselGeometry.G12x8),
        Matrix96SlotRackSC14("Matrix96SlotRackSC14", VesselGeometry.G12x8),
        SlideBox_1x10("SlideBox_1x10", VesselGeometry.G1x10_NUM),
        SlideBox_1x25("SlideBox_1x25", VesselGeometry.G1x25_NUM),
        SlideBox_2x50("SlideBox_2x50", VesselGeometry.G2x50_NUM),
        TeFlow24("TeFlow24", VesselGeometry.TEFLOW3x8, false),
        ThreeInch9x9box("ThreeInch9x9box", VesselGeometry.G9x9_NUM),
        ThreeInch_FTA_Box("ThreeInch_FTA_Box", VesselGeometry.G1x100_NUM),
        TissueCassetteBox("TissueCassetteBox", VesselGeometry.G2x50_NUM),
        TissueCassetteBox_7x3("TissueCassetteBox_7x3", VesselGeometry.G3x7_NUM),
        TwoInch9x9box("TwoInch9x9box", VesselGeometry.G9x9_NUM),
        Vacutainer12x6Rack("Vacutainer12x6Rack", VesselGeometry.G12x6_NUM),
        Voucher_Box("Voucher_Box", VesselGeometry.G10x1_NUM);

        private static final Map<String, RackType> MAP_NAME_TO_RACK_TYPE =
                new HashMap<>(RackType.values().length);
        private final String         displayName;
        private final VesselGeometry vesselGeometry;
        private boolean barcoded = true;

        RackType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        RackType(String displayName, VesselGeometry vesselGeometry, boolean barcoded) {
            this(displayName, vesselGeometry);
            this.barcoded = barcoded;
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
    }

    @Enumerated(EnumType.STRING)
    private RackType rackType;

    public RackOfTubes(String label, RackType rackType) {
        super(label);
        this.rackType = rackType;
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
