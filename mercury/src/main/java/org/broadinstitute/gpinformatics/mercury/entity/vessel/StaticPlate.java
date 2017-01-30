package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.ContainerType.STATIC_PLATE;

/**
 * A traditional plate.
 */
@Entity
@Audited
public class StaticPlate extends LabVessel implements VesselContainerEmbedder<PlateWell>, Serializable {

    /**
     * This isolates the flow cell and strip tube manual types used for manual transfers,
     * and prevents getByAutomationName in PlateType from returning anything for Flowcell.
     */
    public enum ManualTransferFlowCellType implements VesselTypeGeometry {
        FlowCell8("Flowcell", VesselGeometry.FLOWCELL1x8),
        StripTube1x1("StripTube", VesselGeometry.STRIP_TUBE);

        /**
         * The name that will be supplied by automation scripts.
         */
        private String automationName;

        private VesselGeometry vesselGeometry;

        /**
         * Creates a PlateType.
         *
         * @param automationName    the name that will be supplied by automation scripts
         * @param vesselGeometry    the vessel geometry
         */
        ManualTransferFlowCellType(String automationName, VesselGeometry vesselGeometry) {
            this.automationName = automationName;
            this.vesselGeometry = vesselGeometry;
        }

        /**
         * Returns the name that will be supplied by automation scripts.
         */
        public String getAutomationName() {
            return automationName;
        }

        private static Map<String, PlateType> mapAutomationNameToType = new HashMap<>();

        static {
            for (PlateType plateType : PlateType.values()) {
                mapAutomationNameToType.put(plateType.getAutomationName(), plateType);
            }
        }

        /**
         * Returns the PlateType for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the PlateType or null
         */
        public static PlateType getByAutomationName(String automationName) {
            return mapAutomationNameToType.get(automationName);
        }


        @Override
        public String getDisplayName() {
            return automationName;
        }

        @Override
        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }

