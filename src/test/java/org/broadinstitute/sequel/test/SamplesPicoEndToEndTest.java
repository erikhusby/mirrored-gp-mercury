package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

import java.util.ArrayList;
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
            // pass in bean, get back list of tubes
        // validate workflow?
        // messaging
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= 8; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            // todo jmt is null projectPlan reasonable?
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(
                    new BSPStartingSample(bspStock + ".aliquot", null, null));
            mapBarcodeToTube.put(barcode,bspAliquot);
        }

        SamplesPicoMessageBuilder samplesPicoMessageBuilder = new SamplesPicoMessageBuilder(mapBarcodeToTube);
        samplesPicoMessageBuilder.buildEntities();
        // event web service, by batch
        // transfer visualizer?
        // datamart?
    }

    @SuppressWarnings("FeatureEnvy")
    public static class SamplesPicoMessageBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private PlateTransferEventType picoDilutionTransferJaxbA1;
        private PlateTransferEventType picoDilutionTransferJaxbA2;
        private PlateTransferEventType picoDilutionTransferJaxbB1;

        SamplesPicoMessageBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            String picoDilutionPlateBarcode = "PicoDilutionPlate";
            picoDilutionTransferJaxbA1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA1.getPlate().setSection(SBSSection.A1.getSectionName());

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
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateTransferEvent().add(picoStandardsTransferJaxb);
            messageList.add(bettaLIMSMessage4);
        }

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
