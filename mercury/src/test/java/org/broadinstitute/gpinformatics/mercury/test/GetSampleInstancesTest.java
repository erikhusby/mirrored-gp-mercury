package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test LabVessel.getSampleInstances
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class GetSampleInstancesTest {

    public static final String SAMPLE_KIT_1 = "SK-1";
    public static final String LCSET_1 = "LCSET-1";
    public static final String LCSET_2 = "LCSET-2";

    /** date for LabEVents */
    private long now = System.currentTimeMillis();
    private StaticPlate indexPlateP7;
    private StaticPlate indexPlateP5;
    private TwoDBarcodedTube baitTube;

    /**
     * Moves 3 samples through initiation and extraction; makes an LCSET with tubes 1 and 2; reworks tube 2 in
     * another LCSET with tube3; adds a control to each LC rack; pools the 2 LCSETs into one tube.
     */
    @Test
    public void testRework() {
        // Reagents
        // Molecular indexes
        String p7IndexPlateBarcode = "P7";
        String p5IndexPlateBarcode = "P5";
        List<StaticPlate> indexPlates = LabEventTest.buildIndexPlate(null, null,
                Arrays.asList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7,
                        MolecularIndexingScheme.IndexPosition.ILLUMINA_P5),
                Arrays.asList(p7IndexPlateBarcode, p5IndexPlateBarcode));
        indexPlateP7 = indexPlates.get(0);
        indexPlateP5 = indexPlates.get(1);

        // Bait
        String baitTubeBarcode = "BAIT";
        baitTube = LabEventTest.buildBaitTube(baitTubeBarcode, null);

        // sample initiation PDO
        ProductOrder sampleInitProductOrder = ProductOrderTestFactory.createDummyProductOrder(3, "PDO-SI",
                Workflow.ICE, 101L, "Test research project", "Test research project", false, "SamInit", "1",
                "ExExQuoteId");
        String rootSample1 = sampleInitProductOrder.getSamples().get(0).getSampleKey();
        String rootSample2 = sampleInitProductOrder.getSamples().get(1).getSampleKey();
        String rootSample3 = sampleInitProductOrder.getSamples().get(2).getSampleKey();

        // receive samples
        Set<LabVessel> receivedVessels = new LinkedHashSet<>();
        int i = 1;
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            TwoDBarcodedTube tube = new TwoDBarcodedTube("R" + i);
            MercurySample mercurySample = new MercurySample(productOrderSample.getSampleKey());
            mercurySample.addProductOrderSample(productOrderSample);
            tube.addSample(mercurySample);
            receivedVessels.add(tube);
            i++;
        }
        LabBatch receiptBatch = new LabBatch(SAMPLE_KIT_1, receivedVessels, LabBatch.LabBatchType.SAMPLES_RECEIPT);
        receiptBatch.setCreatedOn(new Date(now++));

        // extraction PDO
        Product extractionProduct = ProductTestFactory.createDummyProduct(Workflow.ICE, "EXTR-01");
        List<ProductOrderSample> extractionProductOrderSamples = new ArrayList<>();
        for (ProductOrderSample initProductOrderSample : sampleInitProductOrder.getSamples()) {
            ProductOrderSample extractProductOrderSample = new ProductOrderSample(initProductOrderSample.getSampleKey());
            initProductOrderSample.getMercurySample().addProductOrderSample(extractProductOrderSample);
            extractionProductOrderSamples.add(extractProductOrderSample);
        }
        ProductOrder extractionProductOrder = new ProductOrder(101L, "Extraction PDO", extractionProductOrderSamples,
                "SEQ-01", extractionProduct, sampleInitProductOrder.getResearchProject());
        // todo extraction set?

        // Extraction transfer
        LabEvent extractionTransfer = new LabEvent(LabEventType.SAMPLES_EXTRACTION_END_TRANSFER, new Date(now++), "HULK",
                1L, 101L, "Bravo");
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToSourceTube = new EnumMap<>(VesselPosition.class);
        Iterator<LabVessel> iterator = receivedVessels.iterator();
        mapPositionToSourceTube.put(VesselPosition.A01, (TwoDBarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A02, (TwoDBarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A03, (TwoDBarcodedTube) iterator.next());
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToSourceTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTube = new EnumMap<>(VesselPosition.class);
        TwoDBarcodedTube tube1 = new TwoDBarcodedTube("X1");
        tube1.addSample(new MercurySample("SM-X1"));
        mapPositionToExtractTube.put(VesselPosition.A01, tube1);

        TwoDBarcodedTube tube2 = new TwoDBarcodedTube("X2");
        tube2.addSample(new MercurySample("SM-X2"));
        mapPositionToExtractTube.put(VesselPosition.A02, tube2);

        TwoDBarcodedTube tube3 = new TwoDBarcodedTube("X3");
        tube3.addSample(new MercurySample("SM-X3"));
        mapPositionToExtractTube.put(VesselPosition.A03, tube3);

        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToExtractTube, RackOfTubes.RackType.Matrix96);
        extractionTransfer.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.ALL96,
                targetTubeFormation.getContainerRole(), SBSSection.ALL96, extractionTransfer));

        // sequencing PDO
        Product sequencingProduct = ProductTestFactory.createDummyProduct(Workflow.HYBRID_SELECTION, "HYBSEL-01");
        List<ProductOrderSample> sequencingProductOrderSamples = new ArrayList<>();
        for (TwoDBarcodedTube twoDBarcodedTube : mapPositionToExtractTube.values()) {
            MercurySample mercurySample = twoDBarcodedTube.getMercurySamples().iterator().next();
            ProductOrderSample sequencingProductOrderSample = new ProductOrderSample(mercurySample.getSampleKey());
            mercurySample.addProductOrderSample(sequencingProductOrderSample);
            sequencingProductOrderSamples.add(sequencingProductOrderSample);
        }
        ProductOrder sequencingProductOrder = new ProductOrder(101L, "Sequencing PDO", sequencingProductOrderSamples,
                "SEQ-01", sequencingProduct, sampleInitProductOrder.getResearchProject());

        StaticPlate shearingPlate1 = sequencing(tube1, tube2, sampleInitProductOrder, extractionProductOrder,
                sequencingProductOrder, rootSample1, false, 1);

        // Clear cached bucket entry.
        tube2.clearCaches();
        StaticPlate shearingPlate2 = sequencing(tube2, tube3, sampleInitProductOrder, extractionProductOrder,
                sequencingProductOrder, rootSample2, true, 2);

        // PoolingTransfer
        LabEvent poolingTransfer = new LabEvent(LabEventType.POOLING_TRANSFER, new Date(now++), "ROBIN", 1L, 101L,
                "Janus");
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToPoolTube = new EnumMap<>(VesselPosition.class);
        TwoDBarcodedTube poolTube = new TwoDBarcodedTube("POOL" + now);
        mapPositionToPoolTube.put(VesselPosition.A01, poolTube);
        TubeFormation poolTubeFormation = new TubeFormation(mapPositionToPoolTube, RackOfTubes.RackType.Matrix96);
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A01,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A02,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A03,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate2.getContainerRole(), VesselPosition.A04,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate2.getContainerRole(), VesselPosition.A05,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, poolingTransfer));

        List<SampleInstanceV2> poolSampleInstances = poolTube.getSampleInstancesV2();
        Assert.assertEquals(poolSampleInstances.size(), 5);
        int matchedSamples = 0;
        for (SampleInstanceV2 poolSampleInstance : poolSampleInstances) {
            String s = poolSampleInstance.getMercuryRootSampleName();
            if (s.equals(rootSample1)) {
                matchedSamples++;
                //noinspection ConstantConditions
                Assert.assertEquals(poolSampleInstance.getSingleBucketEntry().getLabBatch().getBatchName(), LCSET_1);
            } else if (s.equals(rootSample2)) {
                MolecularIndexingScheme molecularIndexingScheme =
                        ((MolecularIndexReagent) poolSampleInstance.getReagents().get(0)).getMolecularIndexingScheme();
                switch (molecularIndexingScheme.getName()) {
                case "Illumina_P5-U_P7-U":
                    Assert.assertEquals(poolSampleInstance.getSingleInferredBucketedBatch().getBatchName(), LCSET_1);
                    matchedSamples++;
                    break;
                case "Illumina_P5-C_P7-C":
                    //noinspection ConstantConditions
                    Assert.assertEquals(poolSampleInstance.getSingleBucketEntry().getLabBatch().getBatchName(),
                            LCSET_2);
                    matchedSamples++;
                    break;
                default:
                    Assert.fail("Unexpected scheme name " + molecularIndexingScheme.getName());
                }
            } else if (s.equals("SM-C1")) {
                matchedSamples++;
                Assert.assertEquals(poolSampleInstance.getSingleInferredBucketedBatch().getBatchName(), LCSET_1);
            } else {
                if (s.equals(rootSample3)) {
                    matchedSamples++;
                    //noinspection ConstantConditions
                    Assert.assertEquals(poolSampleInstance.getSingleBucketEntry().getLabBatch().getBatchName(), LCSET_2);
                } else {
                    Assert.fail("Unexpected sample ID " + poolSampleInstance.getMercuryRootSampleName());
                }
            }
        }
        Assert.assertEquals(matchedSamples, 5);

        BaseEventTest.runTransferVisualizer(poolTube);
    }

    /**
     * Moves tubes through (simplified) library construction transfers.
     */
    private StaticPlate sequencing(TwoDBarcodedTube tube1, TwoDBarcodedTube tube2,
            final ProductOrder sampleInitProductOrder, final ProductOrder extractionProductOrder,
            final ProductOrder sequencingProductOrder, String tube1RootSample, boolean tube1Rework, int lcsetNum) {
        // LCSET
        Set<LabVessel> extractedVessels = new HashSet<>();
        extractedVessels.add(tube1);
        extractedVessels.add(tube2);
        LabBatch lcsetBatch = new LabBatch("LCSET-" + lcsetNum, extractedVessels, LabBatch.LabBatchType.WORKFLOW);
        lcsetBatch.setCreatedOn(new Date(now++));
        lcsetBatch.setWorkflowName("Exome Express");
        BucketEntry bucketEntry1 = new BucketEntry(tube1, sequencingProductOrder.getBusinessKey(), new Bucket("Shearing"),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry1.setLabBatch(lcsetBatch);
        tube1.addBucketEntry(bucketEntry1);

        BucketEntry bucketEntry2 = new BucketEntry(tube2, sequencingProductOrder.getBusinessKey(), new Bucket("Shearing"),
                BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry2.setLabBatch(lcsetBatch);
        tube2.addBucketEntry(bucketEntry2);

        // re-array to add control
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTubeControl = new EnumMap<>(VesselPosition.class);

        // use different positions for each LCSET, to avoid molecular index collision.
        VesselPosition position1 = VesselPosition.values()[(lcsetNum - 1) * 3];
        VesselPosition position2 = VesselPosition.values()[((lcsetNum - 1) * 3) + 1];
        VesselPosition position3 = VesselPosition.values()[((lcsetNum - 1) * 3) + 2];

        // Avoid exporting the rework twice
        if (!tube1Rework) {
            mapPositionToExtractTubeControl.put(position1, tube1);
        }
        mapPositionToExtractTubeControl.put(position2, tube2);

        TwoDBarcodedTube controlTube = new TwoDBarcodedTube("CONTROL" + lcsetNum);
        controlTube.addSample(new MercurySample("SM-C" + lcsetNum));
        mapPositionToExtractTubeControl.put(position3, controlTube);

        // Import
        LabBatch importLabBatch = new LabBatch("EX-" + lcsetNum, new HashSet<LabVessel>(mapPositionToExtractTubeControl.values()),
                LabBatch.LabBatchType.SAMPLES_IMPORT);
        importLabBatch.setCreatedOn(new Date(now++));
        // Add the rework after the import
        if (tube1Rework) {
            mapPositionToExtractTubeControl.put(position1, tube1);
        }
        TubeFormation extractControlTubeFormation = new TubeFormation(mapPositionToExtractTubeControl,
                RackOfTubes.RackType.Matrix96);
        LabEvent shearingTransfer = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(now++), "SUPERMAN", 1L, 101L,
                "Bravo");
        StaticPlate shearingPlate = new StaticPlate("SHEAR" + lcsetNum, StaticPlate.PlateType.Eppendorf96);
        shearingTransfer.getSectionTransfers().add(new SectionTransfer(
                extractControlTubeFormation.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, shearingTransfer));

        // P7 index transfer
        LabEvent p7IndexTransfer = new LabEvent(LabEventType.INDEXED_ADAPTER_LIGATION, new Date(now++), "SPIDERMAN", 1L,
                101L, "Bravo");
        p7IndexTransfer.getSectionTransfers().add(new SectionTransfer(indexPlateP7.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, p7IndexTransfer));

        // P5 index transfer
        LabEvent p5IndexTransfer = new LabEvent(LabEventType.INDEX_P5_POND_ENRICHMENT, new Date(now++), "TORCH", 1L,
                101L, "Bravo");
        p5IndexTransfer.getSectionTransfers().add(new SectionTransfer(indexPlateP5.getContainerRole(), SBSSection.ALL96,
                shearingPlate.getContainerRole(), SBSSection.ALL96, p5IndexTransfer));

        // bait transfers
        StaticPlate baitPlate = new StaticPlate("BAITPLATE" + lcsetNum, StaticPlate.PlateType.Eppendorf96);
        LabEvent baitSetupTransfer = new LabEvent(LabEventType.BAIT_SETUP, new Date(now++), "TICK", 1L, 101L, "Bravo");
        baitSetupTransfer.getVesselToSectionTransfers().add(new VesselToSectionTransfer(baitTube, SBSSection.ALL96,
                baitPlate.getContainerRole(), baitSetupTransfer));

        LabEvent baitAdditionTransfer = new LabEvent(LabEventType.BAIT_ADDITION, new Date(now++), "BATMAN", 1L, 101L,
                "Bravo");
        baitAdditionTransfer.getSectionTransfers().add(new SectionTransfer(baitPlate.getContainerRole(),
                SBSSection.ALL96, shearingPlate.getContainerRole(), SBSSection.ALL96, baitAdditionTransfer));

        // Verify 1st sample
        List<SampleInstanceV2> sampleInstances =
                shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(position1);
        Assert.assertEquals(sampleInstances.size(), 1);
        SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
        Assert.assertEquals(sampleInstance.getMercuryRootSampleName(), tube1RootSample);
        verifyReagents(sampleInstance, lcsetNum == 1 ? "Illumina_P5-M_P7-M" : "Illumina_P5-C_P7-C");
        Assert.assertEquals(sampleInstance.getSingleBucketEntry(), bucketEntry1);
        List<LabBatchStartingVessel> importBatchVessels = new ArrayList<>();
        if (!tube1Rework) {
            for (LabBatchStartingVessel labBatchStartingVessel : importLabBatch.getLabBatchStartingVessels()) {
                if (labBatchStartingVessel.getLabVessel().getLabel().equals(tube1.getLabel())) {
                    importBatchVessels.add(labBatchStartingVessel);
                    break;
                }
            }
            Assert.assertEquals(sampleInstance.getAllBatchVessels(LabBatch.LabBatchType.SAMPLES_IMPORT),
                    importBatchVessels);
        }
        Assert.assertEquals(sampleInstance.getSingleBatchVessel(LabBatch.LabBatchType.WORKFLOW).getLabBatch().getBatchName(),
                "LCSET-" + lcsetNum);
        List<LabBatchStartingVessel> allBatchVessels = sampleInstance.getAllBatchVessels();
        int index = 0;
        if (tube1Rework) {
            Assert.assertEquals(allBatchVessels.get(index++).getLabBatch().getBatchName(), LCSET_2);
        }
        Assert.assertEquals(allBatchVessels.get(index++).getLabBatch().getBatchName(), "EX-1");
        Assert.assertEquals(allBatchVessels.get(index++).getLabBatch().getBatchName(), LCSET_1);
        Assert.assertEquals(allBatchVessels.get(index).getLabBatch().getBatchName(), SAMPLE_KIT_1);
        Assert.assertEquals(sampleInstance.getWorkflowName(), "Exome Express");

        Assert.assertEquals(sampleInstance.getAllBucketEntries().size(), lcsetNum);
        final int sampleIndex = lcsetNum - 1;
        HashSet<ProductOrderSample> expected = new HashSet<ProductOrderSample>() {{
            add(sampleInitProductOrder.getSamples().get(sampleIndex));
            add(extractionProductOrder.getSamples().get(sampleIndex));
            add(sequencingProductOrder.getSamples().get(sampleIndex));
        }};
        Assert.assertEquals(new HashSet<>(sampleInstance.getAllProductOrderSamples()), expected);
        Assert.assertEquals(sampleInstance.getSingleProductOrderSample(),
                sequencingProductOrder.getSamples().get(sampleIndex));

        // Verify control
        sampleInstances = shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(position3);
        Assert.assertEquals(sampleInstances.size(), 1);
        sampleInstance = sampleInstances.iterator().next();
        verifyReagents(sampleInstance, lcsetNum == 1 ? "Illumina_P5-V_P7-V" : "Illumina_P5-X_P7-X");
        Assert.assertNull(sampleInstance.getSingleBucketEntry());
        Assert.assertEquals(sampleInstance.getSingleInferredBucketedBatch(), lcsetBatch);

        return shearingPlate;
    }

    private void verifyReagents(SampleInstanceV2 sampleInstance, String expectedMolIndScheme) {
        Assert.assertEquals(sampleInstance.getReagents().size(), 2);
        MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) sampleInstance.getReagents().get(0);
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), expectedMolIndScheme);
        DesignedReagent designedReagent = (DesignedReagent) sampleInstance.getReagents().get(1);
        Assert.assertEquals(designedReagent.getReagentDesign().getDesignName(), "cancer_2000gene_shift170_undercovered");
    }
}
