package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
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
    private PlateTransferEventType fingerprintingPicoTransfer1;
    private PlateTransferEventType fingerprintingPicoTransfer2;
    private PlateEventType fingerprintingPicoBufferAddition1;
    private PlateEventType fingerprintingPicoBufferAddition2;
    private PlateTransferEventType fingerprintingPlateSetup;
    private PlateTransferEventType shearingAliquot;
    private PlateTransferEventType shearingPicoTransfer1;
    private PlateTransferEventType shearingPicoTransfer2;
    private PlateEventType shearingPicoBufferAddition1;
    private PlateEventType shearingPicoBufferAddition2;

    private PlateTransferEventType picoTransfer1;
    private PlateTransferEventType picoTransfer2;
    private PlateEventType picoBufferAddition1;
    private PlateEventType picoBufferAddition2;

    public CrspPicoJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix, String rackBarcode,
            List<String> tubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
        this.tubeBarcodes = tubeBarcodes;
    }

    public CrspPicoJaxbBuilder invoke() {
        Map<String, BigDecimal> mapBarcodeToVolume = new HashMap<>();

        // InitialTare
        List<BigDecimal> weights = new ArrayList<>(tubeBarcodes.size());
        for (int i = 0; i < tubeBarcodes.size(); i++) {
            weights.add(new BigDecimal("0.63"));
        }
        buildInitialTare(rackBarcode, tubeBarcodes, weights);

        // WeightMeasurement
        int i = 0;
        List<BigDecimal> sourceVolumes = new ArrayList<>(tubeBarcodes.size());
        for (String tubeBarcode : tubeBarcodes) {
            BigDecimal volume = new BigDecimal(i % 2 == 0 ? "55" : "75");
            mapBarcodeToVolume.put(tubeBarcode, volume);
            sourceVolumes.add(volume);
            i++;
        }
        buildWeightMeasurement(rackBarcode, tubeBarcodes, sourceVolumes);

        // VolumeAddition, only tubes with volume < 65
        sourceVolumes.clear();
        i = 0;
        for (String tubeBarcode : tubeBarcodes) {
            if (i % 2 == 0) {
                BigDecimal volume = new BigDecimal("65.0");
                mapBarcodeToVolume.put(tubeBarcode, volume);
                sourceVolumes.add(volume);
            } else {
                sourceVolumes.add(mapBarcodeToVolume.get(tubeBarcode));
            }
            i++;
        }
        buildVolumeAddition(rackBarcode, tubeBarcodes, sourceVolumes);

        // Initial PicoTransfer
        sourceVolumes.clear();
        for (String tubeBarcode : tubeBarcodes) {
            BigDecimal volume = mapBarcodeToVolume.get(tubeBarcode);
            volume = volume.subtract(new BigDecimal("2"));
            mapBarcodeToVolume.put(tubeBarcode, volume);
            sourceVolumes.add(volume);
        }
        String initialPico1 = "11" + testPrefix;
        String initialPico2 = "22" + testPrefix;
        buildPicoTransfer(rackBarcode, tubeBarcodes, sourceVolumes, initialPico1, initialPico2);
        initialPicoTransfer1 = picoTransfer1;
        initialPicoTransfer2 = picoTransfer2;

        // Initial PicoBufferAddition
        buildPicoBufferAddition(initialPico1, initialPico2);
        // Increase pico volumes
        BigDecimal picoNewVolume = new BigDecimal(4);
        PositionMapType positionMap1 = new PositionMapType();
        PositionMapType positionMap2 = new PositionMapType();
        positionMap1.setBarcode(picoBufferAddition1.getPlate().getBarcode());
        positionMap2.setBarcode(picoBufferAddition2.getPlate().getBarcode());
        List<ReceptacleType> bufferReceptacles1 = new ArrayList<>();
        List<ReceptacleType> bufferReceptacles2 = new ArrayList<>();
        for (ReceptacleType receptacle : initialPicoTransfer1.getPositionMap().getReceptacle()) {
            ReceptacleType newReceptacle1 = new ReceptacleType();
            ReceptacleType newReceptacle2 = new ReceptacleType();
            newReceptacle1.setVolume(picoNewVolume);
            newReceptacle2.setVolume(picoNewVolume);
            newReceptacle1.setReceptacleType(receptacle.getReceptacleType());
            newReceptacle2.setReceptacleType(receptacle.getReceptacleType());
            newReceptacle1.setPosition(receptacle.getPosition());
            newReceptacle2.setPosition(receptacle.getPosition());
            bufferReceptacles1.add(newReceptacle1);
            bufferReceptacles2.add(newReceptacle2);
        }
        positionMap1.getReceptacle().addAll(bufferReceptacles1);
        picoBufferAddition1.setPositionMap(positionMap1);
        positionMap2.getReceptacle().addAll(bufferReceptacles2);
        picoBufferAddition2.setPositionMap(positionMap2);
        initialPicoBufferAddition1 = picoBufferAddition1;
        initialPicoBufferAddition2 = picoBufferAddition2;

        // FingerprintingAliquot, only tubes with concentration > 60
        String fpRackBarcode = "fpr" + testPrefix;
        int splitPoint = tubeBarcodes.size() / 2;
        List<String> fpSourceTubes = tubeBarcodes.subList(0, splitPoint);
        List<String> fpTargetTubes = new ArrayList<>(fpSourceTubes.size());
        for (int rackPosition = 1; rackPosition <= fpSourceTubes.size(); rackPosition++) {
            fpTargetTubes.add("fpt" + testPrefix + rackPosition);
        }

        sourceVolumes.clear();
        for (String fpSourceTube : fpSourceTubes) {
            BigDecimal volume = mapBarcodeToVolume.get(fpSourceTube);
            volume = volume.subtract(new BigDecimal("12"));
            mapBarcodeToVolume.put(fpSourceTube, volume);
            sourceVolumes.add(volume);
        }

        List<BigDecimal> targetVolumes = new ArrayList<>(tubeBarcodes.size());
        List<BigDecimal> targetConcentrations = new ArrayList<>(tubeBarcodes.size());
        for (String fpTargetTube : fpTargetTubes) {
            BigDecimal volume = new BigDecimal("40");
            mapBarcodeToVolume.put(fpTargetTube, volume);
            targetVolumes.add(volume);
            targetConcentrations.add(new BigDecimal("20"));
        }

        buildFingerprintingAliquot(rackBarcode, fpSourceTubes, sourceVolumes,
                fpRackBarcode, fpTargetTubes, targetVolumes, targetConcentrations);

        // FP PicoTransfer
        List<String> postFpTubes = new ArrayList<>(tubeBarcodes.size());
        postFpTubes.addAll(fpTargetTubes);
        postFpTubes.addAll(tubeBarcodes.subList(splitPoint, tubeBarcodes.size()));
        sourceVolumes.clear();
        for (String tubeBarcode : postFpTubes) {
            BigDecimal volume = mapBarcodeToVolume.get(tubeBarcode);
            volume = volume.subtract(new BigDecimal("2"));
            mapBarcodeToVolume.put(tubeBarcode, volume);
            sourceVolumes.add(volume);
        }
        String fpPico1 = "33" + testPrefix;
        String fpPico2 = "44" + testPrefix;
        buildPicoTransfer(fpRackBarcode, postFpTubes, sourceVolumes, fpPico1, fpPico2);
        fingerprintingPicoTransfer1 = picoTransfer1;
        fingerprintingPicoTransfer2 = picoTransfer2;

        // FP PicoBufferAddition
        buildPicoBufferAddition(fpPico1, fpPico2);
        fingerprintingPicoBufferAddition1 = picoBufferAddition1;
        fingerprintingPicoBufferAddition2 = picoBufferAddition2;

        // FingerprintingPlateSetup
        String fpPlate = "fpp" + testPrefix;
        sourceVolumes.clear();
        for (String postFpTube : postFpTubes) {
            BigDecimal volume = mapBarcodeToVolume.get(postFpTube);
            volume = volume.subtract(new BigDecimal("5"));
            mapBarcodeToVolume.put(postFpTube, volume);
            sourceVolumes.add(volume);
        }
        buildFingerprintingPlateSetup(fpRackBarcode, postFpTubes, sourceVolumes, fpPlate);

        // ShearingAliquot
        String shearingRackBarcode = "sr" + testPrefix;
        List<String> shearingTubes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= postFpTubes.size(); rackPosition++) {
            shearingTubes.add("sat" + testPrefix + rackPosition);
        }
        sourceVolumes.clear();
        for (String postFpTube : postFpTubes) {
            BigDecimal volume = mapBarcodeToVolume.get(postFpTube);
            volume = volume.subtract(new BigDecimal("46"));
            mapBarcodeToVolume.put(postFpTube, volume);
            sourceVolumes.add(volume);
        }
        for (String shearingTube : shearingTubes) {
            BigDecimal volume = new BigDecimal("60");
            mapBarcodeToVolume.put(shearingTube, volume);
            targetVolumes.add(volume);
            targetConcentrations.add(new BigDecimal("3"));
        }

        buildShearingAliquot(rackBarcode, postFpTubes, sourceVolumes,
                shearingRackBarcode, shearingTubes, targetVolumes, targetConcentrations);

        // Shearing PicoTransfer
        sourceVolumes.clear();
        for (String tubeBarcode : shearingTubes) {
            BigDecimal volume = mapBarcodeToVolume.get(tubeBarcode);
            volume = volume.subtract(new BigDecimal("2"));
            mapBarcodeToVolume.put(tubeBarcode, volume);
            sourceVolumes.add(volume);
        }
        String shearingPico1 = "55" + testPrefix;
        String shearingPico2 = "66" + testPrefix;
        buildPicoTransfer(shearingRackBarcode, shearingTubes, sourceVolumes, shearingPico1, shearingPico2);
        shearingPicoTransfer1 = picoTransfer1;
        shearingPicoTransfer2 = picoTransfer2;

        // Shearing PicoBufferAddition
        buildPicoBufferAddition(shearingPico1, shearingPico2);
        shearingPicoBufferAddition1 = picoBufferAddition1;
        shearingPicoBufferAddition2 = picoBufferAddition2;

        return this;
    }

    /**
     * Builds an InitialTare message.  Called from this class and from GPUI tests.
     */
    public BettaLIMSMessage buildInitialTare(String rackBarcode, List<String> tubeBarcodes, List<BigDecimal> weights) {
        initialTareJaxb = bettaLimsMessageTestFactory.buildRackEvent("InitialTare", rackBarcode, tubeBarcodes);
        int i = 0;
        for (ReceptacleType receptacleType : initialTareJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setReceptacleWeight(weights.get(i));
            i++;
        }
        return bettaLimsMessageTestFactory.addMessage(messageList, initialTareJaxb);
    }

    public BettaLIMSMessage buildWeightMeasurement(String rackBarcode, List<String> tubeBarcodes,
            List<BigDecimal> volumes) {
        weightMeasurementJaxb = bettaLimsMessageTestFactory.buildRackEvent("WeightMeasurement",
                rackBarcode, tubeBarcodes);
        int i = 0;
        for (ReceptacleType receptacleType : weightMeasurementJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(volumes.get(i));
            i++;
        }
        return bettaLimsMessageTestFactory.addMessage(messageList, weightMeasurementJaxb);
    }

    public BettaLIMSMessage buildVolumeAddition(String rackBarcode, List<String> tubeBarcodes, List<BigDecimal> volumes) {
        volumeAdditionJaxb = bettaLimsMessageTestFactory.buildRackEvent("VolumeAddition", rackBarcode,
                tubeBarcodes);
        int i = 0;
        for (ReceptacleType receptacleType : volumeAdditionJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(volumes.get(i));
            i++;
        }
        return bettaLimsMessageTestFactory.addMessage(messageList, volumeAdditionJaxb);
    }

    public BettaLIMSMessage buildPicoTransfer(String rackBarcode, List<String> tubeBarcodes, List<BigDecimal> volumes,
            String initialPico1, String initialPico2) {
        picoTransfer1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoTransfer", rackBarcode,
                tubeBarcodes, initialPico1);
        // Pico plates need receptacles for expected lab metrics
        bettaLimsMessageTestFactory.createTargetPlateReceptacles(picoTransfer1, new BigDecimal(2));
        int i = 0;
        for (ReceptacleType receptacleType : picoTransfer1.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume(volumes.get(i));
            i++;
        }
        picoTransfer2 = bettaLimsMessageTestFactory.buildRackToPlate("PicoTransfer", rackBarcode,
                tubeBarcodes, initialPico2);
        bettaLimsMessageTestFactory.createTargetPlateReceptacles(picoTransfer2, new BigDecimal(2));
        i = 0;
        for (ReceptacleType receptacleType : picoTransfer2.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume(volumes.get(i));
            i++;
        }
        picoTransfer2.setDisambiguator(2L);
        return bettaLimsMessageTestFactory.addMessage(messageList, picoTransfer1, picoTransfer2);
    }

    public BettaLIMSMessage buildPicoBufferAddition(String initialPico1, String initialPico2) {
        picoBufferAddition1 = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", initialPico1);
        picoBufferAddition2 = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", initialPico2);
        picoBufferAddition2.setDisambiguator(2L);
        return bettaLimsMessageTestFactory.addMessage(messageList, picoBufferAddition1, picoBufferAddition2);
    }

    public BettaLIMSMessage buildFingerprintingAliquot(
            String rackBarcode, List<String> fpSourceTubes, List<BigDecimal> sourceVolumes,
            String fpRackBarcode, List<String> fpTargetTubes, List<BigDecimal> targetVolumes,
            List<BigDecimal> targetConcentrations) {
        int i;
        fingerprintingAliquotJaxb = bettaLimsMessageTestFactory.buildRackToRack(
                "FingerprintingAliquot", rackBarcode, fpSourceTubes, fpRackBarcode, fpTargetTubes);
        i = 0;
        for (ReceptacleType receptacleType : fingerprintingAliquotJaxb.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume(sourceVolumes.get(i));
            i++;
        }
        i = 0;
        for (ReceptacleType receptacleType : fingerprintingAliquotJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(targetVolumes.get(i));
            receptacleType.setConcentration(targetConcentrations.get(i));
            i++;
        }
        return bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingAliquotJaxb);
    }

    public BettaLIMSMessage buildFingerprintingPlateSetup(String fpRackBarcode, List<String> postFpTubes,
            List<BigDecimal> sourceVolumes, String fpPlate) {
        fingerprintingPlateSetup = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup",
                fpRackBarcode, postFpTubes, fpPlate);
        int i = 0;
        for (ReceptacleType receptacleType : fingerprintingPlateSetup.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume(sourceVolumes.get(i));
            i++;
        }
        MetadataType metadataType = new MetadataType();
        metadataType.setName("Volume");
        metadataType.setValue("2.5");
        fingerprintingPlateSetup.getMetadata().add(metadataType);
        return bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingPlateSetup);
    }

    public BettaLIMSMessage buildShearingAliquot(
            String rackBarcode, List<String> postFpTubes, List<BigDecimal> sourceVolumes,
            String shearingRackBarcode, List<String> shearingTubes, List<BigDecimal> targetVolumes,
            List<BigDecimal> targetConcentrations) {
        shearingAliquot = bettaLimsMessageTestFactory.buildRackToRack("ShearingAliquot",
                rackBarcode, postFpTubes, shearingRackBarcode, shearingTubes);
        int i = 0;
        for (ReceptacleType receptacleType : shearingAliquot.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume(sourceVolumes.get(i));
            i++;
        }
        i = 0;
        for (ReceptacleType receptacleType : shearingAliquot.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(targetVolumes.get(i));
            receptacleType.setConcentration(targetConcentrations.get(i));
            i++;
        }
        return bettaLimsMessageTestFactory.addMessage(messageList, shearingAliquot);
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

    public PlateTransferEventType getFingerprintingPicoTransfer1() {
        return fingerprintingPicoTransfer1;
    }

    public PlateTransferEventType getFingerprintingPicoTransfer2() {
        return fingerprintingPicoTransfer2;
    }

    public PlateEventType getFingerprintingPicoBufferAddition1() {
        return fingerprintingPicoBufferAddition1;
    }

    public PlateEventType getFingerprintingPicoBufferAddition2() {
        return fingerprintingPicoBufferAddition2;
    }

    public PlateTransferEventType getFingerprintingPlateSetup() {
        return fingerprintingPlateSetup;
    }

    public PlateTransferEventType getShearingAliquot() {
        return shearingAliquot;
    }

    public PlateTransferEventType getShearingPicoTransfer1() {
        return shearingPicoTransfer1;
    }

    public PlateTransferEventType getShearingPicoTransfer2() {
        return shearingPicoTransfer2;
    }

    public PlateEventType getShearingPicoBufferAddition1() {
        return shearingPicoBufferAddition1;
    }

    public PlateEventType getShearingPicoBufferAddition2() {
        return shearingPicoBufferAddition2;
    }
}
