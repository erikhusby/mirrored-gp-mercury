package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test LabVessel.getSampleInstances
 */
public class GetSampleInstancesTest {
    @Test
    public void testBasic() {
        // sample initiation PDO
        ProductOrder sampleInitProductOrder = ProductOrderTestFactory.createDummyProductOrder(2, "PDO-SI",
                Workflow.ICE, 101L, "Test research project", "Test research project", false, "SamInit", "1");

        // receive samples
        Set<LabVessel> receivedVessels = new HashSet<>();
        int i = 1;
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            TwoDBarcodedTube tube = new TwoDBarcodedTube("R" + i);
            tube.addSample(new MercurySample(productOrderSample.getSampleKey()));
            receivedVessels.add(tube);
            i++;
        }
        LabBatch receiptBatch = new LabBatch("SK-1", receivedVessels, LabBatch.LabBatchType.SAMPLES_RECEIPT);

        // extraction PDO
        Product extractionProduct = ProductTestFactory.createDummyProduct(Workflow.ICE, "EXTR-01");
        List<ProductOrderSample> extractionProductOrderSamples = new ArrayList<>();
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            extractionProductOrderSamples.add(new ProductOrderSample(productOrderSample.getSampleKey()));
        }
        ProductOrder extractionProductOrder = new ProductOrder(101L, "Extraction PDO", extractionProductOrderSamples,
                "SEQ-01", extractionProduct, sampleInitProductOrder.getResearchProject());
        // todo extraction set?

        // Extraction transfer
        LabEvent extractionTransfer = new LabEvent(LabEventType.SAMPLES_EXTRACTION_END_TRANSFER, new Date(), "HULK",
                1L, 101L, "Bravo");
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToSourceTube = new HashMap<>();
        Iterator<LabVessel> iterator = receivedVessels.iterator();
        mapPositionToSourceTube.put(VesselPosition.A01, (TwoDBarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A02, (TwoDBarcodedTube) iterator.next());
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToSourceTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTube = new HashMap<>();
        TwoDBarcodedTube tube1 = new TwoDBarcodedTube("X1");
        tube1.addSample(new MercurySample("SM-X1"));
        mapPositionToExtractTube.put(VesselPosition.A01, tube1);

        TwoDBarcodedTube tube2 = new TwoDBarcodedTube("X2");
        tube2.addSample(new MercurySample("SM-X2"));
        mapPositionToExtractTube.put(VesselPosition.A02, tube2);
        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToExtractTube, RackOfTubes.RackType.Matrix96);

        extractionTransfer.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.ALL96,
                targetTubeFormation.getContainerRole(), SBSSection.ALL96, extractionTransfer));

        // sequencing PDO
        Product sequencingProduct = ProductTestFactory.createDummyProduct(Workflow.HYBRID_SELECTION, "HYBSEL-01");
        List<ProductOrderSample> sequencingProductOrderSamples = new ArrayList<>();
        for (TwoDBarcodedTube twoDBarcodedTube : mapPositionToExtractTube.values()) {
            sequencingProductOrderSamples.add(
                    new ProductOrderSample(twoDBarcodedTube.getMercurySamples().iterator().next().getSampleKey()));
        }
        ProductOrder sequencingProductOrder = new ProductOrder(101L, "Sequencing PDO", sequencingProductOrderSamples,
                "SEQ-01", sequencingProduct, sampleInitProductOrder.getResearchProject());

        // LCSET
        Set<LabVessel> extractedVessels = new HashSet<>();
        extractedVessels.add(tube1);
        extractedVessels.add(tube2);
        LabBatch lcsetBatch = new LabBatch("LCSET-1", extractedVessels, LabBatch.LabBatchType.WORKFLOW);
        BucketEntry bucketEntry = new BucketEntry(tube1, sequencingProductOrder.getBusinessKey(), new Bucket("Shearing"),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry.setLabBatch(lcsetBatch);
        tube1.addBucketEntry(bucketEntry);

        // re-array to add control
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTubeControl = new HashMap<>(mapPositionToExtractTube);

        TwoDBarcodedTube controlTube = new TwoDBarcodedTube("CONTROL");
        controlTube.addSample(new MercurySample("SM-C"));
        mapPositionToExtractTubeControl.put(VesselPosition.A03, controlTube);

        // Import
        LabBatch importLabBatch = new LabBatch("EX-1", new HashSet<LabVessel>(mapPositionToExtractTubeControl.values()),
                LabBatch.LabBatchType.SAMPLES_IMPORT);

        TubeFormation extractControlTubeFormation = new TubeFormation(mapPositionToExtractTubeControl,
                RackOfTubes.RackType.Matrix96);
        LabEvent shearingTransfer = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(), "SUPERMAN", 1L, 101L,
                "Bravo");
        StaticPlate shearingPlate = new StaticPlate("SHEAR", StaticPlate.PlateType.Eppendorf96);
        shearingTransfer.getSectionTransfers().add(new SectionTransfer(
                extractControlTubeFormation.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, shearingTransfer));

        final String p7IndexPlateBarcode = "P7";
        final String p5IndexPlateBarcode = "P5";
        List<StaticPlate> indexPlates = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
                }},
                new ArrayList<String>() {{
                    add(p7IndexPlateBarcode);
                    add(p5IndexPlateBarcode);
                }}
        );
        StaticPlate indexPlateP7 = indexPlates.get(0);
        StaticPlate indexPlateP5 = indexPlates.get(1);

        // P7 index transfer
        LabEvent p7IndexTransfer = new LabEvent(LabEventType.INDEXED_ADAPTER_LIGATION, new Date(), "SPIDERMAN", 1L,
                101L, "Bravo");
        p7IndexTransfer.getSectionTransfers().add(new SectionTransfer(indexPlateP7.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, p7IndexTransfer));

        // P5 index transfer
        LabEvent p5IndexTransfer = new LabEvent(LabEventType.INDEX_P5_POND_ENRICHMENT, new Date(), "TORCH", 1L,
                101L, "Bravo");
        p5IndexTransfer.getSectionTransfers().add(new SectionTransfer(indexPlateP5.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, p5IndexTransfer));

        // bait transfers
        String baitTubeBarcode = "BAIT";
        TwoDBarcodedTube baitTube = LabEventTest.buildBaitTube(baitTubeBarcode, null);

        StaticPlate baitPlate = new StaticPlate("BAITPLATE", StaticPlate.PlateType.Eppendorf96);
        LabEvent baitSetupTransfer = new LabEvent(LabEventType.BAIT_SETUP, new Date(), "TICK", 1L, 101L, "Bravo");
        baitSetupTransfer.getVesselToSectionTransfers().add(new VesselToSectionTransfer(baitTube, SBSSection.ALL96,
                baitPlate.getContainerRole(), baitSetupTransfer));

        LabEvent baitAdditionTransfer = new LabEvent(LabEventType.BAIT_ADDITION, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        baitAdditionTransfer.getSectionTransfers().add(new SectionTransfer(baitPlate.getContainerRole(),
                SBSSection.ALL96, shearingPlate.getContainerRole(), SBSSection.ALL96, baitAdditionTransfer));

        Set<SampleInstanceV2> sampleInstances =
                shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstances.size(), 1);
    }
}
