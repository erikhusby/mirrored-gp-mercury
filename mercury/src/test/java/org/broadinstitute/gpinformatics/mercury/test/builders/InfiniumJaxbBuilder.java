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
    private List<PlateCherryPickEvent> infiniumHybridizationJaxbs = new ArrayList<>();
    private List<PlateEventType> infiniumPostHybridizationHybOvenLoadedJaxbs = new ArrayList<>();
    private List<PlateEventType> infiniumHybChamberLoadedJaxbs = new ArrayList<>();
    private List<PlateEventType> infiniumXStainJaxbs = new ArrayList<>();

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
        bettaLimsMessageTestFactory.advanceTime();

        infiniumPostFragmentationHybOvenLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostFragmentationHybOvenLoaded", ampPlate);
        bettaLimsMessageTestFactory.advanceTime();
        infiniumPostFragmentationHybOvenLoadedJaxb.setStation("Hyb Oven #1");
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumFragmentationJaxb);
        bettaLIMSMessage.getPlateEvent().add(infiniumPostFragmentationHybOvenLoadedJaxb);
        messageList.add(bettaLIMSMessage);

        infiniumPrecipitationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPrecipitation", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("PM1", "1234-PM1", null)));
        bettaLimsMessageTestFactory.advanceTime();

        infiniumPostPrecipitationHeatBlockLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostPrecipitationHeatBlockLoaded", ampPlate);
        bettaLimsMessageTestFactory.advanceTime();
        infiniumPostPrecipitationHeatBlockLoadedJaxb.setStation("Heat Block #4");
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumPrecipitationJaxb);
        bettaLIMSMessage.getPlateEvent().add(infiniumPostPrecipitationHeatBlockLoadedJaxb);
        messageList.add(bettaLIMSMessage);

        infiniumPrecipitationIsopropanolAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumPrecipitationIsopropanolAddition", ampPlate, Collections.singletonList(
                        new BettaLimsMessageTestFactory.ReagentDto("Isopropanol", "2345", null)));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationIsopropanolAdditionJaxb);

        infiniumResuspensionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumResuspension", ampPlate,
                Collections.singletonList(new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234-RA1", null)));
        bettaLimsMessageTestFactory.advanceTime();

        infiniumPostResuspensionHybOvenJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostResuspensionHybOven", ampPlate);
        bettaLimsMessageTestFactory.advanceTime();
        infiniumPostResuspensionHybOvenJaxb.setStation("Hyb Oven #1");
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumResuspensionJaxb);
        bettaLIMSMessage.getPlateEvent().add(infiniumPostResuspensionHybOvenJaxb);

        for (int i = 0; i < 96 / 24; i++) {
            hybridizationChips.add(testPrefix + "HybridizationChip" + i);
        }

        // 24 chip type
        // 4 separate InfiniumHybridization, each followed by InfiniumHybChamberLoaded
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        bettaLIMSMessage = new BettaLIMSMessage();
        for (int i = 0; i < 96; i++) {
            int chipIndex = i % 24;
            int chipNum = i / 24;
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(ampPlate,
                    bettaLimsMessageTestFactory.buildWellName(i + 1, BettaLimsMessageTestFactory.WellNameType.LONG),
                    hybridizationChips.get(chipNum),
                    String.format("R%02dC%02d", (chipIndex % 12)  + 1, (chipIndex / 12) + 1)));
            if (chipIndex == 23) {
                PlateCherryPickEvent infiniumHybridization = bettaLimsMessageTestFactory.buildPlateToPlateCherryPick(
                        "InfiniumHybridization", ampPlate, Collections.singletonList(hybridizationChips.get(chipNum)),
                        cherryPicks);
                infiniumHybridizationJaxbs.add(infiniumHybridization);
                bettaLimsMessageTestFactory.advanceTime();
                infiniumHybridization.getPlate().get(0).setPhysType("InfiniumChip24");
                infiniumHybridization.getSourcePlate().get(0).setPhysType("DeepWell96");
                bettaLIMSMessage.getPlateCherryPickEvent().add(infiniumHybridization);

                PlateEventType infiniumHybChamberLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                        "InfiniumHybChamberLoaded", hybridizationChips.get(chipNum));
                infiniumHybChamberLoadedJaxbs.add(infiniumHybChamberLoaded);
                bettaLimsMessageTestFactory.advanceTime();
                infiniumHybChamberLoaded.setStation("Hyb Carrier #31");
                bettaLIMSMessage.getPlateEvent().add(infiniumHybChamberLoaded);
                cherryPicks.clear();
            }
        }
        messageList.add(bettaLIMSMessage);

        for(String hybridizationChip: hybridizationChips) {
            PlateEventType infiniumPostHybridizationHybOvenLoadedJaxb =
                    bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostHybridizationHybOvenLoaded", hybridizationChip);
            infiniumPostHybridizationHybOvenLoadedJaxb.setStation("Hyb Oven #1");
            infiniumPostHybridizationHybOvenLoadedJaxbs.add(infiniumPostHybridizationHybOvenLoadedJaxb);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumPostHybridizationHybOvenLoadedJaxb);
        }

        bettaLIMSMessage = new BettaLIMSMessage();
        for (String hybridizationChip : hybridizationChips) {
            // XStain is actually done through the Manual Transfer Page.  It is included here to drive GPUI.
            PlateEventType infiniumXStain = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumXStain",
                    hybridizationChip, Arrays.asList(
                            new BettaLimsMessageTestFactory.ReagentDto("RA1", "1234-RA1", null),
                            new BettaLimsMessageTestFactory.ReagentDto("LX1", "1234-LX1", null),
                            new BettaLimsMessageTestFactory.ReagentDto("LX2", "1234-LX2", null),
                            new BettaLimsMessageTestFactory.ReagentDto("XC3", "1234-XC3", null),
                            new BettaLimsMessageTestFactory.ReagentDto("XC4", "1234-XC4", null),
                            new BettaLimsMessageTestFactory.ReagentDto("SML", "1234-SML", null),
                            new BettaLimsMessageTestFactory.ReagentDto("ATM", "1234-ATM", null),
                            new BettaLimsMessageTestFactory.ReagentDto("EML", "1234-EML", null),
                            new BettaLimsMessageTestFactory.ReagentDto("PB1", "1234-PB1", null)));
            infiniumXStainJaxbs.add(infiniumXStain);
            bettaLIMSMessage.getPlateEvent().add(infiniumXStain);
            bettaLimsMessageTestFactory.advanceTime();
        }
        messageList.add(bettaLIMSMessage);

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

    public List<PlateCherryPickEvent> getInfiniumHybridizationJaxbs() {
        return infiniumHybridizationJaxbs;
    }

    public List<PlateEventType> getInfiniumPostHybridizationHybOvenLoadedJaxbs() {
        return infiniumPostHybridizationHybOvenLoadedJaxbs;
    }

    public List<PlateEventType> getInfiniumHybChamberLoadedJaxbs() {
        return infiniumHybChamberLoadedJaxbs;
    }

    public List<PlateEventType> getInfiniumXStainJaxbs() {
        return infiniumXStainJaxbs;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
