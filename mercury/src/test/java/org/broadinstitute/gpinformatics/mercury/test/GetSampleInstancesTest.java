package org.broadinstitute.gpinformatics.mercury.test;

import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
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
    private BarcodedTube baitTube;

    private SampleInstanceV2 createSampleInstanceForPipelineAPIMetadataTesting(MercurySample.MetadataSource metadataSource,
                                                                        String sampleName) {
        LabVessel tube = new BarcodedTube("barcde");
        MercurySample bspSample = new MercurySample(sampleName, metadataSource);
        tube.addSample(bspSample);
        return new SampleInstanceV2(tube);

    }

    private SampleInstanceV2 createSampleInstanceForPipelineAPIAggregationParticleTesting(String pdo, String sample,
                                                                                          String pdoParticleString,
                                                                                          Product.AggregationParticle aggregationParticle) {
        SampleInstanceV2 sampleInstanceV2 =
            createSampleInstanceForPipelineAPIMetadataTesting(MercurySample.MetadataSource.BSP, sample);

        LabVessel labVessel = sampleInstanceV2.getInitialLabVessel();
        MercurySample bspSample = labVessel.getMercurySamples().iterator().next();

        ProductOrder dummyProductOrder = ProductOrderTestFactory.createDummyProductOrder(0, pdo);
        dummyProductOrder.setDefaultAggregationParticle(aggregationParticle);

        if (sample != null) {
            ProductOrderSample pdoSample = new ProductOrderSample(sample);
            if (StringUtils.isNotBlank(pdoParticleString)) {
                pdoSample.setAggregationParticle(pdoParticleString);
            }
            bspSample.addProductOrderSample(pdoSample);
            dummyProductOrder.addSample(pdoSample);
        }

        BucketEntry bucketEntry = new BucketEntry(labVessel, dummyProductOrder, new Bucket("foo"), BucketEntry.BucketEntryType.PDO_ENTRY);
        labVessel.addBucketEntry(bucketEntry);
             bucketEntry.setLabBatch(new LabBatch("batch", Collections.singleton(labVessel), LabBatch.LabBatchType.WORKFLOW));

        return new SampleInstanceV2(labVessel);
    }

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
            BarcodedTube tube = new BarcodedTube("R" + i);
            MercurySample mercurySample = new MercurySample(productOrderSample.getSampleKey(),
                    MercurySample.MetadataSource.BSP);
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
        Map<VesselPosition, BarcodedTube> mapPositionToSourceTube = new EnumMap<>(VesselPosition.class);
        Iterator<LabVessel> iterator = receivedVessels.iterator();
        mapPositionToSourceTube.put(VesselPosition.A01, (BarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A02, (BarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A03, (BarcodedTube) iterator.next());
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToSourceTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, BarcodedTube> mapPositionToExtractTube = new EnumMap<>(VesselPosition.class);
        BarcodedTube tube1 = new BarcodedTube("X1");
        tube1.addSample(new MercurySample("SM-X1", MercurySample.MetadataSource.BSP));
        mapPositionToExtractTube.put(VesselPosition.A01, tube1);

        BarcodedTube tube2 = new BarcodedTube("X2");
        tube2.addSample(new MercurySample("SM-X2", MercurySample.MetadataSource.BSP));
        mapPositionToExtractTube.put(VesselPosition.A02, tube2);

        BarcodedTube tube3 = new BarcodedTube("X3");
        tube3.addSample(new MercurySample("SM-X3", MercurySample.MetadataSource.BSP));
        mapPositionToExtractTube.put(VesselPosition.A03, tube3);

        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToExtractTube, RackOfTubes.RackType.Matrix96);
        extractionTransfer.getSectionTransfers().add(new SectionTransfer(
                sourceTubeFormation.getContainerRole(), SBSSection.ALL96, null,
                targetTubeFormation.getContainerRole(), SBSSection.ALL96, null, extractionTransfer));

        // sequencing PDO
        Product sequencingProduct = ProductTestFactory.createDummyProduct(Workflow.HYBRID_SELECTION, "HYBSEL-01");
        List<ProductOrderSample> sequencingProductOrderSamples = new ArrayList<>();
        for (BarcodedTube barcodedTube : mapPositionToExtractTube.values()) {
            MercurySample mercurySample = barcodedTube.getMercurySamples().iterator().next();
            ProductOrderSample sequencingProductOrderSample = new ProductOrderSample(mercurySample.getSampleKey());
            mercurySample.addProductOrderSample(sequencingProductOrderSample);
            sequencingProductOrderSamples.add(sequencingProductOrderSample);
        }
        ProductOrder sequencingProductOrder = new ProductOrder(101L, "Sequencing PDO", sequencingProductOrderSamples,
                "SEQ-01", sequencingProduct, sampleInitProductOrder.getResearchProject());

        StaticPlate shearingPlate1 = sequencing(tube1, tube2, sampleInitProductOrder, extractionProductOrder,
                sequencingProductOrder, rootSample1, false, 1);

        // Clear cached bucket entries and computed LCSETS from previous run.
        tube2.clearCaches();

        StaticPlate shearingPlate2 = sequencing(tube2, tube3, sampleInitProductOrder, extractionProductOrder,
                sequencingProductOrder, rootSample2, true, 2);

        // PoolingTransfer
        LabEvent poolingTransfer = new LabEvent(LabEventType.POOLING_TRANSFER, new Date(now++), "ROBIN", 1L, 101L,
                "Janus");
        Map<VesselPosition, BarcodedTube> mapPositionToPoolTube = new EnumMap<>(VesselPosition.class);
        BarcodedTube poolTube = new BarcodedTube("POOL" + now);
        mapPositionToPoolTube.put(VesselPosition.A01, poolTube);
        TubeFormation poolTubeFormation = new TubeFormation(mapPositionToPoolTube, RackOfTubes.RackType.Matrix96);
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A01, null,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, null, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A02, null,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, null, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate1.getContainerRole(), VesselPosition.A03, null,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, null, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate2.getContainerRole(), VesselPosition.A04, null,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, null, poolingTransfer));
        poolingTransfer.getCherryPickTransfers().add(new CherryPickTransfer(
                shearingPlate2.getContainerRole(), VesselPosition.A05, null,
                poolTubeFormation.getContainerRole(), VesselPosition.A01, null, poolingTransfer));

        Set<SampleInstanceV2> poolSampleInstances = poolTube.getSampleInstancesV2();
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
                case "Illumina_P5-Wabab_P7-Wabab":
                    Assert.assertEquals(poolSampleInstance.getSingleBatch().getBatchName(), LCSET_1);
                    matchedSamples++;
                    break;
                case "Illumina_P5-Cabab_P7-Cabab":
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
                Assert.assertEquals(poolSampleInstance.getSingleBatch().getBatchName(), LCSET_1);
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
    private StaticPlate sequencing(BarcodedTube tube1, BarcodedTube tube2,
            final ProductOrder sampleInitProductOrder, final ProductOrder extractionProductOrder,
            final ProductOrder sequencingProductOrder, String tube1RootSample, boolean tube1Rework, int lcsetNum) {
        // LCSET
        Set<LabVessel> extractedVessels = new HashSet<>();
        extractedVessels.add(tube1);
        extractedVessels.add(tube2);

        LabBatch lcsetBatch = new LabBatch("LCSET-" + lcsetNum, extractedVessels, LabBatch.LabBatchType.WORKFLOW);
        lcsetBatch.setCreatedOn(new Date(now++));
        String workflowName = "ICE Exome Express";
        lcsetBatch.setWorkflowName(workflowName);

        Bucket lcsetBucket = new Bucket("Shearing" + lcsetNum);
        BucketEntry bucketEntry1 = new BucketEntry(tube1, sequencingProductOrder, lcsetBucket,
                BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry1.setLabBatch(lcsetBatch);
        tube1.addBucketEntry(bucketEntry1);

        BucketEntry bucketEntry2 = new BucketEntry(tube2, sequencingProductOrder, lcsetBucket,
                BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry2.setLabBatch(lcsetBatch);
        tube2.addBucketEntry(bucketEntry2);

        // re-array to add control
        Map<VesselPosition, BarcodedTube> mapPositionToExtractTubeControl = new EnumMap<>(VesselPosition.class);

        // use different positions for each LCSET, to avoid molecular index collision.
        VesselPosition position1 = VesselPosition.values()[(lcsetNum - 1) * 3];
        VesselPosition position2 = VesselPosition.values()[((lcsetNum - 1) * 3) + 1];
        VesselPosition position3 = VesselPosition.values()[((lcsetNum - 1) * 3) + 2];

        // Avoid exporting the rework twice
        if (!tube1Rework) {
            mapPositionToExtractTubeControl.put(position1, tube1);
        }
        mapPositionToExtractTubeControl.put(position2, tube2);

        BarcodedTube controlTube = new BarcodedTube("CONTROL" + lcsetNum);
        controlTube.addSample(new MercurySample("SM-C" + lcsetNum, MercurySample.MetadataSource.BSP));
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
        lcsetBatch.addLabVessel(controlTube);

        Assert.assertEquals(controlTube.getSampleInstancesV2().iterator().next().getSingleBatch().getBatchName(),
                "LCSET-" + lcsetNum);

        LabEvent shearingTransfer = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(now++), "SUPERMAN", 1L, 101L,
                "Bravo");
        StaticPlate shearingPlate = new StaticPlate("SHEAR" + lcsetNum, StaticPlate.PlateType.Eppendorf96);
        shearingTransfer.getSectionTransfers().add(new SectionTransfer(
                extractControlTubeFormation.getContainerRole(), SBSSection.ALL96, null,
                shearingPlate.getContainerRole(), SBSSection.ALL96, null, shearingTransfer));

        // P7 index transfer
        LabEvent p7IndexTransfer = new LabEvent(LabEventType.INDEXED_ADAPTER_LIGATION, new Date(now++), "SPIDERMAN", 1L,
                101L, "Bravo");
        p7IndexTransfer.getSectionTransfers().add(new SectionTransfer(
                indexPlateP7.getContainerRole(), SBSSection.ALL96, null,
                shearingPlate.getContainerRole(), SBSSection.ALL96, null, p7IndexTransfer));

        // P5 index transfer
        LabEvent p5IndexTransfer = new LabEvent(LabEventType.INDEX_P5_POND_ENRICHMENT, new Date(now++), "TORCH", 1L,
                101L, "Bravo");
        p5IndexTransfer.getSectionTransfers().add(new SectionTransfer(
                indexPlateP5.getContainerRole(), SBSSection.ALL96, null,
                shearingPlate.getContainerRole(), SBSSection.ALL96, null, p5IndexTransfer));

        // bait transfers
        StaticPlate baitPlate = new StaticPlate("BAITPLATE" + lcsetNum, StaticPlate.PlateType.Eppendorf96);
        LabEvent baitSetupTransfer = new LabEvent(LabEventType.BAIT_SETUP, new Date(now++), "TICK", 1L, 101L, "Bravo");
        baitSetupTransfer.getVesselToSectionTransfers().add(new VesselToSectionTransfer(baitTube, SBSSection.ALL96,
                baitPlate.getContainerRole(), null, baitSetupTransfer));

        LabEvent baitAdditionTransfer = new LabEvent(LabEventType.BAIT_ADDITION, new Date(now++), "BATMAN", 1L, 101L,
                "Bravo");
        baitAdditionTransfer.getSectionTransfers().add(new SectionTransfer(
                baitPlate.getContainerRole(), SBSSection.ALL96, null,
                shearingPlate.getContainerRole(), SBSSection.ALL96, null, baitAdditionTransfer));

        // Verify 1st sample
        Set<SampleInstanceV2> sampleInstances =
                shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(position1);
        Assert.assertEquals(sampleInstances.size(), 1);
        SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
        Assert.assertEquals(sampleInstance.getMercuryRootSampleName(), tube1RootSample);
        verifyReagents(sampleInstance, lcsetNum == 1 ? "Illumina_P5-Habab_P7-Habab" : "Illumina_P5-Cabab_P7-Cabab");
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
        Assert.assertEquals(sampleInstance.getWorkflowName(), workflowName);

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
        verifyReagents(sampleInstance, lcsetNum == 1 ? "Illumina_P5-Nabab_P7-Nabab" : "Illumina_P5-Xabab_P7-Xabab");
        Assert.assertNull(sampleInstance.getSingleBucketEntry());
        Assert.assertEquals(sampleInstance.getSingleBatch(), lcsetBatch);

        return shearingPlate;
    }

    private void verifyReagents(SampleInstanceV2 sampleInstance, String expectedMolIndScheme) {
        Assert.assertEquals(sampleInstance.getReagents().size(), 2);
        MolecularIndexReagent molecularIndexReagent = (MolecularIndexReagent) sampleInstance.getReagents().get(0);
        Assert.assertEquals(molecularIndexReagent.getMolecularIndexingScheme().getName(), expectedMolIndScheme);
        DesignedReagent designedReagent = (DesignedReagent) sampleInstance.getReagents().get(1);
        Assert.assertEquals(designedReagent.getReagentDesign().getDesignName(), "cancer_2000gene_shift170_undercovered");
    }

    // todo jmt test that reworking fewer than all tubes in a rack doesn't alter the computed LCSET for messages with all tubes.

    /**
     * Test that reworking every tube in a rack is reflected in the computed LCSET.
     */
    @Test
    public void testLcSetOverride() {
        ProductOrder sampleInitProductOrder = ProductOrderTestFactory.createDummyProductOrder(3, "PDO-SI",
                Workflow.ICE, 101L, "Test research project", "Test research project", false, "SamInit", "1",
                "ExExQuoteId");

        // Create the source tubes for the first transfer
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        Set<LabVessel> starterVessels1 = new HashSet<>();
        int i = 0;
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            BarcodedTube barcodedTube = new BarcodedTube("tube1." + i, BarcodedTube.BarcodedTubeType.MatrixTube);
            productOrderSample.setMercurySample(new MercurySample(productOrderSample.getSampleKey(),
                    MercurySample.MetadataSource.BSP));
            barcodedTube.addSample(productOrderSample.getMercurySample());
            mapPositionToTube.put(SBSSection.ALL96.getWells().get(i), barcodedTube);
            starterVessels1.add(barcodedTube);
            i++;
        }

        // Create the first LCSET.
        LabBatch lcSet1 = new LabBatch("LCSET-1", starterVessels1, LabBatch.LabBatchType.WORKFLOW);
        for (LabVessel labVessel : lcSet1.getStartingBatchLabVessels()) {
            BucketEntry bucketEntry = new BucketEntry(labVessel, sampleInitProductOrder,
                    BucketEntry.BucketEntryType.PDO_ENTRY);
            lcSet1.addBucketEntry(bucketEntry);
            labVessel.addBucketEntry(bucketEntry);
        }
        TubeFormation tubeFormation1 = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        // Create the destination tubes for the first transfer.
        Set<LabVessel> starterVessels2 = new HashSet<>();
        mapPositionToTube = new HashMap<>();
        for (int j = 0; j < tubeFormation1.getContainerRole().getContainedVessels().size(); j++) {
            BarcodedTube barcodedTube = new BarcodedTube("tube2." + j, BarcodedTube.BarcodedTubeType.MatrixTube);
            mapPositionToTube.put(SBSSection.ALL96.getWells().get(j), barcodedTube);
            starterVessels2.add(barcodedTube);
        }
        TubeFormation tubeFormation2 = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        LabEvent labEvent1 = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(), "SPIDERMAN", 1L, 101L, "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(
                tubeFormation1.getContainerRole(), SBSSection.ALL96, null,
                tubeFormation2.getContainerRole(), SBSSection.ALL96, null, labEvent1));

        // Create the second LCSET by reworking all tubes.
        LabBatch lcSet2 = new LabBatch("LCSET-2", starterVessels2, LabBatch.LabBatchType.WORKFLOW);
        for (LabVessel labVessel : lcSet2.getStartingBatchLabVessels()) {
            BucketEntry bucketEntry = new BucketEntry(labVessel, sampleInitProductOrder,
                    BucketEntry.BucketEntryType.REWORK_ENTRY);
            lcSet2.addBucketEntry(bucketEntry);
            labVessel.addBucketEntry(bucketEntry);
        }

        // Create the destination tubes for the second transfer.
        mapPositionToTube = new HashMap<>();
        for (int j = 0; j < tubeFormation2.getContainerRole().getContainedVessels().size(); j++) {
            BarcodedTube barcodedTube = new BarcodedTube("tube3." + j, BarcodedTube.BarcodedTubeType.MatrixTube);
            mapPositionToTube.put(SBSSection.ALL96.getWells().get(j), barcodedTube);
        }
        TubeFormation tubeFormation3 = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        // Create the second transfer.
        LabEvent labEvent2 = new LabEvent(LabEventType.HYBRIDIZATION, new Date(), "BATMAN", 2L, 101L, "Bravo");
        labEvent2.getSectionTransfers().add(new SectionTransfer(
                tubeFormation2.getContainerRole(), SBSSection.ALL96, null,
                tubeFormation3.getContainerRole(), SBSSection.ALL96, null, labEvent2));

        Assert.assertEquals(labEvent2.getComputedLcSets().size(), 1);
        Assert.assertEquals(labEvent2.getComputedLcSets().iterator().next(), lcSet2);
    }

    public void testGetMetadataSourceForPipeline() {
        Assert.assertEquals(createSampleInstanceForPipelineAPIMetadataTesting(MercurySample.MetadataSource.BSP,"sample").getMetadataSourceForPipelineAPI(),
                                                                              MercurySample.BSP_METADATA_SOURCE);
        Assert.assertEquals(createSampleInstanceForPipelineAPIMetadataTesting(MercurySample.MetadataSource.MERCURY, "sample").getMetadataSourceForPipelineAPI(),
                            MercurySample.MERCURY_METADATA_SOURCE);
    }

    @DataProvider(name = "aggregationParticleProvider")
    public Iterator<Object[]> aggregationParticleProvider() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{Product.AggregationParticle.PDO_ALIQUOT, "PDO-XYZ", "SM-222", null, "PDO-XYZ.SM-222"});
        testCases.add(new Object[]{Product.AggregationParticle.PDO, "PDO-XYZ", "SM-222",null,  "PDO-XYZ"});
        testCases.add(new Object[]{null, "PDO-XYZ", "SM-222", null, null});
        testCases.add(new Object[]{null, null, null, null, null});

        testCases.add(new Object[]{Product.AggregationParticle.PDO_ALIQUOT, "PDO-XYZ", "SM-222", "FOOFOO", "FOOFOO"});
        testCases.add(new Object[]{Product.AggregationParticle.PDO, "PDO-XYZ", "SM-222","FOOFOO", "FOOFOO"});

        return testCases.iterator();
    }

    @Test(dataProvider = "aggregationParticleProvider")
    public void testGetAggregationParticleForPipeline(Product.AggregationParticle aggregationParticle, String pdo,
                                                      String sampleId, String pdoParticleString, String aggregationParticleString) {

        SampleInstanceV2 sample =
            createSampleInstanceForPipelineAPIAggregationParticleTesting(pdo, sampleId, pdoParticleString,
                aggregationParticle);

        Assert.assertEquals(sample.getAggregationParticle(), aggregationParticleString);
    }

    @Test
    public void testLcsetInference() {
        // todo jmt add and assert bucket entries
        // Create LCSET 1 with 2 new tubes
        Map<VesselPosition, BarcodedTube> mapLcset1Pos1ToTube = new HashMap<>();
        BarcodedTube lcset1T1 = new BarcodedTube("LCSET1T1");
        lcset1T1.addSample(new MercurySample("S1_1", MercurySample.MetadataSource.MERCURY));
        BarcodedTube lcset1T2 = new BarcodedTube("LCSET1T2");
        lcset1T2.addSample(new MercurySample("S1_2", MercurySample.MetadataSource.MERCURY));
        mapLcset1Pos1ToTube.put(VesselPosition.A01, lcset1T1);
        mapLcset1Pos1ToTube.put(VesselPosition.A02, lcset1T2);
        LabBatch lcset1 = new LabBatch("LCSET1", new HashSet<LabVessel>(mapLcset1Pos1ToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        TubeFormation lcset1TubeForm1 = new TubeFormation(mapLcset1Pos1ToTube, RackOfTubes.RackType.Matrix96);

        // Create LCSET 2 with 1 new tube and 1 rework
        Map<VesselPosition, BarcodedTube> mapLcset2Pos1ToTube = new HashMap<>();
        BarcodedTube lcset2T1 = new BarcodedTube("LCSET2T1");
        lcset2T1.addSample(new MercurySample("S2_1", MercurySample.MetadataSource.MERCURY));
        mapLcset2Pos1ToTube.put(VesselPosition.A01, lcset2T1);
        mapLcset2Pos1ToTube.put(VesselPosition.A02, lcset1T2);
        LabBatch lcset2 = new LabBatch("LCSET2", Collections.<LabVessel>singleton(lcset2T1),
                Collections.<LabVessel>singleton(lcset1T2), LabBatch.LabBatchType.WORKFLOW, "", "", new Date(), "");
        TubeFormation lcset2TubeForm1 = new TubeFormation(mapLcset2Pos1ToTube, RackOfTubes.RackType.Matrix96);

        // LCSET 1 rack to rack
        Map<VesselPosition, BarcodedTube> mapLcset1Pos2ToTube = new HashMap<>();
        BarcodedTube lcset1T1Child = new BarcodedTube("LCSET1T1_2");
        mapLcset1Pos2ToTube.put(VesselPosition.A01, lcset1T1Child);
        BarcodedTube lcset1T2Child = new BarcodedTube("LCSET1T2_2");
        mapLcset1Pos2ToTube.put(VesselPosition.A02, lcset1T2Child);
        TubeFormation lcset1TubeForm2 = new TubeFormation(mapLcset1Pos2ToTube, RackOfTubes.RackType.Matrix96);
        LabEvent shearingEventLcset1 = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        shearingEventLcset1.getSectionTransfers().add(new SectionTransfer(
                lcset1TubeForm1.getContainerRole(), SBSSection.ALL96, null,
                lcset1TubeForm2.getContainerRole(), SBSSection.ALL96, null, shearingEventLcset1));

        // LCSET 1 pool
        Map<VesselPosition, BarcodedTube> mapLcset1Pos3ToTube = new HashMap<>();
        BarcodedTube lcset1PoolTube = new BarcodedTube("LCSET1PT");
        mapLcset1Pos3ToTube.put(VesselPosition.A01, lcset1PoolTube);
        TubeFormation lcset1TubeForm3 = new TubeFormation(mapLcset1Pos3ToTube, RackOfTubes.RackType.Matrix96);
        LabEvent poolingEventLcset1 = new LabEvent(LabEventType.ICE_POOLING_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        poolingEventLcset1.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset1TubeForm2.getContainerRole(), VesselPosition.A01, null,
                lcset1TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset1));
        poolingEventLcset1.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset1TubeForm2.getContainerRole(), VesselPosition.A02, null,
                lcset1TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset1));

        // LCSET 1 post pool
        LabEvent spriEventLcset1 = new LabEvent(LabEventType.ICE_96_PLEX_SPRI_CONCENTRATION, new Date(), "BATMAN", 1L,
                101L, "Bravo");
        Map<VesselPosition, BarcodedTube> mapLcset1Pos4ToTube = new HashMap<>();
        BarcodedTube lcset1SpriTube = new BarcodedTube("LCSET1ST");
        mapLcset1Pos4ToTube.put(VesselPosition.A01, lcset1SpriTube);
        TubeFormation lcset1TubeForm4 = new TubeFormation(mapLcset1Pos4ToTube, RackOfTubes.RackType.Matrix96);
        spriEventLcset1.getSectionTransfers().add(new SectionTransfer(
                lcset1TubeForm3.getContainerRole(), SBSSection.ALL96, null,
                lcset1TubeForm4.getContainerRole(), SBSSection.ALL96, null, spriEventLcset1));

        // LCSET 2 rack to rack
        Map<VesselPosition, BarcodedTube> mapLcset2Pos2ToTube = new HashMap<>();
        BarcodedTube lcset2T1Child = new BarcodedTube("LCSET2T1_2");
        mapLcset2Pos2ToTube.put(VesselPosition.A01, lcset2T1Child);
        BarcodedTube lcset2T2Child = new BarcodedTube("LCSET2T2_2");
        mapLcset2Pos2ToTube.put(VesselPosition.A02, lcset2T2Child);
        TubeFormation lcset2TubeForm2 = new TubeFormation(mapLcset2Pos2ToTube, RackOfTubes.RackType.Matrix96);
        LabEvent shearingEventLcset2 = new LabEvent(LabEventType.SHEARING_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        shearingEventLcset2.getSectionTransfers().add(new SectionTransfer(
                lcset2TubeForm1.getContainerRole(), SBSSection.ALL96, null,
                lcset2TubeForm2.getContainerRole(), SBSSection.ALL96, null, shearingEventLcset2));

        // LCSET 2 pool
        Map<VesselPosition, BarcodedTube> mapLcset2Pos3ToTube = new HashMap<>();
        BarcodedTube lcset2PoolTube = new BarcodedTube("LCSET2PT");
        mapLcset2Pos3ToTube.put(VesselPosition.A01, lcset2PoolTube);
        TubeFormation lcset2TubeForm3 = new TubeFormation(mapLcset2Pos3ToTube, RackOfTubes.RackType.Matrix96);
        LabEvent poolingEventLcset2 = new LabEvent(LabEventType.POOLING_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        poolingEventLcset2.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset2TubeForm2.getContainerRole(), VesselPosition.A01, null,
                lcset2TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset2));
        poolingEventLcset2.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset2TubeForm2.getContainerRole(), VesselPosition.A02, null,
                lcset2TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset2));

        // LCSET 2 post-pool
        LabEvent spriEventLcset2 = new LabEvent(LabEventType.ICE_96_PLEX_SPRI_CONCENTRATION, new Date(), "BATMAN", 1L,
                101L, "Bravo");
        Map<VesselPosition, BarcodedTube> mapLcset2Pos4ToTube = new HashMap<>();
        BarcodedTube lcset2SpriTube = new BarcodedTube("LCSET2ST");
        mapLcset2Pos4ToTube.put(VesselPosition.A01, lcset2SpriTube);
        TubeFormation lcset2TubeForm4 = new TubeFormation(mapLcset2Pos4ToTube, RackOfTubes.RackType.Matrix96);
        spriEventLcset2.getSectionTransfers().add(new SectionTransfer(
                lcset2TubeForm3.getContainerRole(), SBSSection.ALL96, null,
                lcset2TubeForm4.getContainerRole(), SBSSection.ALL96, null, spriEventLcset2));

        // Create LCSET 3 from the first transfers of LCSET 1 and 2
        Map<VesselPosition, BarcodedTube> mapLcset3Pos1ToTube = new HashMap<>();
        mapLcset3Pos1ToTube.put(VesselPosition.A01, lcset1T2Child);
        mapLcset3Pos1ToTube.put(VesselPosition.A02, lcset2T1Child);
        LabBatch lcset3 = new LabBatch("LCSET3", Collections.<LabVessel>emptySet(),
                new HashSet<LabVessel>(mapLcset3Pos1ToTube.values()), LabBatch.LabBatchType.WORKFLOW, "", "",
                new Date(), "");
        TubeFormation lcset3TubeForm2 = new TubeFormation(mapLcset3Pos1ToTube, RackOfTubes.RackType.Matrix96);

        // LCSET 3 pool
        Map<VesselPosition, BarcodedTube> mapLcset3Pos3ToTube = new HashMap<>();
        BarcodedTube lcset3PoolTube = new BarcodedTube("LCSET3PT");
        mapLcset3Pos3ToTube.put(VesselPosition.A01, lcset3PoolTube);
        TubeFormation lcset3TubeForm3 = new TubeFormation(mapLcset3Pos3ToTube, RackOfTubes.RackType.Matrix96);
        LabEvent poolingEventLcset3 = new LabEvent(LabEventType.POOLING_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        poolingEventLcset3.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset3TubeForm2.getContainerRole(), VesselPosition.A01, null,
                lcset3TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset3));
        poolingEventLcset3.getCherryPickTransfers().add(new CherryPickTransfer(
                lcset3TubeForm2.getContainerRole(), VesselPosition.A02, null,
                lcset3TubeForm3.getContainerRole(), VesselPosition.A01, null, poolingEventLcset3));

        // LCSET 3 post-pool
        LabEvent spriEventLcset3 = new LabEvent(LabEventType.ICE_96_PLEX_SPRI_CONCENTRATION, new Date(), "BATMAN", 1L,
                101L, "Bravo");
        Map<VesselPosition, BarcodedTube> mapLcset3Pos4ToTube = new HashMap<>();
        BarcodedTube lcset3SpriTube = new BarcodedTube("LCSET3ST");
        mapLcset3Pos4ToTube.put(VesselPosition.A01, lcset3SpriTube);
        TubeFormation lcset3TubeForm4 = new TubeFormation(mapLcset3Pos4ToTube, RackOfTubes.RackType.Matrix96);
        spriEventLcset3.getSectionTransfers().add(new SectionTransfer(
                lcset3TubeForm3.getContainerRole(), SBSSection.ALL96, null,
                lcset3TubeForm4.getContainerRole(), SBSSection.ALL96, null, spriEventLcset3));

        // Do a transfer that includes all 3 LCSETs
        LabEvent denatureEvent = new LabEvent(LabEventType.DENATURE_TRANSFER, new Date(), "BATMAN", 1L, 101L, "Bravo");
        Map<VesselPosition, BarcodedTube> mapDenatureSourcePosToTube = new HashMap<>();
        mapDenatureSourcePosToTube.put(VesselPosition.A01, lcset1SpriTube);
        mapDenatureSourcePosToTube.put(VesselPosition.A02, lcset2SpriTube);
        mapDenatureSourcePosToTube.put(VesselPosition.A03, lcset3SpriTube);
        TubeFormation denatureSourceTubeForm = new TubeFormation(mapDenatureSourcePosToTube,
                RackOfTubes.RackType.Matrix96);
        Map<VesselPosition, BarcodedTube> mapDenatureDestPosToTube = new HashMap<>();
        mapDenatureDestPosToTube.put(VesselPosition.A01, new BarcodedTube("LCSET1DT"));
        mapDenatureDestPosToTube.put(VesselPosition.A02, new BarcodedTube("LCSET2DT"));
        mapDenatureDestPosToTube.put(VesselPosition.A03, new BarcodedTube("LCSET3DT"));
        TubeFormation denatureDestTubeForm = new TubeFormation(mapDenatureDestPosToTube, RackOfTubes.RackType.Matrix96);

        denatureEvent.getSectionTransfers().add(new SectionTransfer(
                denatureSourceTubeForm.getContainerRole(), SBSSection.ALL96, null,
                denatureDestTubeForm.getContainerRole(), SBSSection.ALL96, null, denatureEvent));

        BaseEventTest.runTransferVisualizer(lcset1T2);

        Assert.assertEquals(lcset1T1Child.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset1);
        Assert.assertEquals(lcset1T2Child.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset1);
        Assert.assertEquals(lcset1PoolTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset1);
        Assert.assertEquals(lcset1SpriTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset1);

        Assert.assertEquals(lcset2T1Child.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset2);
        Assert.assertEquals(lcset2T2Child.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset2);
        Assert.assertEquals(lcset2PoolTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset2);
        Assert.assertEquals(lcset2SpriTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset2);

        Assert.assertEquals(lcset3PoolTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset3);
        Assert.assertEquals(lcset3SpriTube.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset3);

        Assert.assertEquals(shearingEventLcset1.getComputedLcSets().size(), 1);
        Assert.assertEquals(shearingEventLcset1.getComputedLcSets().iterator().next(), lcset1);
        Assert.assertEquals(shearingEventLcset2.getComputedLcSets().size(), 1);
        Assert.assertEquals(shearingEventLcset2.getComputedLcSets().iterator().next(), lcset2);
        Assert.assertEquals(poolingEventLcset1.getComputedLcSets().size(), 1);
        Assert.assertEquals(poolingEventLcset1.getComputedLcSets().iterator().next(), lcset1);
        Assert.assertEquals(poolingEventLcset2.getComputedLcSets().size(), 1);
        Assert.assertEquals(poolingEventLcset2.getComputedLcSets().iterator().next(), lcset2);
        Assert.assertEquals(poolingEventLcset3.getComputedLcSets().size(), 1);
        Assert.assertEquals(poolingEventLcset3.getComputedLcSets().iterator().next(), lcset3);
        Assert.assertEquals(spriEventLcset1.getComputedLcSets().size(), 1);
        Assert.assertEquals(spriEventLcset1.getComputedLcSets().iterator().next(), lcset1);
        Assert.assertEquals(spriEventLcset2.getComputedLcSets().size(), 1);
        Assert.assertEquals(spriEventLcset2.getComputedLcSets().iterator().next(), lcset2);
        Assert.assertEquals(spriEventLcset3.getComputedLcSets().size(), 1);
        Assert.assertEquals(spriEventLcset3.getComputedLcSets().iterator().next(), lcset3);
        Assert.assertEquals(denatureEvent.getComputedLcSets().size(), 3);

    }

    /**
     * Tests an ICE pooling transfer with two LCSETs in parallel.
     */
    @Test
    public void testDoublePool() {
        Map<VesselPosition, BarcodedTube> mapSourcePos1ToTube = new HashMap<>();
        BarcodedTube l1T1 = new BarcodedTube("L1T1", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapSourcePos1ToTube.put(VesselPosition.A01, l1T1);
        mapSourcePos1ToTube.put(VesselPosition.A02, new BarcodedTube("L1T2", BarcodedTube.BarcodedTubeType.MatrixTube));
        LabBatch lcset1 = new LabBatch("LCSET1", new HashSet<LabVessel>(mapSourcePos1ToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        TubeFormation sourceTf1 = new TubeFormation(mapSourcePos1ToTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, BarcodedTube> mapSourcePos2ToTube = new HashMap<>();
        mapSourcePos2ToTube.put(VesselPosition.A01, new BarcodedTube("L2T1", BarcodedTube.BarcodedTubeType.MatrixTube));
        mapSourcePos2ToTube.put(VesselPosition.A02, new BarcodedTube("L2T2", BarcodedTube.BarcodedTubeType.MatrixTube));
        LabBatch lcset2 = new LabBatch("LCSET2", new HashSet<LabVessel>(mapSourcePos2ToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        TubeFormation sourceTf2 = new TubeFormation(mapSourcePos2ToTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, BarcodedTube> mapTargetPosToTube = new HashMap<>();
        BarcodedTube l1T3 = new BarcodedTube("L1T3", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapTargetPosToTube.put(VesselPosition.A01, l1T3);
        BarcodedTube l2T3 = new BarcodedTube("L2T3", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapTargetPosToTube.put(VesselPosition.A02, l2T3);
        TubeFormation targetTf = new TubeFormation(mapTargetPosToTube, RackOfTubes.RackType.Matrix96);

        LabEvent labEvent = new LabEvent(LabEventType.ICE_POOLING_TRANSFER, new Date(), "BATMAN", 1L, 101L, "Hamilton");
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTf1.getContainerRole(), VesselPosition.A01, null,
                targetTf.getContainerRole(), VesselPosition.A01, null, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTf1.getContainerRole(), VesselPosition.A02, null,
                targetTf.getContainerRole(), VesselPosition.A01, null, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTf2.getContainerRole(), VesselPosition.A01, null,
                targetTf.getContainerRole(), VesselPosition.A02, null, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTf2.getContainerRole(), VesselPosition.A02, null,
                targetTf.getContainerRole(), VesselPosition.A02, null, labEvent));
        Assert.assertEquals(labEvent.getComputedLcSets().size(), 2);
        Assert.assertEquals(labEvent.getMapPositionToLcSets().size(), 2);
        Assert.assertEquals(labEvent.getMapPositionToLcSets().get(VesselPosition.A01).getLabBatchSet().iterator().next(), lcset1);
        Assert.assertEquals(labEvent.getMapPositionToLcSets().get(VesselPosition.A02).getLabBatchSet().iterator().next(), lcset2);
        Assert.assertEquals(l1T3.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset1);
        Assert.assertEquals(l2T3.getSampleInstancesV2().iterator().next().getSingleBatch(), lcset2);

        BaseEventTest.runTransferVisualizer(l1T1);
    }

    /**
     * Some daughter transfers backfilled from BSP contain daughter and grand daughter transfers in the same cherry
     * pick.  Test that this does not cause a stack overflow.
     */
    public void testGrandDaughter() {
        BarcodedTube parentTube1 = new BarcodedTube("P1");
        String sampleKey = "SM-1";
        MercurySample mercurySample1 = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
        mercurySample1.addLabVessel(parentTube1);

        BarcodedTube parentTube2 = new BarcodedTube("P2");
        MercurySample mercurySample2 = new MercurySample("SM-2", MercurySample.MetadataSource.BSP);
        mercurySample2.addLabVessel(parentTube2);

        HashMap<VesselPosition, BarcodedTube> mapPositionToParentTube = new HashMap<>();
        mapPositionToParentTube.put(VesselPosition.A01, parentTube1);
        mapPositionToParentTube.put(VesselPosition.A02, parentTube2);
        TubeFormation parentTf = new TubeFormation(mapPositionToParentTube, RackOfTubes.RackType.Matrix96);

        HashMap<VesselPosition, BarcodedTube> mapPositionToDaughterTube = new HashMap<>();
        BarcodedTube daughterTube1 = new BarcodedTube("D1");
        mapPositionToDaughterTube.put(VesselPosition.A01, daughterTube1);
        BarcodedTube daughterTube2 = new BarcodedTube("D2");
        mapPositionToDaughterTube.put(VesselPosition.A02, daughterTube2);
        TubeFormation daughterTf = new TubeFormation(mapPositionToDaughterTube, RackOfTubes.RackType.Matrix96);

        HashMap<VesselPosition, BarcodedTube> mapPositionToGrandDaughterTube = new HashMap<>();
        BarcodedTube grandDaughterTube1 = new BarcodedTube("GD1");
        mapPositionToGrandDaughterTube.put(VesselPosition.A01, grandDaughterTube1);
        BarcodedTube grandDaughterTube2 = new BarcodedTube("GD2");
        mapPositionToGrandDaughterTube.put(VesselPosition.A02, grandDaughterTube2);
        TubeFormation grandDaughterTf = new TubeFormation(mapPositionToGrandDaughterTube, RackOfTubes.RackType.Matrix96);

        LabEvent grandDaughterEvent = new LabEvent(LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION, new Date(), "BATMAN", 1L, 101L, "Bravo");
        grandDaughterEvent.getCherryPickTransfers().add(new CherryPickTransfer(parentTf.getContainerRole(), VesselPosition.A01, null,
                daughterTf.getContainerRole(), VesselPosition.A01, null, grandDaughterEvent));
        grandDaughterEvent.getCherryPickTransfers().add(new CherryPickTransfer(parentTf.getContainerRole(), VesselPosition.A02, null,
                daughterTf.getContainerRole(), VesselPosition.A02, null, grandDaughterEvent));
        grandDaughterEvent.getCherryPickTransfers().add(new CherryPickTransfer(daughterTf.getContainerRole(), VesselPosition.A01, null,
                grandDaughterTf.getContainerRole(), VesselPosition.A01, null, grandDaughterEvent));
        grandDaughterEvent.getCherryPickTransfers().add(new CherryPickTransfer(daughterTf.getContainerRole(), VesselPosition.A02, null,
                grandDaughterTf.getContainerRole(), VesselPosition.A02, null, grandDaughterEvent));

        Set<SampleInstanceV2> sampleInstances = grandDaughterTube1.getSampleInstancesV2();
        Assert.assertEquals(sampleInstances.size(), 1);
        Assert.assertEquals(sampleInstances.iterator().next().getRootOrEarliestMercurySampleName(),
                sampleKey);
    }

    /**
     * Test LCSET inference for an Ultra Low Pass Genome, followed by a Custom Panel on the Ponds.
     * E.g. GPLIM-5727
     */
    @Test
    public void testUlpThenExomeOnPonds() {
        // DNA Tubes
        BarcodedTube dnaTube1 = new BarcodedTube("DNA1");
        dnaTube1.addSample(new MercurySample("SM-1", MercurySample.MetadataSource.BSP));
        BarcodedTube dnaTube2 = new BarcodedTube("DNA2");
        dnaTube2.addSample(new MercurySample("SM-2", MercurySample.MetadataSource.BSP));

        // ULP LCSET
        HashSet<LabVessel> ulpStarterVessels = new HashSet<>();
        ulpStarterVessels.add(dnaTube1);
        ulpStarterVessels.add(dnaTube2);
        LabBatch ulpLcset = new LabBatch("LCSET-1", ulpStarterVessels, LabBatch.LabBatchType.WORKFLOW);
        ulpLcset.setWorkflowName(Workflow.CELL_FREE_HYPER_PREP_UMIS);
        Bucket bucket = new Bucket("Pico");
        ProductOrder ulpPdo = ProductOrderTestFactory.createDummyProductOrder(3, "PDO-ULP",
                Workflow.CELL_FREE_HYPER_PREP_UMIS, 101L, "Test research project", "Test research project", false, "UlpWgs", "1",
                "UlpWgsQuoteId");
        ulpLcset.addBucketEntry(new BucketEntry(dnaTube1, ulpPdo, bucket, BucketEntry.BucketEntryType.PDO_ENTRY));
        ulpLcset.addBucketEntry(new BucketEntry(dnaTube2, ulpPdo, bucket, BucketEntry.BucketEntryType.PDO_ENTRY));

        // DNA Rack
        HashMap<VesselPosition, BarcodedTube> dnaMapPositionToTube = new HashMap<>();
        dnaMapPositionToTube.put(VesselPosition.A01, dnaTube1);
        dnaMapPositionToTube.put(VesselPosition.A02, dnaTube2);
        TubeFormation dnaTf = new TubeFormation(dnaMapPositionToTube, RackOfTubes.RackType.Matrix96);

        // Pond Rack
        BarcodedTube pondTube1 = new BarcodedTube("POND1");
        BarcodedTube pondTube2 = new BarcodedTube("POND2");
        HashMap<VesselPosition, BarcodedTube> pondMapPositionToTube = new HashMap<>();
        pondMapPositionToTube.put(VesselPosition.A01, pondTube1);
        pondMapPositionToTube.put(VesselPosition.A02, pondTube2);
        TubeFormation pondTf = new TubeFormation(pondMapPositionToTube, RackOfTubes.RackType.Matrix96);

        // PondRegistration
        LabEvent pondReg = new LabEvent(LabEventType.CF_DNA_POND_REGISTRATION, new Date(), "BATMAN", 1L, 1L, "Bravo");
        pondReg.getSectionTransfers().add(new SectionTransfer(dnaTf.getContainerRole(), SBSSection.ALL96, null,
                pondTf.getContainerRole(), SBSSection.ALL96, null, pondReg));

        // Pool rack
        BarcodedTube ulpPoolTube = new BarcodedTube("ULPPool");
        HashMap<VesselPosition, BarcodedTube> poolMapPositionToTube = new HashMap<>();
        poolMapPositionToTube.put(VesselPosition.A01, ulpPoolTube);
        TubeFormation poolTf = new TubeFormation(poolMapPositionToTube, RackOfTubes.RackType.Matrix96);

        // Pooling Transfer
        LabEvent poolingEvnt = new LabEvent(LabEventType.POOLING_TRANSFER, new Date(), "BATMAN", 1L, 1L, "Hamilton");
        poolingEvnt.getCherryPickTransfers().add(new CherryPickTransfer(pondTf.getContainerRole(), VesselPosition.A01,
                null, poolTf.getContainerRole(), VesselPosition.A01, null, poolingEvnt));
        poolingEvnt.getCherryPickTransfers().add(new CherryPickTransfer(pondTf.getContainerRole(), VesselPosition.A02,
                null, poolTf.getContainerRole(), VesselPosition.A01, null, poolingEvnt));

        // Exome LCSET
        HashSet<LabVessel> exomeStarterVessels = new HashSet<>();
        exomeStarterVessels.add(pondTube1);
        exomeStarterVessels.add(pondTube2);
        LabBatch exomeLcset = new LabBatch("LCSET-2", exomeStarterVessels, LabBatch.LabBatchType.WORKFLOW);
        exomeLcset.setWorkflowName(Workflow.ICE_EXOME_EXPRESS_HYPER_PREP_UMIS);
        Bucket iceBucket = new Bucket("ICE");
        ProductOrder icePdo = ProductOrderTestFactory.createDummyProductOrder(3, "PDO-EXOME",
                Workflow.ICE_EXOME_EXPRESS_HYPER_PREP_UMIS, 101L, "Test research project", "Test research project",
                false, "Exome", "1", "ExomeQuoteId");
        exomeLcset.addBucketEntry(new BucketEntry(pondTube1, icePdo, iceBucket, BucketEntry.BucketEntryType.PDO_ENTRY));
        exomeLcset.addBucketEntry(new BucketEntry(pondTube2, icePdo, iceBucket, BucketEntry.BucketEntryType.PDO_ENTRY));

        // Selection Pool rack
        BarcodedTube selPoolTube = new BarcodedTube("ExPool");
        HashMap<VesselPosition, BarcodedTube> selPoolMapPositionToTube = new HashMap<>();
        selPoolMapPositionToTube.put(VesselPosition.A01, selPoolTube);
        TubeFormation selPoolTf = new TubeFormation(selPoolMapPositionToTube, RackOfTubes.RackType.Matrix96);

        // SelectionPoolingTransfer
        LabEvent selPoolingEvnt = new LabEvent(LabEventType.SELECTION_POOLING, new Date(), "BATMAN", 1L, 1L, "Hamilton");
        selPoolingEvnt.getCherryPickTransfers().add(new CherryPickTransfer(pondTf.getContainerRole(), VesselPosition.A01,
                null, selPoolTf.getContainerRole(), VesselPosition.A01, null, selPoolingEvnt));
        selPoolingEvnt.getCherryPickTransfers().add(new CherryPickTransfer(pondTf.getContainerRole(), VesselPosition.A02,
                null, selPoolTf.getContainerRole(), VesselPosition.A01, null, selPoolingEvnt));

        // SelectionCatchRegistration
        // PoolingTransfer
        BaseEventTest.runTransferVisualizer(dnaTube1);

        // Validate all transfers
        Set<SampleInstanceV2> ulpSampleInstances = ulpPoolTube.getSampleInstancesV2();
        Assert.assertEquals(ulpSampleInstances.size(), 2);
        // todo jmt single bucket entry?
        Assert.assertEquals(ulpSampleInstances.iterator().next().getSingleBatch().getBatchName(), ulpLcset.getBatchName());

        Set<SampleInstanceV2> exomeSampleInstances = selPoolTube.getSampleInstancesV2();
        Assert.assertEquals(exomeSampleInstances.size(), 2);
        // todo jmt single bucket entry?
        Assert.assertEquals(exomeSampleInstances.iterator().next().getSingleBatch().getBatchName(), exomeLcset.getBatchName());
    }
}
