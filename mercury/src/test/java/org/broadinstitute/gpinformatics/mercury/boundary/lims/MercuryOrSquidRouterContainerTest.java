package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;

/**
 * @author Scott Matthews
 *         Date: 2/7/13
 *         Time: 11:10 PM
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class MercuryOrSquidRouterContainerTest extends StubbyContainerTest {

    public MercuryOrSquidRouterContainerTest(){}

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    UserTransaction utx;

    @Inject
    private ProductOrderDao poDao;

    @Inject
    private LabVesselDao vesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    private ProductOrder testExExOrder;

    private String exExJiraKey;
    private String testPrefix;
    private String ligationBarcode;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        if (bettaLimsMessageResource == null) {
            return;
        }

        utx.begin();

        testExExOrder = poDao.findByWorkflow(Workflow.AGILENT_EXOME_EXPRESS).iterator().next();
        exExJiraKey = testExExOrder.getBusinessKey();

        testPrefix = "bcode";
        ligationBarcode = "ligationPlate" + testPrefix;

        StaticPlate ligationPlate = LabEventTest.buildIndexPlate(molecularIndexingSchemeDao, molecularIndexDao,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList(ligationBarcode)).get(0);
        vesselDao.persist(ligationPlate);
        vesselDao.flush();
        vesselDao.clear();

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

    }

    @Test(groups = TestGroups.STUBBY)
    public void testExomeExpressEvents() {
        testExExOrder = poDao.findByBusinessKey(exExJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(testExExOrder, "Shearing Bucket");

        String rackBarcode = "REXEX" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =

                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));
        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().add(shearingEventJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String xmlMessage = BettaLimsMessageTestFactory.marshal(msg);

        bettaLimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());
        Assert.assertNotNull(shearPlate);

        // Adapter Ligation (Plate to Plate -- where the the Target is the item to validate, not the source)
        PlateTransferEventType ligationCleanupJaxb =
                bettaLimsMessageTestFactory
                        .buildPlateToPlate(LabEventType.INDEXED_ADAPTER_LIGATION.getName(), ligationBarcode,
                                shearingEventJaxb.getPlate().getBarcode());

        BettaLIMSMessage msgLigate = new BettaLIMSMessage();
        msgLigate.getPlateTransferEvent().add(ligationCleanupJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String ligateXmlMessage = BettaLimsMessageTestFactory.marshal(msgLigate);

        bettaLimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate ligatedPlate =
                (StaticPlate) vesselDao.findByIdentifier(ligationCleanupJaxb.getPlate().getBarcode());
        Assert.assertNotNull(ligatedPlate);
    }


    /*
       Helper methods
    */

    /**
     * Assists in creating a set of Mercury Tubes based on an existing Product order.  This method will also, if need
     * be, add the created mercury vessels to a given Bucket to assist
     * <p/>
     * TODO  this could probably be broken up better to define an alternate method to make cleaner
     *
     * @param productOrder Product order by which to determine the Samples to Create.
     * @param bucketName   Optional:  Name of the Bucket to create.  If left null, bucket creation will be avoided
     * @return
     */
    private Map<String, BarcodedTube> buildVesselsForPdo(@Nonnull ProductOrder productOrder,
                                                         @Nullable String bucketName) {

        Date testSuffix = new Date();
        Set<LabVessel> tubes = new HashSet<>(productOrder.getTotalSampleCount());

        List<String> barcodes = new ArrayList<>(productOrder.getTotalSampleCount());

        Bucket bucket = null;
        if (bucketName != null) {
            bucket = bucketDao.findByName(bucketName);
            if (bucket == null) {
                bucket = new Bucket(new WorkflowBucketDef(bucketName));
            }
        }

        for (ProductOrderSample currSample : productOrder.getSamples()) {

            BarcodedTube newTube = (BarcodedTube) vesselDao.findByIdentifier("R" + currSample.getName());
            if (newTube == null) {
                newTube = new BarcodedTube("R" + currSample.getName());
            }

            MercurySample mercurySample = mercurySampleDao.findBySampleKey(currSample.getName());

            if(mercurySample == null) {
                mercurySample = new MercurySample(currSample.getName(), MercurySample.MetadataSource.BSP);
            }
            newTube.addSample(mercurySample);
            tubes.add(newTube);
            barcodes.add(newTube.getLabel());
            if (bucketName != null && bucket.findEntry(newTube) == null) {
                bucket.addEntry(productOrder, newTube,
                        org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry.BucketEntryType.PDO_ENTRY);
            }
        }


        if (bucketName != null) {
            //This will automatically add the batch to the tubes via the internal set methods
            LabBatch labBatch = new LabBatch("testBatch" + testSuffix.getTime(), tubes, LabBatch.LabBatchType.WORKFLOW);
            labBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

            for (LabVessel currTube : tubes) {
                for (BucketEntry currentEntry : currTube.getBucketEntries()) {
                    currentEntry.setStatus(BucketEntry.Status.Archived);
                    currentEntry.setLabBatch(labBatch);
                }
            }
        }

        Map<String, BarcodedTube> mapBarcodeToTube =
                new LinkedHashMap<>(productOrder.getTotalSampleCount());

        if (bucketName != null) {
            bucketDao.persist(bucket);
            bucketDao.flush();
            bucketDao.clear();
        } else {
            vesselDao.persistAll(new ArrayList<Object>(tubes));
            vesselDao.flush();
            vesselDao.clear();
        }

        for (String barcode : barcodes) {
            LabVessel foundTube = vesselDao.findByIdentifier(barcode);
            mapBarcodeToTube.put(barcode, (BarcodedTube) foundTube);
        }
        return mapBarcodeToTube;
    }

}
