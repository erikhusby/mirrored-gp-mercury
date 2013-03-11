package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.ContainerType.STATIC_PLATE;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalControl.ContinueTraversing;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalControl.StopTraversing;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * A traditional plate.
 */
@Entity
@Audited
public class StaticPlate extends LabVessel implements VesselContainerEmbedder<PlateWell>, Serializable {

    public enum PlateType {
        Eppendorf96("Eppendorf96", VesselGeometry.G12x8),
        Matrix96("Matrix96", VesselGeometry.G12x8),
        CovarisRack("CovarisRack", VesselGeometry.G12x8),
        IndexedAdapterPlate96("IndexedAdapterPlate96", VesselGeometry.G12x8),
        SageCassette("SageCassette", VesselGeometry.SAGE_CASSETTE),
        Fluidigm48_48AccessArrayIFC("Fluidigm48.48AccessArrayIFC", VesselGeometry.FLUIDIGM_48_48),
        FilterPlate96("FilterPlate96", VesselGeometry.G12x8),
        Eppendorf384("Eppendorf384", VesselGeometry.G24x16),
        NinetySixDeepWell("96DeepWell", VesselGeometry.G12x8);
        // todo jmt Eco48

        private String displayName;
        private VesselGeometry vesselGeometry;

        PlateType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        public String getDisplayName() {
            return displayName;
        }

        private static Map<String, PlateType> mapDisplayNameToType = new HashMap<String, PlateType>();
        static {
            for (PlateType plateType : PlateType.values()) {
                mapDisplayNameToType.put(plateType.getDisplayName(), plateType);
            }
        }

        public static PlateType getByDisplayName(String displayName) {
            PlateType plateTypeLocal = mapDisplayNameToType.get(displayName);
            if(plateTypeLocal == null) {
                throw new RuntimeException("Failed to find plate type " + displayName);
            }
            return plateTypeLocal;
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }


    @Embedded
    private VesselContainer<PlateWell> vesselContainer = new VesselContainer<PlateWell>(this);

    @Enumerated(EnumType.STRING)
    private PlateType plateType;

    public StaticPlate(String label, PlateType plateType) {
        super(label);
        this.plateType = plateType;
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
        List<StaticPlate> parents = new ArrayList<StaticPlate>();
        for (LabEvent event : getTransfersTo()) {
            for (LabVessel source : event.getSourceLabVessels()) {
                if (source.getType() == STATIC_PLATE) {
                    parents.add(OrmUtil.proxySafeCast(source, StaticPlate.class));
                }
            }
        }
        return parents;
    }

    public static class HasRackContentByWellCriteria implements TransferTraverserCriteria {

        private Map<VesselPosition, Boolean> result = new HashMap<VesselPosition, Boolean>();

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
            if (!result.containsKey(context.getVesselPosition())) {
                result.put(context.getVesselPosition(), false);
            }
            if (context.getLabVessel() != null) {
                if (OrmUtil.proxySafeIsInstance(context.getVesselContainer().getEmbedder(), TubeFormation.class)) {
                    result.put(context.getVesselPosition(), true);
                    return TraversalControl.StopTraversing;
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {}

        @Override
        public void evaluateVesselPostOrder(Context context) {}

        public Map<VesselPosition, Boolean> getResult() {
            return result;
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
        applyCriteriaToAllWells(criteria);
        return criteria.getResult();
    }

    public static class NearestTubeAncestorsCriteria implements TransferTraverserCriteria {

        private Set<LabVessel> tubes = new HashSet<LabVessel>();
        private Set<VesselAndPosition> vesselAndPositions = new LinkedHashSet<VesselAndPosition>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), TwoDBarcodedTube.class)) {
                tubes.add(context.getLabVessel());
                vesselAndPositions.add(new VesselAndPosition(context.getLabVessel(), context.getVesselPosition()));
                return StopTraversing;
            } else {
                return ContinueTraversing;
            }
        }

        @Override
        public void evaluateVesselInOrder(Context context) {}

        @Override
        public void evaluateVesselPostOrder(Context context) {}

        public Set<LabVessel> getTubes() {
            return tubes;
        }

        public Set<VesselAndPosition> getVesselAndPositions() {
            return vesselAndPositions;
        }
    }

    /**
     * Returns a list of the most immediate tube ancestors for each well. The "distance" from this plate across upstream
     * plate transfers is not relevant; all upstream branches are traversed until either a tube is found or the branch
     * ends.
     *
     * @return all nearest tube ancestors
     */
    public List<VesselAndPosition> getNearestTubeAncestors() {
        final List<VesselAndPosition> vesselAndPositions = new ArrayList<VesselAndPosition>();
        Iterator<String> positionNames = plateType.getVesselGeometry().getPositionNames();
/*
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            NearestTubeAncestorsCriteria criteria = new NearestTubeAncestorsCriteria();
            vesselContainer.evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
            for (LabVessel tube : criteria.getTubes()) {
                vesselAndPositions.add(new VesselAndPosition(tube, vesselPosition));
            }
        }
        return vesselAndPositions;
*/
        NearestTubeAncestorsCriteria criteria = new NearestTubeAncestorsCriteria();
        applyCriteriaToAllWells(criteria);
        return new ArrayList<VesselAndPosition>(criteria.getVesselAndPositions());
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
        Set<SectionTransfer> sectionTransfers = new LinkedHashSet<SectionTransfer>();
        recursePlateTransfers(sectionTransfers, vesselContainer, depth, 1);
        return new ArrayList<SectionTransfer>(sectionTransfers);
    }

    private void recursePlateTransfers(Set<SectionTransfer> transfers, VesselContainer vesselContainer, int depth, int currentDepth) {
        Set<SectionTransfer> sectionTransfersTo = vesselContainer.getSectionTransfersTo();
        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            recurseSectionTransfer(transfers, depth, currentDepth, sectionTransfer);
        }
    }

    private void recurseSectionTransfer(Set<SectionTransfer> transfers, int depth, int currentDepth, SectionTransfer sectionTransfer) {
        transfers.add(sectionTransfer);
        if (currentDepth < depth) {
            recursePlateTransfers(transfers, sectionTransfer.getSourceVesselContainer(), depth, currentDepth + 1);
        }
    }

    private void applyCriteriaToAllWells(TransferTraverserCriteria criteria) {
        Iterator<String> positionNames = plateType.getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            vesselContainer.evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
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
