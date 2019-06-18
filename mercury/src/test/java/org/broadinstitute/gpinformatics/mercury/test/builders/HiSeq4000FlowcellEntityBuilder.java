package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graph for loading a HiSeq 4000
 */
public class HiSeq4000FlowcellEntityBuilder {


    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final TubeFormation denatureRack;
    private final String flowcellBarcode;
    private final String barcodeSuffix;
    private final LabBatch fctTicket;
    private final String designationName;
    private final int flowcellLanes;
    private final TubeFormation normRack;
    private final FCTCreationPoint fctCreationPoint;
    private StripTube stripTube;
    private IlluminaFlowcell illuminaFlowcell;

    public enum FCTCreationPoint {
        NORMALIZATION, DENATURE
    }

    public HiSeq4000FlowcellEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          TubeFormation denatureRack, String flowcellBarcode, String barcodeSuffix,
                                          LabBatch fctTicket, String designationName, int flowcellLanes,
                                          TubeFormation normRack, FCTCreationPoint fctCreationPoint) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.denatureRack = denatureRack;
        this.flowcellBarcode = flowcellBarcode;
        this.barcodeSuffix = barcodeSuffix;
        this.fctTicket = fctTicket;
        this.designationName = designationName;
        this.flowcellLanes = flowcellLanes;
        this.normRack = normRack;
        this.fctCreationPoint = fctCreationPoint;
    }

    public HiSeq4000FlowcellEntityBuilder invoke() {

        List<String> denatureTubeBarcodes = new ArrayList<>();
        Map<String, String> denatureToPosition = new HashMap<>();
        for (VesselPosition vesselPosition : denatureRack.getRackType().getVesselGeometry().getVesselPositions()) {
            BarcodedTube vesselAtPosition = denatureRack.getContainerRole().getVesselAtPosition(vesselPosition);
            if (vesselAtPosition != null) {
                denatureTubeBarcodes.add(vesselAtPosition.getLabel());
                denatureToPosition.put(vesselAtPosition.getLabel(), vesselPosition.name());
            }
        }

        Map<String, String> normToPosition = new HashMap<>();
        if (normRack != null) {
            for (VesselPosition vesselPosition : normRack.getRackType().getVesselGeometry().getVesselPositions()) {
                BarcodedTube vesselAtPosition = normRack.getContainerRole().getVesselAtPosition(vesselPosition);
                if (vesselAtPosition != null) {
                    normToPosition.put(vesselAtPosition.getLabel(), vesselPosition.name());
                }
            }
        }

        HiSeq4000JaxbBuilder hiSeq4000JaxbBuilder = new HiSeq4000JaxbBuilder(
                bettaLimsMessageTestFactory, barcodeSuffix, flowcellBarcode,
                denatureTubeBarcodes, denatureToPosition, denatureRack.getRacksOfTubes().iterator().next().getLabel(),
                fctTicket, denatureRack.getSampleInstanceCount(), designationName, flowcellLanes, normRack,
                normToPosition, fctCreationPoint).invoke();

        Map<String, BarcodedTube> mapBarcodeToVessel = new HashMap<>();

        LabEventTest.validateWorkflow("StripTubeBTransfer", denatureRack);
        for (BarcodedTube barcodedTube : denatureRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        PlateCherryPickEvent stripTubeTransferJaxb = hiSeq4000JaxbBuilder.getStripTubeTransferJaxb();
        final String stripTubeHolderBarcode = hiSeq4000JaxbBuilder.getStripTubeHolderBarcode();
        PlateTransferEventType stbFlowcellTransferJaxb = hiSeq4000JaxbBuilder.getStbFlowcellTransferJaxb();

        Map<String, StripTube> mapBarcodeToStripTube = new HashMap<>();
        int catchSampleInstanceCount = denatureRack.getSampleInstancesV2().size();
        LabEvent stripTubeTransferEntity =
                labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                        new HashMap<String, TubeFormation>() {{
                            put(denatureRack.getRacksOfTubes().iterator().next().getLabel(), denatureRack);
                        }},
                        mapBarcodeToVessel,
                        new HashMap<String, TubeFormation>() {{
                            put(stripTubeHolderBarcode, null);
                        }},
                        mapBarcodeToStripTube, new HashMap<String, RackOfTubes>()
                );
        labEventFactory.getEventHandlerSelector()
                .applyEventSpecificHandling(stripTubeTransferEntity, stripTubeTransferJaxb);
        labEventHandler.processEvent(stripTubeTransferEntity);
        // asserts
        stripTube = (StripTube) TestUtils.getFirst(stripTubeTransferEntity.getTargetLabVessels());
        Assert.assertNotNull(stripTube);

        // FlowcellTransfer
        LabEventTest.validateWorkflow("FlowcellTransfer", stripTube);
        LabEvent stbFlowcellTransferEntity =
                labEventFactory.buildFromBettaLimsPlateToPlateDbFree(stbFlowcellTransferJaxb,
                        stripTube, null);
        labEventFactory.getEventHandlerSelector()
                .applyEventSpecificHandling(stbFlowcellTransferEntity, stbFlowcellTransferJaxb);
        labEventHandler.processEvent(stbFlowcellTransferEntity);
        //asserts
        illuminaFlowcell =
                (IlluminaFlowcell) TestUtils.getFirst(stbFlowcellTransferEntity.getTargetLabVessels());

        return this;
    }

    public IlluminaFlowcell getIlluminaFlowcell() {
        return illuminaFlowcell;
    }
}
