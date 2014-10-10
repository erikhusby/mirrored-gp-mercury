package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.util.List;

/**
 * Builds JAXB BettaLIMS DTOs to test messaging for CRSP Pico.
 */
public class CrspPicoJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String rackBarcode;
    private final List<String> tubeBarcodes;

    public CrspPicoJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix, String rackBarcode,
            List<String> tubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
        this.tubeBarcodes = tubeBarcodes;
    }

    public CrspPicoJaxbBuilder invoke() {
        // todo jmt why don't InitialTare, WeightMeasurement and VolumeAddition appear in User Defined Search?

        // Need to test a variety of next steps: FP Daughter, Shearing Daughter, Exclude?
        // Set Pico upload values round-robin to achieve desired outcomes.
        // Re-array based on next steps.

        // InitialTare
        PlateEventType initialTare = bettaLimsMessageTestFactory.buildRackEvent("InitialTare", rackBarcode,
                tubeBarcodes);
        for (ReceptacleType receptacleType : initialTare.getPositionMap().getReceptacle()) {
            receptacleType.setReceptacleWeight();
        }

        // WeightMeasurement
        PlateEventType weightMeasurement = bettaLimsMessageTestFactory.buildRackEvent("WeightMeasurement", rackBarcode,
                tubeBarcodes);
        for (ReceptacleType receptacleType : weightMeasurement.getPositionMap().getReceptacle()) {
            receptacleType.setVolume();
        }

        // VolumeAddition
        PlateEventType volumeAddition = bettaLimsMessageTestFactory.buildRackEvent("VolumeAddition", rackBarcode,
                tubeBarcodes);
        for (ReceptacleType receptacleType : volumeAddition.getPositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration(); // todo jmt how does the XL20 know the concentration?
        }

/*
        // Initial PicoTransfer
        PlateTransferEventType picoTransfer = bettaLimsMessageTestFactory
                .buildRackToPlate("PicoTransfer", rackBarcode, tubeBarcodes, "11" + testPrefix);
        for (ReceptacleType receptacleType : picoTransfer.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration();
        }

        bettaLimsMessageTestFactory.buildRackToPlate("PicoTransfer", rackBarcode, tubeBarcodes, "22" + testPrefix);
        // Initial PicoBufferAddition
        bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", ); // disambiguator 3, 4
*/

        // FingerprintingAliquot
        PlateTransferEventType fingerprintingAliquot = bettaLimsMessageTestFactory.buildRackToRack(
                "FingerprintingAliquot", rackBarcode, tubeBarcodes, , );
        for (ReceptacleType receptacleType : fingerprintingAliquot.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration();
        }
        for (ReceptacleType receptacleType : fingerprintingAliquot.getPositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration();
        }

        // FP PicoTransfer
        // FP PicoBufferAddition

        // FingerprintingPlateSetup
        PlateTransferEventType fingerprintingPlateSetup =
                bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup", , ,);
        MetadataType metadataType = new MetadataType();
        metadataType.setName("Volume");
        metadataType.setValue("2.5");
        fingerprintingPlateSetup.getMetadata().add(metadataType);
        // todo jmt should set source volume?

        // ShearingAliquot
        PlateTransferEventType shearingAliquot = bettaLimsMessageTestFactory.buildRackToRack("ShearingAliquot", , , , );
        for (ReceptacleType receptacleType : shearingAliquot.getSourcePositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration();
        }
        for (ReceptacleType receptacleType : shearingAliquot.getPositionMap().getReceptacle()) {
            receptacleType.setVolume();
            receptacleType.setConcentration();
        }

        // Shearing PicoTransfer
        // Shearing PicoBufferAddition

        return this;
    }
}
