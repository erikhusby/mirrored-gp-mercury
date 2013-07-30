package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Audited
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<RunChamber> {
    public IlluminaFlowcell(String flowcellBarcode) {
        this(FlowcellType.getTypeForBarcode(flowcellBarcode),flowcellBarcode);
    }

    /**
     * get information on what vessels loaded given flowcell.
     *
     * @return Map of VesselAndPosition representing what is loaded onto the flowcell
     */
    @DaoFree
    public Set<VesselAndPosition> getLoadingVessels() {
        Set<VesselAndPosition> loadedVesselsAndPositions = new HashSet<>();
        for (Map.Entry<VesselPosition, LabVessel> vesselPositionEntry : getNearestTubeAncestorsForLanes().entrySet()) {
            if (vesselPositionEntry.getValue() != null) {
                loadedVesselsAndPositions.add(new VesselAndPosition(vesselPositionEntry.getValue(),vesselPositionEntry.getKey()));
            }
        }
        return loadedVesselsAndPositions;
    }

    public enum FlowcellType {
        MiSeqFlowcell("Flowcell1Lane", "MiSeq Flowcell", VesselGeometry.FLOWCELL1x1,"Illumina MiSeq", "^A{1}\\w{4}$",
                "MiSeq"),
        HiSeqFlowcell("Flowcell8Lane", "HiSeq 2000 Flowcell", VesselGeometry.FLOWCELL1x8,"Illumina HiSeq 2000", "^\\w{9}$",
                "HiSeq"),
        HiSeq2500Flowcell("Flowcell2Lane", "HiSeq 2500 Flowcell", VesselGeometry.FLOWCELL1x2,"Illumina HiSeq 2500", "^\\w+ADXX$",
                "HiSeq"),
        OtherFlowcell("FlowcellUnknown", "Unknown Flowcell", VesselGeometry.FLOWCELL1x2,"Unknown Model", ".*",
                null);

        /**
         * The sequencer model (think vendor/make/model)
         */
        private final String model;

        /**
         * The name that will be supplied by automation scripts.
         */
        private final String automationName;

        /**
         * The name to be displayed in UI.
         */
        private final String displayName;

        private final Pattern flowcellTypeRegex;

        private final VesselGeometry vesselGeometry;

        private  String sequencingStationName;

        /**
         * Creates a FlowcellType with an automation name, display name, and geometry.
         *
         * @param automationName    the name that will be supplied by automation scripts
         * @param displayName       the name that will be supplied by automation scripts
         * @param vesselGeometry    the vessel geometry
         * @param sequencingStationName
         */
        FlowcellType(String automationName, String displayName, VesselGeometry vesselGeometry, String model,
                     String flowcellTypeRegex, String sequencingStationName) {
            this.automationName = automationName;
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
            this.model = model;
            this.sequencingStationName = sequencingStationName;
            this.flowcellTypeRegex = Pattern.compile(flowcellTypeRegex);
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

        /**
         * Returns the model of the sequencer (think vendor/make/model)
         * @return
         */
        public String getSequencerModel() {
            return model;
        }

        private static Map<String, FlowcellType> mapAutomationNameToType = new HashMap<>();
        private static Map<String, FlowcellType> mapDisplayNameToType = new HashMap<>();

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

        public Pattern getFlowcellTypeRegex() {
            return flowcellTypeRegex;
        }

        /**
         * Try to figure out what kind of flowcell it is based on barcode:
         * 2500's end in ADXX
         * MiSeqs are A plus 4 digits/chars
         * 2000's are (mostly) any other 9 char/digit FC name.
         *
         * @param barcode Barcode to test
         *
         * @return The FlowcellType, if found or fall back to HiSeqFlowcell
         */
        public static FlowcellType getTypeForBarcode(@Nonnull String barcode) {
            if (FlowcellType.MiSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return MiSeqFlowcell;
            } else if (FlowcellType.HiSeq2500Flowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeq2500Flowcell;
            } else if (FlowcellType.HiSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeqFlowcell;
            } else if (FlowcellType.OtherFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return OtherFlowcell;
            } else {
                throw new RuntimeException("You seem to have found a FlowcellType that I don't know about.");
            }
        }
    }

    @Enumerated(EnumType.STRING)
    private FlowcellType flowcellType;

    // todo jmt how is this different from label?
    private String flowcellBarcode;

    @Embedded
    VesselContainer<RunChamber> vesselContainer = new VesselContainer<>(this);

    public IlluminaFlowcell(FlowcellType flowcellType, String flowcellBarcode) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
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
    @Override
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

    @Override
    public String getSequencerModel() {
        return getFlowcellType().getSequencerModel();
    }
}
