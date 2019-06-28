package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Builds JAXB objects for Infinium messages.
 */
public class InfiniumJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String sourcePlate;
    private final int numSamples;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    private String ampPlate;
    private List<String> hybridizationChips = new ArrayList<>();
    private PlateTransferEventType infiniumMethlationZymoPlateJaxb;
    private PlateEventType infiniumMethylationBufferAddition1Jaxb;
    private PlateEventType infiniumMethylationBufferAddition2Jaxb;
    private PlateTransferEventType infiniumMethlationFilterPlateJaxb;
    private PlateEventType infiniumMethylationWash1Jaxb;
    private PlateEventType infiniumMethylationDesulphonationJaxb;
    private PlateEventType infiniumMethylationWash2Jaxb;
    private PlateEventType infiniumMethylationWash3Jaxb;
    private PlateTransferEventType InfiniumMethylationElutionJaxb;
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
    private final InfiniumJaxbBuilder.IncludeMethylation includeMethylation;
    private List<Triple<String, String, Integer>> amplificationReagents;
    private List<Triple<String, String, Integer>> amplificationReagentReagents;
    private List<Triple<String, String, Integer>> fragmentationReagents;
    private List<Triple<String, String, Integer>> precipitationReagents;
    private List<Triple<String, String, Integer>> precipitationIsopropanolReagents;
    private List<Triple<String, String, Integer>> resuspensionReagents;
    private List<Triple<String, String, Integer>> xstainReagents;
    private String filterPlate;
    private String elutionPlate;
    private String zymoPlate;

    public enum IncludeMethylation {
        TRUE, FALSE
    }

    public InfiniumJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                               String sourcePlate, int numSamples,
                               InfiniumJaxbBuilder.IncludeMethylation includeMethylation,
                               List<Triple<String, String, Integer>> amplificationReagents,
                               List<Triple<String, String, Integer>> amplificationReagentReagents,
                               List<Triple<String, String, Integer>> fragmentationReagents,
                               List<Triple<String, String, Integer>> precipitationReagents,
                               List<Triple<String, String, Integer>> precipitationIsopropanolReagents,
                               List<Triple<String, String, Integer>> resuspensionReagents,
                               List<Triple<String, String, Integer>> xstainReagents) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourcePlate = sourcePlate;
        this.numSamples = numSamples;
        this.includeMethylation = includeMethylation;
        this.amplificationReagents = amplificationReagents;
        this.amplificationReagentReagents = amplificationReagentReagents;
        this.fragmentationReagents = fragmentationReagents;
        this.precipitationReagents = precipitationReagents;
        this.precipitationIsopropanolReagents = precipitationIsopropanolReagents;
        this.resuspensionReagents = resuspensionReagents;
        this.xstainReagents = xstainReagents;
    }

    public InfiniumJaxbBuilder invoke() {
        ampPlate = testPrefix + "AmplificationPlate";
        if (includeMethylation == InfiniumJaxbBuilder.IncludeMethylation.TRUE) {
            zymoPlate = testPrefix + "ZymoPlate";
            infiniumMethlationZymoPlateJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "InfiniumMethylationZymoTransferElution", sourcePlate, zymoPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethlationZymoPlateJaxb);

            infiniumMethylationBufferAddition1Jaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationBufferAddition1", zymoPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationBufferAddition1Jaxb);

            infiniumMethylationBufferAddition2Jaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationBufferAddition2", zymoPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationBufferAddition2Jaxb);

            filterPlate = testPrefix + "FilterPlate";
            infiniumMethlationFilterPlateJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "InfiniumMethylationFilterPlateTransfer", zymoPlate, filterPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethlationFilterPlateJaxb);

            infiniumMethylationWash1Jaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationWash1", filterPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationWash1Jaxb);

            infiniumMethylationDesulphonationJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationDesulphonation", filterPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationDesulphonationJaxb);

            infiniumMethylationWash2Jaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationWash2", filterPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationWash2Jaxb);

            infiniumMethylationWash3Jaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    "InfiniumMethylationWash3", filterPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, infiniumMethylationWash3Jaxb);

            elutionPlate = testPrefix + "ElutionPlate";
            InfiniumMethylationElutionJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "InfiniumMethylationElution", filterPlate, elutionPlate);
            bettaLimsMessageTestFactory.addMessage(messageList, InfiniumMethylationElutionJaxb);

            infiniumAmplificationJaxb =
                    bettaLimsMessageTestFactory.buildPlateToPlate("InfiniumAmplification", elutionPlate, ampPlate);
        } else {
            infiniumAmplificationJaxb =
                    bettaLimsMessageTestFactory.buildPlateToPlate("InfiniumAmplification", sourcePlate, ampPlate);
        }

        for (Triple<String, String, Integer> typeLotYearOffset : amplificationReagents) {
            ReagentType reagentType = new ReagentType();
            reagentType.setKitType(typeLotYearOffset.getLeft());
            reagentType.setBarcode(typeLotYearOffset.getMiddle());
            GregorianCalendar expiration = new GregorianCalendar();
            expiration.add(Calendar.YEAR, typeLotYearOffset.getRight());
            reagentType.setExpiration(expiration.getTime());
            infiniumAmplificationJaxb.getReagent().add(reagentType);
        }
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationJaxb);

        infiniumAmplificationReagentAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "InfiniumAmplificationReagentAddition", ampPlate,
                BettaLimsMessageTestFactory.reagentList(amplificationReagentReagents));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumAmplificationReagentAdditionJaxb);

        infiniumFragmentationJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumFragmentation", ampPlate,
                BettaLimsMessageTestFactory.reagentList(fragmentationReagents));
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
                BettaLimsMessageTestFactory.reagentList(precipitationReagents));
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
                "InfiniumPrecipitationIsopropanolAddition", ampPlate,
                BettaLimsMessageTestFactory.reagentList(precipitationIsopropanolReagents));
        bettaLimsMessageTestFactory.addMessage(messageList, infiniumPrecipitationIsopropanolAdditionJaxb);

        infiniumResuspensionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("InfiniumResuspension", ampPlate,
                BettaLimsMessageTestFactory.reagentList(resuspensionReagents));
        bettaLimsMessageTestFactory.advanceTime();

        infiniumPostResuspensionHybOvenJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("InfiniumPostResuspensionHybOven", ampPlate);
        bettaLimsMessageTestFactory.advanceTime();
        infiniumPostResuspensionHybOvenJaxb.setStation("Hyb Oven #1");
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateEvent().add(infiniumResuspensionJaxb);
        bettaLIMSMessage.getPlateEvent().add(infiniumPostResuspensionHybOvenJaxb);

        int numChips = (int) Math.ceil(numSamples / 24.0);
        for (int i = 0; i < numChips; i++) {
            hybridizationChips.add(testPrefix + "HC" + i);
        }

        // 24 chip type
        // 4 separate InfiniumHybridization, each followed by InfiniumHybChamberLoaded
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        bettaLIMSMessage = new BettaLIMSMessage();
        // The decks don't "know" that wells are empty, so do a cherry pick for every destination
        for (int i = 0; i < numChips * 24; i++) {
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

        for (String hybridizationChip : hybridizationChips) {
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
                    hybridizationChip, BettaLimsMessageTestFactory.reagentList(xstainReagents));
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

    public PlateEventType getInfiniumMethylationBufferAddition1Jaxb() {
        return infiniumMethylationBufferAddition1Jaxb;
    }

    public PlateEventType getInfiniumMethylationBufferAddition2Jaxb() {
        return infiniumMethylationBufferAddition2Jaxb;
    }

    public PlateTransferEventType getInfiniumMethlationFilterPlateJaxb() {
        return infiniumMethlationFilterPlateJaxb;
    }

    public PlateEventType getInfiniumMethylationWash1Jaxb() {
        return infiniumMethylationWash1Jaxb;
    }

    public PlateEventType getInfiniumMethylationDesulphonationJaxb() {
        return infiniumMethylationDesulphonationJaxb;
    }

    public PlateEventType getInfiniumMethylationWash2Jaxb() {
        return infiniumMethylationWash2Jaxb;
    }

    public PlateEventType getInfiniumMethylationWash3Jaxb() {
        return infiniumMethylationWash3Jaxb;
    }

    public PlateTransferEventType getInfiniumMethylationElutionJaxb() {
        return InfiniumMethylationElutionJaxb;
    }

    public PlateTransferEventType getInfiniumMethlationZymoPlateJaxb() {
        return infiniumMethlationZymoPlateJaxb;
    }

    public String getZymoPlate() {
        return zymoPlate;
    }

    public InfiniumJaxbBuilder.IncludeMethylation getIncludeMethylation() {
        return includeMethylation;
    }
}
