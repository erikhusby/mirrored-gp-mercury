package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Audited
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<IlluminaRunChamber> {
    public IlluminaFlowcell(String flowcellBarcode) {
        this(FlowcellType.getTypeForBarcode(flowcellBarcode),flowcellBarcode);
    }

    /**
     * Get information on which vessels are loaded for a given flowcell.
     *
     * @return Map of {@link VesselAndPosition} representing what is loaded onto the flowcell
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

    public enum CreateFct {
        YES,
        NO
    }

    /**
     * See the Google doc "Illumina Flowcell Suffix Guide".
     */
    public enum FlowcellType {
        MiSeqFlowcell("Flowcell1Lane", "MiSeq Flowcell", VesselGeometry.FLOWCELL1x1, "Illumina MiSeq", "^A\\w{4}$",
                "MiSeq", CreateFields.IssueType.MISEQ, LabBatch.LabBatchType.MISEQ, CreateFct.YES),
        HiSeqFlowcell("Flowcell8Lane", "HiSeq 2000 Flowcell", VesselGeometry.FLOWCELL1x8, "Illumina HiSeq 2000",
                "^\\w+(AB|AC)..$", "HiSeq", CreateFields.IssueType.HISEQ_2000, LabBatch.LabBatchType.FCT, CreateFct.YES),
        HiSeq2500Flowcell("Flowcell2Lane", "HiSeq 2500 Rapid Run Flowcell", VesselGeometry.FLOWCELL1x2, "Illumina HiSeq 2500",
                "^\\w+(AD|AM|BC)..$", "HiSeq", CreateFields.IssueType.HISEQ_2500_RAPID_RUN, LabBatch.LabBatchType.FCT, CreateFct.YES),
        HiSeq2500HighOutputFlowcell("Flowcell8Lane2500", "HiSeq 2500 High Output Flowcell", VesselGeometry.FLOWCELL1x8, "Illumina HiSeq 2500",
                "^\\w+AN..$", "HiSeq", CreateFields.IssueType.HISEQ_2500_HIGH_OUTPUT, LabBatch.LabBatchType.FCT, CreateFct.YES),
        HiSeq4000Flowcell("Flowcell8Lane4000", "HiSeq 4000 Flowcell", VesselGeometry.FLOWCELL1x8, "Illumina HiSeq 4000",
                "^\\w+BB..$", "HiSeq", CreateFields.IssueType.HISEQ_4000, LabBatch.LabBatchType.FCT, CreateFct.YES),
        HiSeqX10Flowcell("Flowcell8LaneX10", "HiSeq X 10 Flowcell", VesselGeometry.FLOWCELL1x8, "Illumina HiSeq X 10",
                "^\\w+(CC|AL)..$", "HiSeq", CreateFields.IssueType.HISEQ_X_10, LabBatch.LabBatchType.FCT, CreateFct.YES),
        NovaSeqFlowcell("Flowcell2LaneNova", "NovaSeq Flowcell", VesselGeometry.FLOWCELL1x2, "Illumina NovaSeq",
                "^\\w+DM..$", "NovaSeq", CreateFields.IssueType.NOVASEQ, LabBatch.LabBatchType.FCT, CreateFct.YES),
        NovaSeqS4Flowcell("Flowcell4LaneNova", "NovaSeq S4 Flowcell", VesselGeometry.FLOWCELL1x4, "Illumina NovaSeq",
                "^\\w+DS..$", "NovaSeq", CreateFields.IssueType.NOVASEQ_S4, LabBatch.LabBatchType.FCT, CreateFct.YES),
        NextSeqFlowcell("Flowcell4LaneNextSeq", "NextSeq Flowcell", VesselGeometry.FLOWCELL1x4, "Illumina NextSeq",
                "^\\w+BG..$", "NovaSeq", CreateFields.IssueType.NEXTSEQ, LabBatch.LabBatchType.FCT, CreateFct.YES),
        OtherFlowcell("FlowcellUnknown", "Unknown Flowcell", VesselGeometry.FLOWCELL1x2, "Unknown Model", ".*", null,
                null, null, CreateFct.NO);

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

        /**
         * In the lab, the barcodes of the different flowcell types have known format. If we don't know what type
         * of flowcell we are dealing with, we can infer it with by matching it's barcode with a regular expression.
         */
        private final Pattern flowcellTypeRegex;

        private final VesselGeometry vesselGeometry;

        /**
         * Used primarily when updating the FCT Jira ticket, this will indicate the sequencing station used when the
         * flowcell was sequenced.
         */
        private  String sequencingStationName;

        /**
         * When creating a FCT, different technologies use different JIRA issue types.
         */
        private final CreateFields.IssueType issueType;

        /**
         * The type of batch to use for this flowcell type.
         */
        private LabBatch.LabBatchType batchType;

        /**
         * Whether to display on Create FCT page.
         */
        private CreateFct createFct;

        /**
         * Creates a FlowcellType with an automation name, display name, and geometry.
         *  @param automationName    The name that will be supplied by automation scripts
         * @param displayName       The name that will be supplied by automation scripts
         * @param vesselGeometry    The vessel geometry
         * @param model             The sequencer model (think vendor/make/model).
         * @param flowcellTypeRegex The pattern to used to match the barcode to the flowcell type
         * @param sequencingStationName The sequencing station used when the flowcell was sequenced.
         * @param issueType         The Jira IssueType used when creating FCT tickets.
         * @param batchType         The type of batch to use for this flowcell type.
         * @param createFct         Whether to display on Create FCT page.
         */
        FlowcellType(String automationName, String displayName, VesselGeometry vesselGeometry, String model,
                String flowcellTypeRegex, String sequencingStationName, CreateFields.IssueType issueType,
                LabBatch.LabBatchType batchType, CreateFct createFct) {
            this.automationName = automationName;
            this.displayName = displayName;
            this.vesselGeometry = vesselGeometry;
            this.model = model;
            this.sequencingStationName = sequencingStationName;
            this.issueType = issueType;
            this.batchType = batchType;
            this.createFct = createFct;
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
         */
        public String getSequencerModel() {
            return model;
        }

        public String getSequencingStationName() {
            return sequencingStationName;
        }

        private static Map<String, FlowcellType> mapAutomationNameToType = new HashMap<>();
        private static Map<String, FlowcellType> mapDisplayNameToType = new HashMap<>();

        static {
            for (FlowcellType plateType : EnumSet.allOf(FlowcellType.class)) {
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

        public CreateFields.IssueType getIssueType() {
            return issueType;
        }

        /**
         * Try to figure out what kind of flowcell it is based on barcode.
         * See chart attached to GPLIM-4811 for more detail.
         *
         * @param barcode Barcode to test
         *
         * @return The FlowcellType.
         */
        public static FlowcellType getTypeForBarcode(@Nonnull String barcode) {
            FlowcellType flowcellType = getTypeForBarcodeStrict(barcode);
            if (flowcellType != null) {
                return flowcellType;
            } else if (FlowcellType.OtherFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return OtherFlowcell;
            } else {
                throw new RuntimeException("You seem to have found a FlowcellType that I don't know about.");
            }
        }

        public static FlowcellType getTypeForBarcodeStrict(@Nonnull String barcode) {
            // The order that these are evaluated is important which is why there are a
            // bunch of if-else's instead iterating over an enumSet or values().
            if (FlowcellType.MiSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return MiSeqFlowcell;
            } else if (FlowcellType.HiSeq2500Flowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeq2500Flowcell;
            } else if (FlowcellType.HiSeq2500HighOutputFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeq2500HighOutputFlowcell;
            } else if (FlowcellType.HiSeq4000Flowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeq4000Flowcell;
            } else if (FlowcellType.HiSeqX10Flowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeqX10Flowcell;
            } else if (FlowcellType.HiSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return HiSeqFlowcell;
            } else if (FlowcellType.NovaSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return NovaSeqFlowcell;
            } else if (FlowcellType.NovaSeqS4Flowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return NovaSeqS4Flowcell;
            } else if (FlowcellType.NextSeqFlowcell.getFlowcellTypeRegex().matcher(barcode).matches()) {
                return NextSeqFlowcell;
            }
            return null;
        }

        public static FlowcellType getTypeForPhysTypeAndBarcode(@Nonnull String physType, @Nonnull String barcode) {
            if (!physType.startsWith("Flowcell")) {
                return null;
            }
            FlowcellType flowcellType = getTypeForBarcodeStrict(barcode);
            if (flowcellType != null) {
                return flowcellType;
            }
            FlowcellType byAutomationName = getByAutomationName(physType);
            if (byAutomationName == null) {
                return HiSeqFlowcell;
            }
            return byAutomationName;
        }

        public static CreateFields.IssueType getIssueTypeByAutomationName(String automationName) {
            return mapAutomationNameToType.get(automationName).issueType;
        }

        public LabBatch.LabBatchType getBatchType() {
            return batchType;
        }

        public CreateFct getCreateFct() {
            return createFct;
        }
    }

    @Enumerated(EnumType.STRING)
    private FlowcellType flowcellType;

    // todo jmt how is this different from label?
    private String flowcellBarcode;

    @Embedded
    private VesselContainer<IlluminaRunChamber> vesselContainer = new VesselContainer<>(this);

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
    public VesselContainer<IlluminaRunChamber> getContainerRole() {
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
        return new HashSet<RunChamber>(vesselContainer.getContainedVessels());
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
                    TransferTraverserCriteria.TraversalDirection.Ancestors, 0);

            vesselsWithPositions.put(vesselPosition,criteria.getTube());
        }

        return vesselsWithPositions;
    }

    @Override
    public String getSequencerModel() {
        return getFlowcellType().getSequencerModel();
    }

    /** Should only be used by a data fixup test. */
    public void setFlowcellType(FlowcellType flowcellType) {
        this.flowcellType = flowcellType;
    }

    /** Should only be used by a data fixup test. */
    void setFlowcellBarcode(String flowcellBarcode) {
        this.flowcellBarcode = flowcellBarcode;
    }
}
