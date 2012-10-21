package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test messaging for BSP Dried Blood Spot Extraction
 */
public class DriedBloodSpotTest {

    @Test
    public void testX() {
        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder = new DriedBloodSpotJaxbBuilder();
        driedBloodSpotJaxbBuilder.buildJaxb();
    }

    public static class DriedBloodSpotJaxbBuilder{
        private List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

        public void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            // DBSSamplePunch receptacle -> plate A1
            String incubationPlateBarcode = "DBSIncPlate";
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            ReceptaclePlateTransferEvent samplePunchJaxb1 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-1", incubationPlateBarcode, "96DeepWell", "A01", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb1);
            bettaLimsMessageFactory.advanceTime();

            // DBSSamplePunch receptacle -> plate A2
            ReceptaclePlateTransferEvent samplePunchJaxb2 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-2", incubationPlateBarcode, "96DeepWell", "A02", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb2);
            bettaLimsMessageFactory.advanceTime();

            // DBSSamplePunch receptacle -> plate A3 etc.
            ReceptaclePlateTransferEvent samplePunchJaxb3 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-3", incubationPlateBarcode, "96DeepWell", "A03", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb3);
            bettaLimsMessageFactory.advanceTime();
            messageList.add(bettaLIMSMessage);

            // DBSIncubationMix plateEvent
            PlateEventType incubationMixJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSIncubationMix", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(incubationMixJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            // DBSLysisBuffer plateEvent
            PlateEventType lysisBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSLysisBuffer", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(lysisBufferJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            // DBSMagneticResin plateEVent
            PlateEventType magneticResinJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSMagneticResin", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateEvent().add(magneticResinJaxb);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            String firstPurificationBarcode = "DBS1stPur";
            // DBS1stPurification plate -> plate
            PlateTransferEventType dbs1stPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS1stPurification", incubationPlateBarcode, firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateEvent().add(dbs1stPurificationJaxb);
            messageList.add(bettaLIMSMessage5);
            bettaLimsMessageFactory.advanceTime();

            // DBSWashBuffer plateEvent
            PlateEventType dbsWashBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSWashBuffer", firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(dbsWashBufferJaxb);
            messageList.add(bettaLIMSMessage6);
            bettaLimsMessageFactory.advanceTime();

            String secondPurificationBarcode = "DBS2ndPur";
            // DBS2ndPurification plate -> plate
            PlateTransferEventType dbs2ndPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS2ndPurification", firstPurificationBarcode, secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateEvent().add(dbs2ndPurificationJaxb);
            messageList.add(bettaLIMSMessage7);
            bettaLimsMessageFactory.advanceTime();

            // DBSElutionBuffer plateEvent
            PlateEventType dbsElutionBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSElutionBuffer", secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateEvent().add(dbsElutionBufferJaxb);
            messageList.add(bettaLIMSMessage8);
            bettaLimsMessageFactory.advanceTime();

            // DBSFinalTransfer plate -> rack
            List<String> finalTubeBarcodes = new ArrayList<String>();
            for(int i = 0; i < 3; i++) {
                finalTubeBarcodes.add("DBSFinal" + i);
            }
            PlateTransferEventType plateTransferEventTypeJaxb = bettaLimsMessageFactory.buildPlateToRack("DBSFinalTransfer", secondPurificationBarcode, "DBSFinal", finalTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage9 = new BettaLIMSMessage();
            bettaLIMSMessage9.getPlateEvent().add(plateTransferEventTypeJaxb);
            messageList.add(bettaLIMSMessage9);
            bettaLimsMessageFactory.advanceTime();

        }
    }
}
