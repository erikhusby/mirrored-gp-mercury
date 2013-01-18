package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.CONTAINER_TYPE.STATIC_PLATE;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalControl.ContinueTraversing;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalControl.StopTraversing;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Ancestors;

/**
 * A traditional plate.
 */
@Entity
@Audited
@Table(schema = "mercury")
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
        while (positionNames.hasNext()) {
            final String positionName = positionNames.next();
            final VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            final Set<LabVessel> tubes = new HashSet<LabVessel>();
            TransferTraverserCriteria criteria = new TransferTraverserCriteria() {
                @Override
                public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
                    if (OrmUtil.proxySafeIsInstance(labVessel, TwoDBarcodedTube.class)) {
                        tubes.add(labVessel);
                        return StopTraversing;
                    } else {
                        return ContinueTraversing;
                    }
                }

                @Override
                public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {}

                @Override
                public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {}
            };
            vesselContainer.evaluateCriteria(vesselPosition, criteria, Ancestors, null, 0);
            for (LabVessel tube : tubes) {
                vesselAndPositions.add(new VesselAndPosition(tube, vesselPosition));
            }
        }
        return vesselAndPositions;
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return plateType.getVesselGeometry();
    }

    public PlateType getPlateType() {
        return plateType;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.STATIC_PLATE;
    }

    public VesselContainer<PlateWell> getContainerRole() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<PlateWell> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }
}
