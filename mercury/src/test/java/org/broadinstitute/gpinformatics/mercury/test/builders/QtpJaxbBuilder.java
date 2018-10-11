package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds JAXB objects for QTP messages
 */
public class QtpJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<List<String>> listLcsetListNormCatchBarcodes;
    private final List<String> normCatchRackBarcodes;
    private final boolean doPoolingTransfer;
    private final PcrType pcrType;

    private String poolRackBarcode;
    private List<String> poolTubeBarcodes = new ArrayList<>();
    private PlateCherryPickEvent poolingTransferJaxb;
    private String denatureRackBarcode;
    private PlateCherryPickEvent denatureJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private String denatureTubeBarcode;
    private String ecoPlateBarcode;
    private PlateTransferEventType ecoTransferDuplicateA3Jaxb;
    private String viia7PlateBarcode;
    private PlateTransferEventType viia7TransferJaxb;
    private String normalizationRackBarcode;
    private PlateCherryPickEvent normalizationJaxb;
    private final List<String> normalizationTubeBarcodes = new ArrayList<>();
    private List<String> denatureTubeBarcodes = new ArrayList<>();

    private BettaLIMSMessage poolingTransferMessage;
    private BettaLIMSMessage ecoTransferMessage;
    private BettaLIMSMessage viia7TransferMessage;
    private BettaLIMSMessage denatureMessage;
    private BettaLIMSMessage stripTubeTransferMessage;
    private BettaLIMSMessage flowcellTransferMessage;
    private BettaLIMSMessage flowcellLoadMessage;
    private PlateTransferEventType ecoTransferTriplicateA3;
    private PlateTransferEventType ecoTransferTriplicateA5;
    private PlateTransferEventType ecoTransferTriplicateA7;
    private BettaLIMSMessage ecoTransferTriplicateMessage;
    private PlateTransferEventType ecoTransferDuplicateB3Jaxb;
    private final Map<String, String> tubeToPosition = new HashMap<>();

    public QtpJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageFactory, String testPrefix,
            List<List<String>> listLcsetListNormCatchBarcodes, List<String> normCatchRackBarcodes,
            boolean doPoolingTransfer, PcrType pcrType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageFactory;
        this.testPrefix = testPrefix;
        this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
        this.normCatchRackBarcodes = normCatchRackBarcodes;
        this.doPoolingTransfer = doPoolingTransfer;
        this.pcrType = pcrType;
    }

    public String getPoolRackBarcode() {
        return poolRackBarcode;
    }

    public List<String> getPoolTubeBarcodes() {
        return poolTubeBarcodes;
    }

    public PlateCherryPickEvent getPoolingTransferJaxb() {
        return poolingTransferJaxb;
    }

    public PlateTransferEventType getEcoTransferDuplicateA3Jaxb() {
        return ecoTransferDuplicateA3Jaxb;
    }

    public PlateTransferEventType getEcoTransferDuplicateB3Jaxb() {
        return ecoTransferDuplicateB3Jaxb;
    }

    public PlateTransferEventType getEcoTransferTriplicateA7() {
        return ecoTransferTriplicateA7;
    }

    public PlateTransferEventType getEcoTransferTriplicateA5() {
        return ecoTransferTriplicateA5;
    }

    public PlateTransferEventType getEcoTransferTriplicateA3() {
        return ecoTransferTriplicateA3;
    }

    public PlateTransferEventType getViia7TransferJaxb() {
        return viia7TransferJaxb;
    }

    public String getDenatureRackBarcode() {
        return denatureRackBarcode;
    }

    public PlateCherryPickEvent getDenatureJaxb() {
        return denatureJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public List<String> getNormalizationTubeBarcodes() {
        return normalizationTubeBarcodes;
    }

    public String getNormalizationRackBarcode() {
        return normalizationRackBarcode;
    }

    public PlateCherryPickEvent getNormalizationJaxb() {
        return normalizationJaxb;
    }

    /** Adds the optional pooling transfer message and the eco/viia7 quant messages. */
    public QtpJaxbBuilder invokeToQuant() {
        if (doPoolingTransfer) {
            int i = 0;
            // PoolingTransfer
            poolRackBarcode = "PoolRack" + testPrefix;
            for (List<String> normCatchBarcodes : listLcsetListNormCatchBarcodes) {
                List<BettaLimsMessageTestFactory.CherryPick> poolingCherryPicks =
                        new ArrayList<>();
                for (int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                    poolingCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(normCatchRackBarcodes.get(i),
                            bettaLimsMessageTestFactory.buildWellName(rackPosition,
                                    BettaLimsMessageTestFactory.WellNameType.SHORT), poolRackBarcode,
                            "A1"));
                }
                poolTubeBarcodes.add("Pool" + testPrefix + i);
                poolingTransferJaxb = bettaLimsMessageTestFactory.buildCherryPick("PoolingTransfer",
                        Arrays.asList(normCatchRackBarcodes.get(i)), Collections.singletonList(normCatchBarcodes),
                        Collections.singletonList(poolRackBarcode),
                        Collections.singletonList(Collections.singletonList(poolTubeBarcodes.get(i))), poolingCherryPicks);
                poolingTransferMessage = bettaLimsMessageTestFactory.addMessage(messageList, poolingTransferJaxb);
                i++;
            }
        } else {
            poolRackBarcode = normCatchRackBarcodes.get(0);
            for (List<String> normCatchBarcodes : listLcsetListNormCatchBarcodes) {
                poolTubeBarcodes.addAll(normCatchBarcodes);
            }
        }

        ecoPlateBarcode = "EcoPlate" + testPrefix;

        switch (pcrType) {
            case ECO_DUPLICATE:
                String[] sectionPositions = {"A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3",
                        "D1", "D2", "D3", "E1", "E2", "E3", "F1", "F2", "F3"};
                ecoTransferDuplicateA3Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("EcoTransfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "3BY6A1", "8BY6A3ALTROWS");

                ecoTransferDuplicateB3Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("EcoTransfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "3BY6A1", "8BY6B3ALTROWS");
                for(int i = 0; i < poolTubeBarcodes.size(); i++) {
                    String position = sectionPositions[i];
                    ecoTransferDuplicateA3Jaxb.getSourcePositionMap().getReceptacle().get(i).setPosition(position);
                    ecoTransferDuplicateB3Jaxb.getSourcePositionMap().getReceptacle().get(i).setPosition(position);
                }

                ecoTransferMessage = bettaLimsMessageTestFactory.addMessage(
                        messageList, ecoTransferDuplicateA3Jaxb, ecoTransferDuplicateB3Jaxb);
                break;
            case ECO_TRIPLICATE:
                ecoTransferTriplicateA3 = bettaLimsMessageTestFactory.buildRackToPlate("EcoTransfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "2BY6A1", "8BY6A3COLWISE2");

                ecoTransferTriplicateA5 = bettaLimsMessageTestFactory.buildRackToPlate("EcoTransfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "2BY6A1", "8BY6A5COLWISE2_ALT");

                ecoTransferTriplicateA7 = bettaLimsMessageTestFactory.buildRackToPlate("EcoTransfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "2BY6A1", "8BY6A7COLWISE2");
                ecoTransferTriplicateMessage = bettaLimsMessageTestFactory.addMessage(
                        messageList, ecoTransferTriplicateA3, ecoTransferTriplicateA5, ecoTransferTriplicateA7);
                break;
            case VIIA_7:
                // Viia7Transfer reuses the eco barcode since they are interchangable from workflow point of view.
                viia7TransferJaxb = bettaLimsMessageTestFactory.buildRackToPlate("Viia7Transfer", poolRackBarcode,
                        poolTubeBarcodes, ecoPlateBarcode, "Eco48", "3BY6A1", "8BY6A3ALTROWS");
                viia7TransferMessage = bettaLimsMessageTestFactory.addMessage(messageList, viia7TransferJaxb);
                break;
        }

        return this;
    }

    /** Adds the normalization and denature transfer messages. */
    public QtpJaxbBuilder invokePostQuant() {

        // NormalizationTransfer
        normalizationRackBarcode = "NormalizationRack" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> normaliztionCherryPicks = makeCherryPicks(poolRackBarcode,
                normalizationRackBarcode, poolTubeBarcodes, normalizationTubeBarcodes, tubeToPosition,
                bettaLimsMessageTestFactory, "Norm", testPrefix);
        normalizationJaxb = bettaLimsMessageTestFactory.buildCherryPick("NormalizationTransfer",
                Collections.singletonList(poolRackBarcode), Collections.singletonList(poolTubeBarcodes),
                Collections.singletonList(normalizationRackBarcode),
                Collections.singletonList(normalizationTubeBarcodes),
                normaliztionCherryPicks);
        fixupPositionMap(tubeToPosition, normalizationJaxb.getSourcePositionMap(), normalizationJaxb.getPositionMap());
        bettaLimsMessageTestFactory.addMessage(messageList, normalizationJaxb);

        // DenatureTransfer
        denatureRackBarcode = "DenatureRack" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> denatureCherryPicks = makeCherryPicks(normalizationRackBarcode,
                denatureRackBarcode, normalizationTubeBarcodes, denatureTubeBarcodes, tubeToPosition,
                bettaLimsMessageTestFactory, "Denature", testPrefix);
        denatureJaxb = bettaLimsMessageTestFactory.buildCherryPick("DenatureTransfer",
                Collections.singletonList(normalizationRackBarcode),
                Collections.singletonList(normalizationTubeBarcodes),
                Collections.singletonList(denatureRackBarcode), Collections.singletonList(denatureTubeBarcodes),
                denatureCherryPicks);
        fixupPositionMap(tubeToPosition, denatureJaxb.getSourcePositionMap(), denatureJaxb.getPositionMap());

        denatureMessage = bettaLimsMessageTestFactory.addMessage(messageList, denatureJaxb);

        return this;
    }

    public static List<BettaLimsMessageTestFactory.CherryPick> makeCherryPicks(String sourceRack, String destRack,
            List<String> sourceTubes, List<String> destTubes, Map<String, String> tubeToPosition,
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String destTubePrefix, String testPrefix) {

        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (int j = 0; j < sourceTubes.size(); j++) {
            // Uses tube positions if given, possibly rearraying the source tubes. If not given puts them in a row.
            String sourcePosition = tubeToPosition.containsKey(sourceTubes.get(j)) ?
                    tubeToPosition.get(sourceTubes.get(j)) :
                    bettaLimsMessageTestFactory.buildWellName(j + 1, BettaLimsMessageTestFactory.WellNameType.SHORT);
            String destPosition = sourcePosition;
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(sourceRack, sourcePosition,
                    destRack, destPosition));
            String destTube = destTubePrefix + testPrefix + j;
            tubeToPosition.put(destTube, destPosition);
            destTubes.add(destTube);
        }
        return cherryPicks;
    }

    /** Updates the jaxb position maps with the actual tube positions. */
    public static void fixupPositionMap(Map<String, String> tubeToPosition, List<PositionMapType>... positionMaps) {
        // Extracts each receptacleType and updates the position if the barcode is in the tubeToPosition map.
        Arrays.asList(positionMaps).stream().
                flatMap(List::stream).
                map(PositionMapType::getReceptacle).
                flatMap(List::stream).
                filter(receptacle -> tubeToPosition.containsKey(receptacle.getBarcode())).
                forEach(receptacle -> receptacle.setPosition(tubeToPosition.get(receptacle.getBarcode())));
    }

    public BettaLIMSMessage getStep01PoolingTransferMessage() {
        return poolingTransferMessage;
    }

    public BettaLIMSMessage getStep02EcoTransferMessage() {
        return ecoTransferMessage;
    }

    public BettaLIMSMessage getStep02Viia7TransferMessage() {
        return viia7TransferMessage;
    }

    public BettaLIMSMessage getStep03DenatureMessage() {
        return denatureMessage;
    }

    public BettaLIMSMessage getStep04StripTubeTransferMessage() {
        return stripTubeTransferMessage;
    }

    public BettaLIMSMessage getStep05FlowcellTransferMessage() {
        return flowcellTransferMessage;
    }

    public BettaLIMSMessage getStep06FlowcellLoadMessage() {
        return flowcellLoadMessage;
    }

    public List<String> getDenatureTubeBarcodes() {
        return denatureTubeBarcodes;
    }

    public enum PcrType {
        ECO_DUPLICATE, ECO_TRIPLICATE, VIIA_7
    }

    public Map<String, String> getTubeToPosition() {
        return tubeToPosition;
    }
}
