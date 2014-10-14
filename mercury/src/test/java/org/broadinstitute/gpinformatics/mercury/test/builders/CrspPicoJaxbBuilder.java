package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds JAXB BettaLIMS DTOs to test messaging for CRSP Pico.
 */
public class CrspPicoJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String rackBarcode;
    private final List<String> tubeBarcodes;

    private List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateEventType initialTareJaxb;
    private PlateEventType weightMeasurementJaxb;
    private PlateEventType volumeAdditionJaxb;
    private PlateTransferEventType initialPicoTransfer1;
    private PlateTransferEventType initialPicoTransfer2;
    private PlateEventType initialPicoBufferAddition1;
    private PlateEventType initialPicoBufferAddition2;
    private PlateTransferEventType fingerprintingAliquotJaxb;
    private PlateTransferEventType fingerprintingPlateSetup;
    private PlateTransferEventType shearingAliquot;

    public CrspPicoJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix, String rackBarcode,
            List<String> tubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
        this.tubeBarcodes = tubeBarcodes;
    }

    public CrspPicoJaxbBuilder invoke() {
        // Need to test a variety of next steps: FP Daughter, Shearing Daughter, Exclude?
        // Set Pico upload values round-robin to achieve desired outcomes.
        // Re-array based on next steps.
        // Need to keep track of changing volumes (and concentrations?) within test: map from barcode to BigDecimal

        Map<String, BigDecimal> mapBarcodeToVolume = new HashMap<>();
        // InitialTare
        initialTareJaxb = bettaLimsMessageTestFactory.buildRackEvent("InitialTare", rackBarcode,
                tubeBarcodes);
        for (ReceptacleType receptacleType : initialTareJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setReceptacleWeight(new BigDecimal("0.63"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, initialTareJaxb);

        // WeightMeasurement
        weightMeasurementJaxb = bettaLimsMessageTestFactory.buildRackEvent("WeightMeasurement",
                rackBarcode, tubeBarcodes);
        int i = 1;
        for (ReceptacleType receptacleType : weightMeasurementJaxb.getPositionMap().getReceptacle()) {
            BigDecimal volume = new BigDecimal(i % 2 == 0 ? "55" : "75");
            mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
            receptacleType.setVolume(volume);
            i++;
        }
        bettaLimsMessageTestFactory.addMessage(messageList, weightMeasurementJaxb);

        // VolumeAddition, only tubes with volume < 65
        volumeAdditionJaxb = bettaLimsMessageTestFactory.buildRackEvent("VolumeAddition", rackBarcode,
                tubeBarcodes);
        i = 1;
        for (ReceptacleType receptacleType : volumeAdditionJaxb.getPositionMap().getReceptacle()) {
            if (i % 2 == 0) {
                BigDecimal volume = new BigDecimal("65.0");
                mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
                receptacleType.setVolume(volume);
            }
        }
        bettaLimsMessageTestFactory.addMessage(messageList, volumeAdditionJaxb);

        // Initial PicoTransfer
        String initialPico1 = "11" + testPrefix;
        initialPicoTransfer1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoTransfer", rackBarcode,
                tubeBarcodes, initialPico1);
        for (ReceptacleType receptacleType : initialPicoTransfer1.getSourcePositionMap().getReceptacle()) {
            BigDecimal volume = mapBarcodeToVolume.get(receptacleType.getBarcode());
            volume = volume.subtract(new BigDecimal("2"));
            mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
            receptacleType.setVolume(volume);
        }
        String initialPico2 = "22" + testPrefix;
        initialPicoTransfer2 = bettaLimsMessageTestFactory.buildRackToPlate("PicoTransfer", rackBarcode, tubeBarcodes,
                initialPico2);

        // Initial PicoBufferAddition
        initialPicoBufferAddition1 = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", initialPico1);
        initialPicoBufferAddition2 = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", initialPico2);

        // FingerprintingAliquot, only tubes with concentration > 60
        String fpRackBarcode = "fpr" + testPrefix;
        int splitPoint = tubeBarcodes.size() / 2;
        List<String> fpSourceTubes = tubeBarcodes.subList(0, splitPoint);
        List<String> fpTargetTubes = new ArrayList<>(fpSourceTubes.size());
        for (int rackPosition = 1; rackPosition <= fpSourceTubes.size(); rackPosition++) {
            fpTargetTubes.add("fpt" + testPrefix + rackPosition);
        }
        fingerprintingAliquotJaxb = bettaLimsMessageTestFactory.buildRackToRack(
                "FingerprintingAliquot", rackBarcode, fpSourceTubes, fpRackBarcode, fpTargetTubes);
        for (ReceptacleType receptacleType : fingerprintingAliquotJaxb.getSourcePositionMap().getReceptacle()) {
            BigDecimal volume = mapBarcodeToVolume.get(receptacleType.getBarcode());
            volume = volume.subtract(new BigDecimal("12"));
            mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
            receptacleType.setVolume(volume);
        }
        for (ReceptacleType receptacleType : fingerprintingAliquotJaxb.getPositionMap().getReceptacle()) {
            BigDecimal volume = new BigDecimal("40");
            mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
            receptacleType.setVolume(volume);
            receptacleType.setConcentration(new BigDecimal("20"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingAliquotJaxb);

        // todo jmt FP PicoTransfer
        // FP PicoBufferAddition

        List<String> postFpTubes = new ArrayList<>(tubeBarcodes.size());
        postFpTubes.addAll(fpTargetTubes);
        postFpTubes.addAll(tubeBarcodes.subList(splitPoint, tubeBarcodes.size()));
        // FingerprintingPlateSetup
        String fpPlate = "fpp" + testPrefix;
        fingerprintingPlateSetup = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup",
                fpRackBarcode, postFpTubes, fpPlate);
        MetadataType metadataType = new MetadataType();
        metadataType.setName("Volume");
        metadataType.setValue("2.5");
        fingerprintingPlateSetup.getMetadata().add(metadataType);
        // todo jmt should set source volume?

        // ShearingAliquot
        String shearingRackBarcode = "sr" + testPrefix;
        List<String> shearingTubes = new ArrayList<>();
        shearingAliquot = bettaLimsMessageTestFactory.buildRackToRack("ShearingAliquot",
                rackBarcode, postFpTubes, shearingRackBarcode, shearingTubes);
        for (ReceptacleType receptacleType : shearingAliquot.getSourcePositionMap().getReceptacle()) {
            BigDecimal volume = mapBarcodeToVolume.get(receptacleType.getBarcode());
            volume = volume.subtract(new BigDecimal("46"));
            mapBarcodeToVolume.put(receptacleType.getBarcode(), volume);
            receptacleType.setVolume(volume);
        }
        for (ReceptacleType receptacleType : shearingAliquot.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("60"));
            receptacleType.setConcentration(new BigDecimal("3"));
        }

        // todo jmt Shearing PicoTransfer
        // Shearing PicoBufferAddition

        return this;
    }

    public PlateEventType getInitialTareJaxb() {
        return initialTareJaxb;
    }

    public PlateEventType getWeightMeasurementJaxb() {
        return weightMeasurementJaxb;
    }

    public PlateEventType getVolumeAdditionJaxb() {
        return volumeAdditionJaxb;
    }

    public PlateTransferEventType getInitialPicoTransfer1() {
        return initialPicoTransfer1;
    }

    public PlateTransferEventType getInitialPicoTransfer2() {
        return initialPicoTransfer2;
    }

    public PlateEventType getInitialPicoBufferAddition1() {
        return initialPicoBufferAddition1;
    }

    public PlateEventType getInitialPicoBufferAddition2() {
        return initialPicoBufferAddition2;
    }

    public PlateTransferEventType getFingerprintingAliquotJaxb() {
        return fingerprintingAliquotJaxb;
    }

    public PlateTransferEventType getFingerprintingPlateSetup() {
        return fingerprintingPlateSetup;
    }

    public PlateTransferEventType getShearingAliquot() {
        return shearingAliquot;
    }
}
