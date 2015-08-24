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
    private List<String> hybridizationChips = new ArrayList<>();
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
    private PlateEventType infiniumPostHybridizationHybOvenLoadedJaxb;
    private PlateEventType infiniumHybChamberLoadedJaxb;
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
        reagentType.setBarcode("1234-NaOH");
        infiniumAmplificationJaxb.getReagent().add(reagentType);
        reagentType = new ReagentType();
        reagentType.setKitType("MA1");
        reagentType.setBarcode("1234-MA1");
        infiniumAmplificationJaxb.getReagent().add(reagentType);
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationJaxb);

        infiniumAmplificationReagentAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumAmplificationReagentAddition", ampPlate, Arrays.asList(
                        new BettaLimsMessageTestFactory.ReagentDto("MA2", "1234-MA2", null),
                        new BettaLimsMessageTestFactory.ReagentDto("MSM", "1234-MSM", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationReagentAdditionJaxb);

        infiniumFragmentationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumFragmentation", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("FMS", "1234-FMS", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumFragmentationJaxb);

        infiniumPostFragmentationHybOvenLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostFragmentationHybOvenLoaded", ampPlate);
        infiniumPostFragmentationHybOvenLoadedJaxb.setStation("Hyb Oven #1");
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostFragmentationHybOvenLoadedJaxb);

        infiniumPrecipitationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPrecipitation", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("PM1", "1234-PM1", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationJaxb);

        infiniumPostPrecipitationHeatBlockLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostPrecipitationHeatBlockLoaded", ampPlate);
        infiniumPostPrecipitationHeatBlockLoadedJaxb.setStation("Heat Block #4");
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostPrecipitationHeatBlockLoadedJaxb);

        infiniumPrecipitationIsopropanolAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumPrecipitationIsopropanolAddition", ampPlate, Collections.singletonList(
                        new BettaLimsMessageTestFactory.ReagentDto("Isopropanol", "2345", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationIsopropanolAdditionJaxb);

        infiniumResuspensionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumResuspension", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234-RA1", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumResuspensionJaxb);

        infiniumPostResuspensionHybOvenJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostResuspensionHybOven", ampPlate);
        infiniumPostResuspensionHybOvenJaxb.setStation("Hyb Oven #1");
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostResuspensionHybOvenJaxb);

        for (int i = 0; i < 96 / 24; i++) {
            hybridizationChips.add(testPrefix + "HybridizationChip" + i);
        }

        // 24 chip type
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            int chipIndex = i % 24;
            int chipNum = i / 24;
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(ampPlate,
                    bettaLimsMessageTestFactory.buildWellName(i + 1, BettaLimsMessageTestFactory.WellNameType.LONG),
                    hybridizationChips.get(chipNum),
                    String.format("R%02dC%02d", (chipIndex % 12)  + 1, (chipIndex / 12) + 1)));
        }

        infiniumHybridizationJaxb = bettaLimsMessageTestFactory.buildPlateToPlateCherryPick("InfiniumHybridization",
                ampPlate, hybridizationChips, cherryPicks);
        for (int i = 0; i < 4; i++) {
            infiniumHybridizationJaxb.getPlate().get(i).setPhysType("InfiniumChip24");
        }
        infiniumHybridizationJaxb.getSourcePlate().get(0).setPhysType("DeepWell96");
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumHybridizationJaxb);

        for(String hybridizationChip: hybridizationChips) {
            infiniumPostHybridizationHybOvenLoadedJaxb =
                    bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostHybridizationHybOvenLoaded", hybridizationChip);
            infiniumPostHybridizationHybOvenLoadedJaxb.setStation("Hyb Oven #1");
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostHybridizationHybOvenLoadedJaxb);
        }

        for (String hybridizationChip : hybridizationChips) {
            infiniumHybChamberLoadedJaxb =
                    bettaLimsMessageTestFactory.buildPlateEvent("InfiniumHybChamberLoaded", hybridizationChip);
            infiniumHybChamberLoadedJaxb.setStation("Hyb Carrier #31");
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumHybChamberLoadedJaxb);
        }
        for (String hybridizationChip : hybridizationChips) {
            infiniumWashJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumWash", hybridizationChip,
                    Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("PB1", "1234-PB1", null)));
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumWashJaxb);
        }
        for (String hybridizationChip : hybridizationChips) {
            infiniumXStainJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumXStain", hybridizationChip,
                    Arrays.asList(
                            new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234-RA1", null),
                            new BettaLimsMessageTestFactory.ReagentDto("LX1", "1234-LX1", null),
                            new BettaLimsMessageTestFactory.ReagentDto("LX2", "1234-LX2", null),
                            new BettaLimsMessageTestFactory.ReagentDto("XC3", "1234-XC3", null),
                            new BettaLimsMessageTestFactory.ReagentDto("XC4", "1234-XC4", null),
                            new BettaLimsMessageTestFactory.ReagentDto("SML", "1234-SML", null),
                            new BettaLimsMessageTestFactory.ReagentDto("ATM", "1234-ATM", null),
                            new BettaLimsMessageTestFactory.ReagentDto("EML", "1234-EML", null)));
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumXStainJaxb);
        }

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

    public PlateEventType getInfiniumPostHybridizationHybOvenLoadedJaxb() {
        return infiniumPostHybridizationHybOvenLoadedJaxb;
    }

    public PlateEventType getInfiniumHybChamberLoadedJaxb() {
        return infiniumHybChamberLoadedJaxb;
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
