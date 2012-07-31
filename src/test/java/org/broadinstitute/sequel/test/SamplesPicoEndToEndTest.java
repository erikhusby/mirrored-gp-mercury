package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.sequel.boundary.vessel.LabBatchBean;
import org.broadinstitute.sequel.boundary.vessel.LabBatchResource;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test BSP Pico messaging
 */
public class SamplesPicoEndToEndTest {

    @Test
    public void testAll() {
        // import batch, workflow and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int rackPosition = 1; rackPosition <= 8; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, bspStock));
        }
        String batchId = "BP-1";
        BasicProjectPlan projectPlan = labBatchResource.buildProjectPlan(new LabBatchBean(batchId, "HybSel", tubeBeans),
                new HashMap<String, TwoDBarcodedTube>(), new HashMap<String, BSPStartingSample>());

        // validate workflow?
        // messaging
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (Starter starter : projectPlan.getStarters()) {
            LabVessel aliquotForStarter = projectPlan.getAliquotForStarter(starter);
            mapBarcodeToTube.put(aliquotForStarter.getLabel(), (TwoDBarcodedTube) aliquotForStarter);
        }

        SamplesPicoMessageBuilder samplesPicoMessageBuilder = new SamplesPicoMessageBuilder(mapBarcodeToTube, batchId);
        samplesPicoMessageBuilder.buildEntities();
        // event web service, by batch
        // transfer visualizer?
        // datamart?
    }

    /**
     * Build the messages used in Samples (BSP) Pico batches
     */
    @SuppressWarnings("FeatureEnvy")
    public static class SamplesPicoMessageBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final String batchId;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private PlateTransferEventType picoDilutionTransferJaxbA1;
        private PlateTransferEventType picoDilutionTransferJaxbA2;
        private PlateTransferEventType picoDilutionTransferJaxbB1;

        SamplesPicoMessageBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube, String batchId) {
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.batchId = batchId;
        }

        /**
         * Build JAXB messages.  These messages can be sent to BettalimsMessageResource, or used to build entity
         * graphs.
         */
        void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            String picoDilutionPlateBarcode = "PicoDilutionPlate";
            picoDilutionTransferJaxbA1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA1.getPlate().setSection(SBSSection.A1.getSectionName());
            picoDilutionTransferJaxbA1.setBatchId(batchId);

            picoDilutionTransferJaxbA2 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA2.getPlate().setSection(SBSSection.A2.getSectionName());

            picoDilutionTransferJaxbB1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbB1.getPlate().setSection(SBSSection.B1.getSectionName());

            BettaLIMSMessage bettaLIMSMessage1 = new BettaLIMSMessage();
            bettaLIMSMessage1.getPlateTransferEvent().add(picoDilutionTransferJaxbA1);
            bettaLIMSMessage1.getPlateTransferEvent().add(picoDilutionTransferJaxbA2);
            bettaLIMSMessage1.getPlateTransferEvent().add(picoDilutionTransferJaxbB1);
            messageList.add(bettaLIMSMessage1);

            PlateTransferEventType picoBufferTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoBufferTransfer", "PicoBufferPlate", picoDilutionPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(picoBufferTransferJaxb);
            messageList.add(bettaLIMSMessage2);

            String picoMicroflourPlateBarcode = "PicoMicroflourPlate";
            PlateTransferEventType picoMicroflourTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoMicroflourTransfer", picoDilutionPlateBarcode, picoMicroflourPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateTransferEvent().add(picoMicroflourTransferJaxb);
            messageList.add(bettaLIMSMessage3);

            PlateTransferEventType picoStandardsTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferJaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferJaxb.getPlate().setSection(SBSSection.P384_COL2_1INTERVAL_B.getSectionName());

            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(picoStandardsTransferJaxb);
            messageList.add(bettaLIMSMessage4);
        }

        /**
         * Build an entity graph for database free testing
         */
        void buildEntities() {
            buildJaxb();

            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setPersonDAO(new PersonDAO());

            LabEvent picoDilutionTransferEntityA1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbA1, mapBarcodeToTube, null);
            StaticPlate dilutionPlate = (StaticPlate) picoDilutionTransferEntityA1.getTargetLabVessels().iterator().next();
            LabEvent picoDilutionTransferEntityA2 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbA2, mapBarcodeToTube, dilutionPlate);
            LabEvent picoDilutionTransferEntityB1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbB1, mapBarcodeToTube, dilutionPlate);
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }
    }
}
