package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Entity
@Audited
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<RunChamber> {
    public enum FlowcellType {
        MiSeqFlowcell("MiSeq Flowcell", VesselGeometry.FLOWCELL1x1),
        HiSeqFlowcell("HiSeq 2000 Flowcell", VesselGeometry.FLOWCELL1x8),
        HiSeq2500Flowcell("HiSeq 2500 Flowcell", VesselGeometry.FLOWCELL1x2);

        private String displayName;
        private VesselGeometry vesselGeometry;

        FlowcellType(String displayName, VesselGeometry vesselGeometry) {
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
        }

        public String getDisplayName() {
            return displayName;
        }

        private static Map<String, FlowcellType> mapDisplayNameToType = new HashMap<String, FlowcellType>();

        static {
            for (FlowcellType plateType : FlowcellType.values()) {
                mapDisplayNameToType.put(plateType.getDisplayName(), plateType);
            }
        }

        public static FlowcellType getByDisplayName(String displayName) {
            FlowcellType plateTypeLocal = mapDisplayNameToType.get(displayName);
            if (plateTypeLocal == null) {
                throw new RuntimeException("Failed to find plate type " + displayName);
            }
            return plateTypeLocal;
        }

        public VesselGeometry getVesselGeometry() {
            return vesselGeometry;
        }
    }

    // todo jmt fix this
    @Transient
    private IlluminaRunConfiguration runConfiguration;

    @Enumerated(EnumType.STRING)
    private FlowcellType flowcellType;

    // todo jmt how is this different from label?
    private String flowcellBarcode;

    @Embedded
    VesselContainer<RunChamber> vesselContainer = new VesselContainer<RunChamber>(this);

    protected IlluminaFlowcell(String label, FlowcellType flowcellType) {
        super(label);
        this.flowcellBarcode = label;
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
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.FLOWCELL;
    }

    @Override
    public VesselContainer<RunChamber> getContainerRole() {
        return this.vesselContainer;
    }

    public IlluminaFlowcell(FlowcellType flowcellType, String flowcellBarcode, IlluminaRunConfiguration runConfig) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
        this.runConfiguration = runConfig;
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

    /**
     * In the illumina world, one sets the run configuration
     * when the flowcell is made.  But other technologies
     * might have their run configuration set later
     * in the process.
     *
     * @return
     */
    public IlluminaRunConfiguration getRunConfiguration() {
        return this.runConfiguration;
    }

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

}
