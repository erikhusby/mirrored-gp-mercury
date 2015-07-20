package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for Infinium messages.
 */
public class InfiniumJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String sourcePlate;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    private String ampPlate;
    private String hybridizationChip;
    private PlateTransferEventType infiniumAmplificationJaxb;
    private PlateEventType infiniumAmplificationReagentAdditionJaxb;
    private PlateEventType infiniumFragmentationJaxb;
    private PlateEventType infiniumPostFragmentationHybOvenLoadedJaxb;
    private PlateEventType infiniumPrecipitationJaxb;
    private PlateEventType infiniumPostPrecipitationHeatBlockLoadedJaxb;
    private PlateEventType infiniumPrecipitationIsopropanolAdditionJaxb;
    private PlateEventType infiniumResuspensionJaxb;
    private PlateEventType infiniumPostResuspensionHybOvenJaxb;
    private PlateCherryPickEvent infiniumHybridizationJaxb;
    private PlateEventType infiniumWashJaxb;
    private PlateEventType infiniumXStainJaxb;

    public InfiniumJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix, String sourcePlate) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourcePlate = sourcePlate;
    }

    public InfiniumJaxbBuilder invoke() {
        ampPlate = testPrefix + "AmplificationPlate";
        infiniumAmplificationJaxb =
                bettaLimsMessageTestFactory.buildPlateToPlate("InfiniumAmplification", sourcePlate, ampPlate);
        ReagentType reagentType = new ReagentType();
        reagentType.setKitType("NaOH");
        reagentType.setBarcode("1234");
        infiniumAmplificationJaxb.getReagent().add(reagentType);
        reagentType = new ReagentType();
        reagentType.setKitType("MA1");
        reagentType.setBarcode("1234");
        infiniumAmplificationJaxb.getReagent().add(reagentType);
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationJaxb);

        infiniumAmplificationReagentAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumAmplificationReagentAddition", ampPlate, Arrays.asList(
                        new BettaLimsMessageTestFactory.ReagentDto("MA2", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("MSM", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationReagentAdditionJaxb);

        infiniumFragmentationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumFragmentation", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("FMS", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumFragmentationJaxb);

        infiniumPostFragmentationHybOvenLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostFragmentationHybOvenLoaded", ampPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostFragmentationHybOvenLoadedJaxb);

        infiniumPrecipitationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPrecipitation", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("PM1", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationJaxb);

        infiniumPostPrecipitationHeatBlockLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostPrecipitationHeatBlockLoaded", ampPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostPrecipitationHeatBlockLoadedJaxb);

        infiniumPrecipitationIsopropanolAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumPrecipitationIsopropanolAddition", ampPlate, Collections.singletonList(
                        new BettaLimsMessageTestFactory.ReagentDto("Isopropanol", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationIsopropanolAdditionJaxb);

        infiniumResuspensionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumResuspension", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumResuspensionJaxb);

        infiniumPostResuspensionHybOvenJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostResuspensionHybOven", ampPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostResuspensionHybOvenJaxb);

        hybridizationChip = testPrefix + "HybridizationChip";

        //24 chip type
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        int chipRow = 1;
        int chipCol = 1;
        for(int col = 0; col < 3; col++) {
            for (int row = 0; row < 8; row++) {
                char rowChar = (char) (row + 'A');
                String sourceWell = rowChar + "0" + (col + 1); //Convert A01 to R01C01, D02 -> R12C01, E02 -> R01C02
                String chipWell = (chipRow < 10)
                        ? String.format("R0%dC0%d", chipRow, chipCol)
                        : String.format("R%dC0%d", chipRow, chipCol);
                cherryPicks.add(
                        new BettaLimsMessageTestFactory.CherryPick(ampPlate, sourceWell, hybridizationChip, chipWell));
                chipRow++;
                if(chipRow > 12) {
                    chipRow = 1;
                    chipCol++;
                }
            }
        }

        infiniumHybridizationJaxb = bettaLimsMessageTestFactory.buildPlateToPlateCherryPick("InfiniumHybridization",
                ampPlate, hybridizationChip, cherryPicks);
        infiniumHybridizationJaxb.getPlate().get(0).setPhysType("InfiniumChip24");
        infiniumHybridizationJaxb.getSourcePlate().get(0).setPhysType("DeepWell96");
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumHybridizationJaxb);

        infiniumWashJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumWash", hybridizationChip,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("PB1", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumWashJaxb);

        infiniumXStainJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumXStain", hybridizationChip,
                Arrays.asList(
                        new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("LX1", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("LX2", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("XC3", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("XC4", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("SML", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("ATM", "1234", null),
                        new BettaLimsMessageTestFactory.ReagentDto("EML", "1234", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumXStainJaxb);

        return this;
    }

    public PlateTransferEventType getInfiniumAmplificationJaxb() {
        return infiniumAmplificationJaxb;
    }

    public PlateEventType getInfiniumAmplificationReagentAdditionJaxb() {
        return infiniumAmplificationReagentAdditionJaxb;
    }

    public PlateEventType getInfiniumFragmentationJaxb() {
        return infiniumFragmentationJaxb;
    }

    public PlateEventType getInfiniumPostFragmentationHybOvenLoadedJaxb() {
        return infiniumPostFragmentationHybOvenLoadedJaxb;
    }

    public PlateEventType getInfiniumPrecipitationJaxb() {
        return infiniumPrecipitationJaxb;
    }

    public PlateEventType getInfiniumPostPrecipitationHeatBlockLoadedJaxb() {
        return infiniumPostPrecipitationHeatBlockLoadedJaxb;
    }

    public PlateEventType getInfiniumPrecipitationIsopropanolAdditionJaxb() {
        return infiniumPrecipitationIsopropanolAdditionJaxb;
    }

    public PlateEventType getInfiniumResuspensionJaxb() {
        return infiniumResuspensionJaxb;
    }

    public PlateEventType getInfiniumPostResuspensionHybOvenJaxb() {
        return infiniumPostResuspensionHybOvenJaxb;
    }

    public PlateCherryPickEvent getInfiniumHybridizationJaxb() {
        return infiniumHybridizationJaxb;
    }

    public PlateEventType getInfiniumWashJaxb() {
        return infiniumWashJaxb;
    }

    public PlateEventType getInfiniumXStainJaxb() {
        return infiniumXStainJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
