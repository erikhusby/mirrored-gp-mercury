package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.BucketException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow.AGILENT_EXOME_EXPRESS;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow.ICE;

/**
 * DbFree test of BucketEjb.
 */
// singleThreaded because EasyMock mocks are not thread-safe during recording.
@Test(enabled = true, groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class BucketEjbDbFreeTest {
    private final int SAMPLE_SIZE = 5;

    private ProductOrder pdo;
    private LabBatch labBatch;
    private WorkflowLoader workflowLoader = new WorkflowLoader();
    private BSPUserList bspUserList;
    private String username;
    private final Map<String, BspSampleData> bspSampleDataMap = new HashMap<>();
    private final List<LabVessel> labVessels = new ArrayList<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();

    private BucketEjb bucketEjb;

    private LabEventFactory labEventFactory = Mockito.mock(LabEventFactory.class);
    private BucketDao bucketDao = Mockito.mock(BucketDao.class);
    private BucketEntryDao bucketEntryDao = Mockito.mock(BucketEntryDao.class);
    private LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
    private BSPSampleDataFetcher bspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcherImpl.class);
    private LabVesselFactory labVesselFactory = Mockito.mock(LabVesselFactory.class);
    public MercurySampleDao mercurysampleDao = Mockito.mock(MercurySampleDao.class);

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    private void beforeClass() {
        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        username = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();
        workflowLoader.load();
    }

    // Initializes the samples and sets up mocks.
    private void setup(String workflow, boolean makeTubes) {
        switch (workflow) {
        case AGILENT_EXOME_EXPRESS:
            labBatch = new LabBatch("ExEx Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
            break;
        case ICE:
            labBatch = new LabBatch("ICE Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            pdo = ProductOrderTestFactory.buildIceProductOrder(SAMPLE_SIZE);
            break;
        default:
            Assert.fail("No constructor for " + workflow);
        }

        labVessels.clear();
        mercurySamples.clear();
        bspSampleDataMap.clear();
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);

        Map<String, MercurySample> mercurySampleMap = new HashMap<>();

        int rackPosition = 1;
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            Map<BSPSampleSearchColumn, String> bspData = new HashMap<>();
            bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.DNA_DNA_GENOMIC.getDisplayName());
            bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getName());
            bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getName());
            bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "04/22/2013");
            // Puts the BSP tube barcode in the SampleData.
            String tubeBarcode = makeTubeBarcode(rackPosition++);
            BspSampleData bspSampleData = new BspSampleData(bspData);
            bspSampleData.addPlastic(tubeBarcode);
            bspSampleDataMap.put(pdoSample.getName(), bspSampleData);

            if (makeTubes) {
                LabVessel labVessel = new BarcodedTube(tubeBarcode);
                labVessels.add(labVessel);
                labBatch.addLabVessel(labVessel);

                MercurySample mercurySample = new MercurySample(pdoSample.getName(), bspSampleData);
                mercurySamples.add(mercurySample);
                mercurySample.addLabVessel(labVessel);
                mercurySample.addProductOrderSample(pdoSample);

                mercurySampleMap.put(pdoSample.getName(), mercurySample);
            }
        }

        bucketEjb = new BucketEjb(labEventFactory, JiraServiceTestProducer.stubInstance(), bucketDao,
                bucketEntryDao, labVesselDao, labVesselFactory, bspSampleDataFetcher, bspUserList,
                workflowLoader, Mockito.mock(ProductOrderDao.class), mercurysampleDao);

        Mockito.when(labEventFactory.buildFromBatchRequests(Mockito.anyList(), Mockito.anyObject(),
                Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject(),
                Mockito.anyObject(), Mockito.eq(0))).thenReturn(Collections.emptyList());

        Mockito.when(bspSampleDataFetcher.fetchSampleData(Mockito.anyCollection())).thenReturn(bspSampleDataMap);

        Mockito.when(labVesselFactory.buildInitialLabVessels(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any())).thenAnswer(invocationOnMock -> {
            final Object[] arguments = invocationOnMock.getArguments();
            String barcode = (String)arguments[1];
            List<LabVessel> tubes = new ArrayList<>();
            if (StringUtils.isNotBlank(barcode)) {
                tubes.add(new BarcodedTube(barcode));
            }
            return Pair.of(tubes, Collections.emptyList());
        });
    }

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testCreateInitialVessel() throws Exception {
        // Doesn't create tubes or mercury samples and leaves the pdo samples unlinked.
        setup(ICE, false);

        // Updates the bspSampleData to indicate a sample is not received.
        int indexOfBadSample = 4;
        bspSampleDataMap.get(pdo.getSamples().get(indexOfBadSample).getName()).getColumnToValue().
                remove(BSPSampleSearchColumn.RECEIPT_DATE);

        // Non-genomic material should get a new tube.
        bspSampleDataMap.get(pdo.getSamples().get(1).getName()).getColumnToValue().
                put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.FRESH_BLOOD.getDisplayName());

        Collection<LabVessel> vessels;
        try {
            vessels = bucketEjb.createInitialVessels(pdo.getSamples().stream().
                    map(ProductOrderSample::getName).collect(Collectors.toList()), username);
            Assert.fail("Should have thrown.");
        } catch (BucketException e) {
            Assert.assertTrue(e.getMessage().contains(pdo.getSamples().get(indexOfBadSample).getName()));
        }
        // Removes the problem sample and tries again.
        pdo.getSamples().remove(indexOfBadSample);
        vessels = bucketEjb.createInitialVessels(pdo.getSamples().stream().
                map(ProductOrderSample::getName).collect(Collectors.toList()), username);

        Assert.assertEquals(vessels.size(), 4);
        Assert.assertTrue(vessels.stream().map(LabVessel::getLabel).anyMatch(s -> s.equals(makeTubeBarcode(1))));
        Assert.assertTrue(vessels.stream().map(LabVessel::getLabel).anyMatch(s -> s.equals(makeTubeBarcode(2))));
        Assert.assertTrue(vessels.stream().map(LabVessel::getLabel).anyMatch(s -> s.equals(makeTubeBarcode(3))));
        Assert.assertTrue(vessels.stream().map(LabVessel::getLabel).anyMatch(s -> s.equals(makeTubeBarcode(4))));
    }

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testBadLabelSamplesToPicoBucket() {
        // Doesn't create tubes or mercury samples and leaves the pdo samples unlinked.
        setup(ICE, false);

        // Updates the bspSampleData to indicate a sample has no BSP data.
        bspSampleDataMap.get(pdo.getSamples().get(2).getName()).getPlasticBarcodes().clear();
        bspSampleDataMap.get(pdo.getSamples().get(2).getName()).getColumnToValue().clear();
        try {
            Collection<LabVessel> vessels = bucketEjb.createInitialVessels(
                    pdo.getSamples().stream().map(ProductOrderSample::getName).collect(Collectors.toList()), username);
            Assert.fail("Should have thrown.");

        } catch (BucketException e) {
            Assert.assertTrue(e.getMessage().contains(pdo.getSamples().get(2).getName()));
        }
    }

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testAddSamplesToBucket() throws Exception {
        for (String workflow : (new String[]{AGILENT_EXOME_EXPRESS, ICE})) {
            setup(workflow, true);

            // Non-genomic material should not be bucketed.
            bspSampleDataMap.get(pdo.getSamples().get(1).getName()).getColumnToValue().
                    put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.FRESH_BLOOD.getDisplayName());

            // Changes the name of the pdo sample to be the vessel barcode. It should still be bucketed.
            pdo.getSamples().get(2).setName(labVessels.get(2).getLabel());

            // A PDO sample without a LabVessel should not be bucketed.
            mercurySamples.get(3).removeSampleFromVessels(Collections.singleton(labVessels.get(3)));

            // A PDO sample without a MercurySample should not be bucketed.
            pdo.getSamples().get(4).setMercurySample(null);

            Mockito.when(labEventFactory.buildFromBatchRequests(Mockito.anyList(), Mockito.anyObject(),
                    Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject(), Mockito.anyObject(),
                    Mockito.anyObject(), Mockito.eq(0))).
                    thenReturn(Collections.emptyList());

            // Given a PDO and new PDO samples, the PDO samples' linked MercurySample tubes will be bucketed,
            // provided the workflow and material types match.
            Triple<Boolean, Collection<String>, Multimap<String, ProductOrderSample>> triple =
                    bucketEjb.addSamplesToBucket(pdo, pdo.getSamples(), "Mercury",
                            ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION);

            // There is a valid workflow.
            Assert.assertTrue(triple.getLeft());

            // Two PDO samples are not linked to a MercurySample and LabVessel
            Assert.assertEquals(triple.getMiddle().size(), 2);
            Assert.assertTrue(triple.getMiddle().contains(pdo.getSamples().get(3).getName()));
            Assert.assertTrue(triple.getMiddle().contains(pdo.getSamples().get(4).getName()));

            Assert.assertEquals(triple.getRight().keySet().iterator().next(), "Pico/Plating Bucket");
            Assert.assertEquals(triple.getRight().values().size(), 2);
            Assert.assertTrue(triple.getRight().values().contains(pdo.getSamples().get(0)));
            Assert.assertTrue(triple.getRight().values().contains(pdo.getSamples().get(2)));
        }
    }


    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testApplyBucketCriteriaNoWorkflow(){
        setup(AGILENT_EXOME_EXPRESS, true);
        pdo.getProduct().setWorkflowName(Workflow.NONE);

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                bucketEjb.applyBucketCriteria(labVessels, pdo, "whatever",
                        ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Collection<BucketEntry> bucketEntries = workflowBucketEntriesPair.getRight();
        Assert.assertTrue(bucketEntries.isEmpty());
    }

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testDuplicateSamplesToPicoBucket() throws Exception {
        String workflow = AGILENT_EXOME_EXPRESS;
        setup(workflow, true);

        Mockito.when(bspSampleDataFetcher.fetchSampleData(Mockito.anyCollection())).thenReturn(bspSampleDataMap);

        ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflow(AGILENT_EXOME_EXPRESS);

        WorkflowBucketDef picoBucket = workflowDef.getEffectiveVersion().findBucketDefByName("Pico/Plating Bucket");

        Map<WorkflowBucketDef, Collection<LabVessel>> newBucketEntry = new HashMap<>();
        LabVessel labVessel = labVessels.get(1);
        newBucketEntry.put(picoBucket, Collections.singleton(labVessel));

        Collection<BucketEntry> bucketEntries = bucketEjb
                .add(newBucketEntry, BucketEntry.BucketEntryType.PDO_ENTRY, LabEvent.UI_PROGRAM_NAME, "seinfeld",
                        LabEvent.UI_EVENT_LOCATION, pdo, new Date());
        Assert.assertEquals(bucketEntries.size(), 1);

        bucketEntries = bucketEjb
                .add(newBucketEntry, BucketEntry.BucketEntryType.PDO_ENTRY, LabEvent.UI_PROGRAM_NAME, "seinfeld",
                        LabEvent.UI_EVENT_LOCATION, pdo, new Date());
        Assert.assertTrue(bucketEntries.isEmpty());
    }

    private String makeTubeBarcode(int rackPosition) {
        return "R" + rackPosition;
    }

}