        @Override
        public boolean isBarcoded() {
            return true;
        }

    }

    public enum PlateType implements VesselTypeGeometry {
        CovarisRack("CovarisRack", VesselGeometry.G12x8),
        Eco48("Eco48", VesselGeometry.G8x6),
        Eppendorf96("Eppendorf96", VesselGeometry.G12x8),
        Eppendorf384("Eppendorf384", VesselGeometry.G24x16),
        FilterPlate96("FilterPlate96", VesselGeometry.G12x8),
        Fluidigm48_48AccessArrayIFC("Fluidigm48.48AccessArrayIFC", VesselGeometry.FLUIDIGM_48_48),
        IndexedAdapterPlate96("IndexedAdapterPlate96", VesselGeometry.G12x8),
        IndexedAdapterPlate384("IndexedAdapterPlate384", VesselGeometry.G24x16),
        Matrix96("Matrix96", VesselGeometry.G12x8),
        MiSeqReagentKit("MiseqReagentKit", VesselGeometry.MISEQ_REAGENT_KIT),
        MicrofluorPlate96Well("MicrofluorPlate96Well", VesselGeometry.G12x8),
        NinetySixDeepWell("96DeepWell", VesselGeometry.G12x8),
        Plate384WellBlack50("Plate384WellBlack50", VesselGeometry.G24x16),
        Plate384WellClear50("Plate384WellClear50", VesselGeometry.G24x16),
        Plate384WellEppendorfTwintec40("Plate384WellEppendorfTwintec40", VesselGeometry.G24x16),
        Plate4x6Well5000("Plate4x6Well5000", VesselGeometry.G4x6_NUM),
        Plate96Microtube1200("Plate96Microtube1200", VesselGeometry.G12x8),
        Plate96Microtube2000("Plate96Microtube2000", VesselGeometry.G12x8),
        Plate96RNEasyWell1000("Plate96RNEasyWell1000", VesselGeometry.G12x8),
        Plate96RoundWellBlock2000("Plate96RoundWellBlock2000", VesselGeometry.G12x8),
        Plate96SchredderSpinColumn1000("Plate96SchredderSpinColumn1000", VesselGeometry.G12x8),
        Plate96Well200("Plate96Well200", VesselGeometry.G12x8),
        Plate96Well200PCR("Plate96Well200PCR", VesselGeometry.G12x8),
        Plate96Well200PCR_Expression("Plate96Well200PCR_Expression", VesselGeometry.G12x8),
        Plate96Well800("Plate96Well800", VesselGeometry.G12x8),
        Plate96Well1200("Plate96Well1200", VesselGeometry.G12x8),
        Plate96WellCollectionTube2000("Plate96WellCollectionTube2000", VesselGeometry.G12x8),
        Plate96WellRNA("Plate96WellRNA", VesselGeometry.G12x8),
        SageCassette("SageCassette", VesselGeometry.SAGE_CASSETTE),
        SpinColumn96SlotRack("SpinColumn96SlotRack", VesselGeometry.G12x8),
        InfiniumChip24("InfiniumChip24", VesselGeometry.INFINIUM_24_CHIP),
        InfiniumChip12("InfiniumChip12", VesselGeometry.INFINIUM_12_CHIP),
        InfiniumChip8("InfiniumChip8", VesselGeometry.INFINIUM_8_CHIP),
        TenXChip("10XChip", VesselGeometry.TEN_X_CHIP);

        /**
         * The name that will be supplied by automation scripts.
         */
        private String automationName;

        private VesselGeometry vesselGeometry;

        /**
         * Creates a PlateType.
         *
         * @param automationName    the name that will be supplied by automation scripts
         * @param vesselGeometry    the vessel geometry
         */
        PlateType(String automationName, VesselGeometry vesselGeometry) {
            this.automationName = automationName;
            this.vesselGeometry = vesselGeometry;
        }

        /**
         * Returns the name that will be supplied by automation scripts.
         */
        public String getAutomationName() {
            return automationName;
        }

        private static Map<String, PlateType> mapAutomationNameToType = new HashMap<>();

        static {
            for (PlateType plateType : PlateType.values()) {
                mapAutomationNameToType.put(plateType.getAutomationName(), plateType);
            }
        }

        /**
         * Returns the PlateType for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the PlateType or null
         */
        public static PlateType getByAutomationName(String automationName) {
            return mapAutomationNameToType.get(automationName);
        }


        @Override
        public String getDisplayName() {
            return automationName;
        }

        @Override
        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }

        @Override
        public boolean isBarcoded() {
            return true;
        }

    }

   public String getAutomationName()
   {
       return plateType.automationName;
   }

    @Embedded
    private VesselContainer<PlateWell> vesselContainer = new VesselContainer<>(this);

    @Enumerated(EnumType.STRING)
    private PlateType plateType;

    public StaticPlate(String label, PlateType plateType) {
        super(label);
        this.plateType = plateType;
    }

    public StaticPlate(String manufacturerBarcode, PlateType plateType, String plateName) {
        this(manufacturerBarcode, plateType);
        this.name = plateName;
    }

    /** For Hibernate */
    protected StaticPlate() {
    }

    /**
     * Returns a list of plates (in no specific order) that had material directly transferred into this plate.
     *
     * Note, some sense of order might be preserved if LabVessel.getTransfersTo() used a LinkedHashSet to gather the events.
     *
     * @return immediate plate parents
     */
    public List<StaticPlate> getImmediatePlateParents() {
        List<StaticPlate> parents = new ArrayList<>();
        for (LabEvent event : getTransfersTo()) {
            for (LabVessel source : event.getSourceLabVessels()) {
                if (source.getType() == STATIC_PLATE) {
                    parents.add(OrmUtil.proxySafeCast(source, StaticPlate.class));
                }
            }
        }
        return parents;
    }

    public static class HasRackContentByWellCriteria extends TransferTraverserCriteria {

        private Map<VesselPosition, Boolean> result = new HashMap<>();
        private Map<VesselPosition, Pair<TubeFormation, VesselPosition>> wellPositionToFormationAndTubePosition =
                new HashMap<>();

        /**
         * The current position in the query plate for which we are trying to determine sample containment.  This
         * is recorded as a member variable of this TransferTraverserCriteria implementation to be visible across
         * recursions.  The current VesselPosition in the Context may not be the same as the original query
         * VesselPosition.
         */
        private VesselPosition queryVesselPosition;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            /*
             * TraversalControl is somewhat advisory in that the criteria callbacks will still be called for other
             * transfers with the same hopCount even after one of them returns StopTraversing. Therefore, we have to
             * guard against overwriting a previously written true value. There is a test,
             * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlateTest#testGetHasRackContentByWellWithParallelPlateTransfer()},
             * that attempts to verify this. However, since the events are processed in a non-deterministic order, it's
             * possible for that test to pass even with this check. Note that this may also make code coverage waver
             * ever so slightly based on whether or not this expression evaluates to true for a particular test run.
             */
            // Record the VesselPosition at hop count one as the query position.  The position at which we find the
            // upstream tube (or empty slot in the rack) may be different, but the "sample containment" flag should
            // be set for the query vessel position.

            Pair<LabVessel,VesselPosition> vesselPositionPair = context.getContextVesselAndPosition();
            LabVessel contextVessel = vesselPositionPair.getLeft();
            VesselPosition contextVesselPosition = vesselPositionPair.getRight();
            VesselContainer contextVesselContainer = context.getContextVesselContainer();

            if ( context.getHopCount() == 0 ) {
                if( contextVesselPosition != null ) {
                    queryVesselPosition = contextVesselPosition;
                    if (!result.containsKey(queryVesselPosition)) {
                        result.put(queryVesselPosition, false);
                        // When starting from a new well, may need to revisit an upstream plate well
                        // that was previously traversed such as when Rack to Epp96 to Epp384.
                        resetAllTraversed();
                    }
                }
            } else if( contextVessel != null && contextVesselContainer != null ) {
                if (OrmUtil.proxySafeIsInstance(contextVesselContainer.getEmbedder(), TubeFormation.class)) {
                    result.put(contextVesselPosition, true);
                    wellPositionToFormationAndTubePosition.put(queryVesselPosition, Pair.of(
                            OrmUtil.proxySafeCast(contextVesselContainer.getEmbedder(), TubeFormation.class),
                            contextVesselPosition));
                    return TraversalControl.StopTraversing;
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {}

        public Map<VesselPosition, Boolean> getResult() {
            return result;
        }

        public Map<VesselPosition, Pair<TubeFormation, VesselPosition>> getWellPositionToFormationAndTubePosition() {
            return wellPositionToFormationAndTubePosition;
        }
    }

    /**
     * Returns, for each well position, whether or not there has been a source tube in a tube rack somewhere in the
     * ancestry.
     *
     * @return
     */
    public Map<VesselPosition, Boolean> getHasRackContentByWell() {
        HasRackContentByWellCriteria criteria = new HasRackContentByWellCriteria();
        vesselContainer.applyCriteriaToAllPositions(criteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return criteria.getResult();
    }

    /**
     * Finds nearest ancestor rack and tube position for each well.
     * @return Map of well position to tube formation and tube's position.
     */
    public Map<VesselPosition, Pair<TubeFormation, VesselPosition>> nearestFormationAndTubePositionByWell() {
        HasRackContentByWellCriteria criteria = new HasRackContentByWellCriteria();
        vesselContainer.applyCriteriaToAllPositions(criteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return criteria.getWellPositionToFormationAndTubePosition();
    }

    /**
     * Returns a list of the most immediate tube ancestors for each well. The "distance" from this plate across upstream
     * plate transfers is not relevant; all upstream branches are traversed until either a tube is found or the branch
     * ends.
     *
     * @return all nearest tube ancestors
     */
    public List<VesselAndPosition> getNearestTubeAncestors() {

        /**
         * to share the implementation with other potential container embedders, this code has been moved to Vessel
         * Container
         */
        return vesselContainer.getNearestTubeAncestors();
    }

    /**
     * Traverses section transfer ancestors, returning section transfers up to the specified depth.
     *
     * Does not use a TransferTraverserCriteria because the goal is to look at the plate as a whole instead of focusing
     * on individual wells.
     *
     * @param depth
     * @return
     */
    public List<SectionTransfer> getUpstreamPlateTransfers(int depth) {
        Set<SectionTransfer> sectionTransfers = new LinkedHashSet<>();
        collectPlateTransfers(sectionTransfers, vesselContainer, depth, 1);
        return new ArrayList<>(sectionTransfers);
    }

    private void collectPlateTransfers(Set<SectionTransfer> transfers, VesselContainer vesselContainer, int depth,
                                       int currentDepth) {
        Set<SectionTransfer> sectionTransfersTo = vesselContainer.getSectionTransfersTo();
        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            transfers.add(sectionTransfer);
            if (currentDepth < depth) {
                collectPlateTransfers(transfers, sectionTransfer.getSourceVesselContainer(), depth, currentDepth + 1);
            }
        }
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return plateType.getVesselGeometry();
    }


    public PlateType getPlateType() {
        return plateType;
    }

    @Override
    public ContainerType getType() {
        return ContainerType.STATIC_PLATE;
    }


    public VesselContainer<PlateWell> getContainerRole() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<PlateWell> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }
}
