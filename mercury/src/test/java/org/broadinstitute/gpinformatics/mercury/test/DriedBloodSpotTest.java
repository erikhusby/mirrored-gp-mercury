package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
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
        private ReceptaclePlateTransferEvent samplePunchJaxb1;
        private ReceptaclePlateTransferEvent samplePunchJaxb2;
        private ReceptaclePlateTransferEvent samplePunchJaxb3;
        private PlateEventType incubationMixJaxb;
        private PlateEventType lysisBufferJaxb;
        private PlateEventType magneticResinJaxb;
        private PlateTransferEventType dbs1stPurificationJaxb;
        private PlateEventType dbsWashBufferJaxb;
        private PlateTransferEventType dbs2ndPurificationJaxb;
        private PlateEventType dbsElutionBufferJaxb;
        private PlateTransferEventType dbsFinalTransferJaxb;

        public void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            // DBSSamplePunch receptacle -> plate A1
            String incubationPlateBarcode = "DBSIncPlate";
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            samplePunchJaxb1 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-1", incubationPlateBarcode, "96DeepWell", "A01", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb1);
            bettaLimsMessageFactory.advanceTime();

            // DBSSamplePunch receptacle -> plate A2
            samplePunchJaxb2 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-2", incubationPlateBarcode, "96DeepWell", "A02", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb2);
            bettaLimsMessageFactory.advanceTime();

            // DBSSamplePunch receptacle -> plate A3 etc.
            samplePunchJaxb3 = bettaLimsMessageFactory.buildTubeToPlate(
                    "DBSSamplePunch", "SM-3", incubationPlateBarcode, "96DeepWell", "A03", "FTAPaper");
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb3);
            bettaLimsMessageFactory.advanceTime();
            messageList.add(bettaLIMSMessage);

            // DBSIncubationMix plateEvent
            incubationMixJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSIncubationMix", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(incubationMixJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            // DBSLysisBuffer plateEvent
            lysisBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSLysisBuffer", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(lysisBufferJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            // DBSMagneticResin plateEVent
            magneticResinJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSMagneticResin", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateEvent().add(magneticResinJaxb);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            String firstPurificationBarcode = "DBS1stPur";
            // DBS1stPurification plate -> plate
            dbs1stPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS1stPurification", incubationPlateBarcode, firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateEvent().add(dbs1stPurificationJaxb);
            messageList.add(bettaLIMSMessage5);
            bettaLimsMessageFactory.advanceTime();

            // DBSWashBuffer plateEvent
            dbsWashBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSWashBuffer", firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(dbsWashBufferJaxb);
            messageList.add(bettaLIMSMessage6);
            bettaLimsMessageFactory.advanceTime();

            String secondPurificationBarcode = "DBS2ndPur";
            // DBS2ndPurification plate -> plate
            dbs2ndPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS2ndPurification", firstPurificationBarcode, secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateEvent().add(dbs2ndPurificationJaxb);
            messageList.add(bettaLIMSMessage7);
            bettaLimsMessageFactory.advanceTime();

            // DBSElutionBuffer plateEvent
            dbsElutionBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSElutionBuffer", secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateEvent().add(dbsElutionBufferJaxb);
            messageList.add(bettaLIMSMessage8);
            bettaLimsMessageFactory.advanceTime();

            // DBSFinalTransfer plate -> rack
            List<String> finalTubeBarcodes = new ArrayList<String>();
            for(int i = 0; i < 3; i++) {
                finalTubeBarcodes.add("DBSFinal" + i);
            }
            dbsFinalTransferJaxb = bettaLimsMessageFactory.buildPlateToRack("DBSFinalTransfer", secondPurificationBarcode, "DBSFinal", finalTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage9 = new BettaLIMSMessage();
            bettaLIMSMessage9.getPlateEvent().add(dbsFinalTransferJaxb);
            messageList.add(bettaLIMSMessage9);
            bettaLimsMessageFactory.advanceTime();
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public ReceptaclePlateTransferEvent getSamplePunchJaxb1() {
            return samplePunchJaxb1;
        }

        public ReceptaclePlateTransferEvent getSamplePunchJaxb2() {
            return samplePunchJaxb2;
        }

        public ReceptaclePlateTransferEvent getSamplePunchJaxb3() {
            return samplePunchJaxb3;
        }

        public PlateEventType getIncubationMixJaxb() {
            return incubationMixJaxb;
        }

        public PlateEventType getLysisBufferJaxb() {
            return lysisBufferJaxb;
        }

        public PlateEventType getMagneticResinJaxb() {
            return magneticResinJaxb;
        }

        public PlateTransferEventType getDbs1stPurificationJaxb() {
            return dbs1stPurificationJaxb;
        }

        public PlateEventType getDbsWashBufferJaxb() {
            return dbsWashBufferJaxb;
        }

        public PlateTransferEventType getDbs2ndPurificationJaxb() {
            return dbs2ndPurificationJaxb;
        }

        public PlateEventType getDbsElutionBufferJaxb() {
            return dbsElutionBufferJaxb;
        }

        public PlateTransferEventType getDbsFinalTransferJaxb() {
            return dbsFinalTransferJaxb;
        }
    }

    public static class DriedBloodSpotEntityBuilder {
        private DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder;

        public DriedBloodSpotEntityBuilder(DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder) {
            this.driedBloodSpotJaxbBuilder = driedBloodSpotJaxbBuilder;
        }

        public void buildEntities() {
            driedBloodSpotJaxbBuilder.buildJaxb();
            LabEventFactory labEventFactory = new LabEventFactory();
//            labEventFactory.buildFromBettaLimsPlateToPlateDbFree(driedBloodSpotJaxbBuilder.getSamplePunchJaxb1());
        }
    }
}
