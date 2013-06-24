package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Entity
@Audited
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<RunChamber> {
    public enum FlowcellType {
        MiSeqFlowcell("Flowcell1Lane", "MiSeq Flowcell", VesselGeometry.FLOWCELL1x1),
        HiSeqFlowcell("Flowcell8Lane", "HiSeq 2000 Flowcell", VesselGeometry.FLOWCELL1x8),
        HiSeq2500Flowcell("Flowcell2Lane", "HiSeq 2500 Flowcell", VesselGeometry.FLOWCELL1x2);

        /**
         * The name that will be supplied by automation scripts.
         */
        private String automationName;

        /**
         * The name to be displayed in UI.
         */
        private String displayName;

        private VesselGeometry vesselGeometry;

        /**
         * Creates a FlowcellType with an automation name, display name, and geometry.
         *
         * @param automationName    the name that will be supplied by automation scripts
         * @param displayName       the name that will be supplied by automation scripts
         * @param vesselGeometry    the vessel geometry
         */
        FlowcellType(String automationName, String displayName, VesselGeometry vesselGeometry) {
            this.automationName = automationName;
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        /**
         * Returns the name that will be supplied by automation scripts.
         */
        public String getAutomationName() {
            return automationName;
        }

        /**
         * Returns the name to be displayed in UI.
         */
        public String getDisplayName() {
            return displayName;
        }

        private static Map<String, FlowcellType> mapAutomationNameToType = new HashMap<String, FlowcellType>();
        private static Map<String, FlowcellType> mapDisplayNameToType = new HashMap<String, FlowcellType>();

        static {
            for (FlowcellType plateType : FlowcellType.values()) {
                mapAutomationNameToType.put(plateType.getAutomationName(), plateType);
                mapDisplayNameToType.put(plateType.getDisplayName(), plateType);
            }
        }

        /**
         * Returns the FlowcellType for the given automation name or null if none is found.
         *
         * @param automationName    the name supplied by automation scripts
         * @return the FlowcellType or null
         */
        public static FlowcellType getByAutomationName(String automationName) {
            return mapAutomationNameToType.get(automationName);
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }

    @Enumerated(EnumType.STRING)
    private FlowcellType flowcellType;

    // todo jmt how is this different from label?
    private String flowcellBarcode;

    @Embedded
    VesselContainer<RunChamber> vesselContainer = new VesselContainer<>(this);

    protected IlluminaFlowcell(String label, FlowcellType flowcellType) {
        super(label);
        this.flowcellBarcode = label;
        this.flowcellType = flowcellType;
    }

    public IlluminaFlowcell() {
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        return vesselContainer.getTransfersFrom();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return vesselContainer.getTransfersTo();
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return flowcellType.getVesselGeometry();
    }

    @Override
    public ContainerType getType() {
        return ContainerType.FLOWCELL;
    }

    @Override
    public VesselContainer<RunChamber> getContainerRole() {
        return this.vesselContainer;
    }

    public IlluminaFlowcell(FlowcellType flowcellType, String flowcellBarcode) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
        this.flowcellType = flowcellType;
    }

/*
    todo jmt need something similar in VesselContainer
    public void addChamber(LabVessel library,int laneNumber) {
        if (flowcellType == FLOWCELL_TYPE.EIGHT_LANE) {
            if (laneNumber < 1 || laneNumber > 8) {
                throw new RuntimeException("Lane numbers are 1-8");
            }
        }
        else if (flowcellType == FLOWCELL_TYPE.MISEQ) {
            if (laneNumber != 1) {
                throw new RuntimeException("Miseq flowcells only have a single lane");
            }
        }
        IlluminaRunChamber runChamber = new IlluminaRunChamber(this,laneNumber,library);
        runChambers.add(runChamber);
    }
*/

    @Override
    public Iterable<RunChamber> getChambers() {
        return this.vesselContainer.getContainedVessels();
    }

    @Override
    public String getCartridgeName() {
        return this.flowcellBarcode;
    }

    @Override
    public String getCartridgeBarcode() {
        return this.flowcellBarcode;
    }

    public FlowcellType getFlowcellType() {
        return flowcellType;
    }

    /**
     * Returns a list of the most immediate tube ancestors for each Flowcell Lane. The "distance" from this flowcell
     * across upstream transfers is not relevant; all upstream branches are traversed until either a tube is found or
     * the branch ends.
     *
     * @return all nearest tube ancestors and the lane to which they are ancestors.
     */
    public Map<VesselPosition, LabVessel> getNearestTubeAncestorsForLanes() {

        Map<VesselPosition, LabVessel> vesselsWithPositions = new HashMap<>();

        Iterator<String> positionNames = getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);

            TransferTraverserCriteria.NearestTubeAncestorCriteria
                    criteria = new TransferTraverserCriteria.NearestTubeAncestorCriteria();

            vesselContainer.evaluateCriteria(vesselPosition, criteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, null, 0);

            vesselsWithPositions.put(vesselPosition,criteria.getTube());
        }

        return vesselsWithPositions;
    }
}
