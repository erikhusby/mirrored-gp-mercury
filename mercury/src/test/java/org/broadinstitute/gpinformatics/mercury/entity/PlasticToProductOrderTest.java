package org.broadinstitute.gpinformatics.mercury.entity;


import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.StandardPOResolver;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

@Test(groups = {TestGroups.EXTERNAL_INTEGRATION})
public class PlasticToProductOrderTest extends ContainerTest {

    @Inject
    private BucketDao bucketDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketBean bucketResource;

    @Inject
    private TwoDBarcodedTubeDAO tubeDAO;

    @Inject
    private TubeFormationDao rackDAO;

    @Inject
    private StaticPlateDAO plateDAO;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private LabEventFactory eventFactory;

    @Inject
    private UserTransaction utx;



    private static final String PRODUCT_ORDER_KEY = "PDO-1";
    public static final String BUCKET_REFERENCE_NAME = "Start";
    private Bucket bucket;
    private String tubeBarcode;
    private WorkflowBucketDef bucketDef;

    @BeforeMethod ( groups = TestGroups.EXTERNAL_INTEGRATION )
    public void setUp () throws Exception {
        // Skip if no injections, meaning we're not running in container
        if ( utx == null ) {
            return;
        }
        utx.begin ();

        bucketDef = new WorkflowBucketDef (BUCKET_REFERENCE_NAME);

        bucket = new Bucket (bucketDef);
        bucketDao.persist(bucket);
        bucketDao.flush ();
        bucketDao.clear();

    }

    @AfterMethod ( groups = TestGroups.EXTERNAL_INTEGRATION )
    public void tearDown () throws Exception {
        // Skip if no injections, meaning we're not running in container
        if ( utx == null ) {
            return;
        }

        utx.rollback ();
    }

    public void test_simple_tube_in_rack_maps_to_pdo() {

        bucket = bucketDao.findByName(BUCKET_REFERENCE_NAME);

        BettaLimsMessageFactory messageFactory = new BettaLimsMessageFactory();
//        LabEventFactory eventFactory = new LabEventFactory(new BSPUserList(new BSPManagerFactory () {
//                    @Override
//                    public WorkRequestManager createWorkRequestManager () {
//                        return null;  //To change body of implemented methods use File | Settings | File Templates.
//                    }
//
//                    @Override
//                    public ContainerManager createContainerManager () {
//                        return null;  //To change body of implemented methods use File | Settings | File Templates.
//                    }
//
//                    @Override
//                    public UserManager createUserManager () {
//
//                        UserManager mgr = new UserManager () {
//                            @Override
//                            public BspUser get ( String s ) {
//                                return new BSPUserList.QADudeUser("Test", LabEventTest.QA_DUDE_USER_ID);
//                            }
//
//                            @Override
//                            public List<BspUser> getPrimaryInvestigators () {
//                                List<BspUser> testList = new LinkedList<BspUser>();
//                                testList.add(new BSPUserList.QADudeUser("PM", LabEventTest.QA_DUDE_USER_ID+1));
//                                testList.add(new BSPUserList.QADudeUser("PDM", LabEventTest.QA_DUDE_USER_ID+2));
//                                testList.add(new BSPUserList.QADudeUser("LU", LabEventTest.QA_DUDE_USER_ID+3));
//                                testList.add(new BSPUserList.QADudeUser("LM", LabEventTest.QA_DUDE_USER_ID+4));
//                                return testList;
//                            }
//
//                            @Override
//                            public List<BspUser> getProjectManagers () {
//                                List<BspUser> testList = new LinkedList<BspUser>();
//                                testList.add(new BSPUserList.QADudeUser("PM", LabEventTest.QA_DUDE_USER_ID+1));
//                                return testList;
//                            }
//
//                            @Override
//                            public List<BspUser> getUsers () {
//                                List<BspUser> testList = new LinkedList<BspUser>();
//                                testList.add(new BSPUserList.QADudeUser("Test", LabEventTest.QA_DUDE_USER_ID));
//                                testList.add(new BSPUserList.QADudeUser("PM", LabEventTest.QA_DUDE_USER_ID+1));
//                                testList.add(new BSPUserList.QADudeUser("PDM", LabEventTest.QA_DUDE_USER_ID+2));
//                                testList.add(new BSPUserList.QADudeUser("LU", LabEventTest.QA_DUDE_USER_ID+3));
//                                testList.add(new BSPUserList.QADudeUser("LM", LabEventTest.QA_DUDE_USER_ID+4));
//                                BspUser jwalshUsr = new BspUser();
//                                jwalshUsr.setUserId(LabEventTest.QA_DUDE_USER_ID+100);
//                                jwalshUsr.setUsername("jowalsh");
//                                jwalshUsr.setFirstName("John");
//                                jwalshUsr.setLastName("Walsh");
//                                jwalshUsr.setEmail("jowaslh@broadinstitute.com");
//                                testList.add(jwalshUsr);
//
//                                BspUser hrafalUsr = new BspUser();
//                                hrafalUsr.setUserId(LabEventTest.QA_DUDE_USER_ID+100);
//                                hrafalUsr.setUsername("hrafal");
//                                hrafalUsr.setFirstName("Howard");
//                                hrafalUsr.setLastName("Rafal");
//                                hrafalUsr.setEmail("hrafal@broadinstitute.com");
//                                testList.add(hrafalUsr);
//
//                                return testList;
//                            }
//                        };
//
//                        return mgr;  //To change body of implemented methods use File | Settings | File Templates.
//                    }
//                }));

        Set<MercurySample> rootSamples = new HashSet<MercurySample>();
        rootSamples.add(new MercurySample(PRODUCT_ORDER_KEY, "TCGA123")); //StubSampleMetadata("TCGA123","Human",null));
        Map<String,TwoDBarcodedTube> barcodeToTubeMap = new HashMap<String, TwoDBarcodedTube>();
        String destinationPlateBarcode = "Plate0000";
        tubeBarcode = "0000";
        TwoDBarcodedTube tube = new TwoDBarcodedTube( tubeBarcode );
        tube.addAllSamples(rootSamples);
        barcodeToTubeMap.put(tube.getLabel(),tube);


        PlateTransferEventType plateXfer = messageFactory.buildRackToPlate ( LabEventType.POND_REGISTRATION.getName (),
                                                                             "RackBarcode", barcodeToTubeMap.keySet (),
                                                                             destinationPlateBarcode );


        LabEvent rackToPlateTransfer = eventFactory.buildFromBettaLimsRackToPlateDbFree ( plateXfer, barcodeToTubeMap,
                                                                                          null, null );

        BucketEntry bucketEntry = bucketResource.add(tube, PRODUCT_ORDER_KEY, bucket, "hrafal" );

        assertTrue ( bucket.contains ( bucketEntry ) );

        final Map<LabVessel,String> samplePOMapFromAthena = new HashMap<LabVessel, String>();
        samplePOMapFromAthena.put(bucketEntry.getLabVessel(),bucketEntry.getPoBusinessKey ());

        List<BucketEntry> testEntries = new LinkedList<BucketEntry>();
        testEntries.add ( bucketEntry);
        bucketResource.start ( testEntries,"hrafal", LabEvent.UI_EVENT_LOCATION );

        Assert.assertFalse ( tube.getInPlaceEvents ().isEmpty () );
        boolean doesEventHavePDO = false;
        for (LabEvent labEvent : tube.getInPlaceEvents ()) {
            if (labEvent.getProductOrderId()!= null) {
                if (PRODUCT_ORDER_KEY.equals(labEvent.getProductOrderId())) {
                    doesEventHavePDO = true;
                }
            }
            // make sure that adding the vessel to the bucket created
            // a new event and tagged it with the appropriate PDO
            assertTrue(doesEventHavePDO);
        }

        assertTrue ( bucket.getBucketEntries ().isEmpty () );
        // pull tubes from bucket: this creates a LabEvent for every
        // container pulled from the bucket, and the LabEvent calls out
        // a PDO for each sample
        // here we set the order explicitly, but this should be done by bucketResource.start()
        rackToPlateTransfer.setProductOrderId(PRODUCT_ORDER_KEY);

        // put the tubes in a rack

        // transfer from a rack to a plate

        // ask the plate for the PDOs associated with each well on the plate.

        StandardPOResolver poResolver = new StandardPOResolver();


        assertEquals(1, rackToPlateTransfer.getTargetLabVessels().size());
        assertEquals(1, rackToPlateTransfer.getSourceLabVessels().size());

        LabVessel targetVessel = rackToPlateTransfer.getTargetLabVessels().iterator().next();

        targetVessel.setChainOfCustodyRoots(barcodeToTubeMap.values());
        targetVessel.addAllSamples(rootSamples); // in reality we wouldn't set the samples list; it would be derived from a history walk
        // todo I'm setting this on the rack here, but it should just work on the tube
        LabVessel source = rackToPlateTransfer.getSourceLabVessels().iterator().next();

        source.setChainOfCustodyRoots(barcodeToTubeMap.values());
        source.addAllSamples(rootSamples); // in reality we wouldn't set the samples list; it would be derived from a history walk


        Map<LabVessel,String> vesselPOMap = poResolver.findProductOrders(targetVessel);

        for (Map.Entry<LabVessel, String> labVesselPO : vesselPOMap.entrySet()) {
            assertTrue(samplePOMapFromAthena.containsKey(labVesselPO.getKey())); // make sure that the LabVessel called out in the PO
                                                                                 // is found by the resolver
            Set<MercurySample> samples = labVesselPO.getKey().getMercurySamples();

            for (LabVessel athenaProductOrderLabVessel : samplePOMapFromAthena.keySet()) {
                // athenaProductOrderLabVessel is the mercury LabVessel created when BSP pushes the sample
                // over to mercury during sample receipt
                for (MercurySample athenaRootSample : athenaProductOrderLabVessel.getMercurySamples()) {
                    // although we expect a 1-1 between athenaRootSample and athenaProductOrderLabVessel, pre-pooled stuff
                    // can have a many-to-one.
                    assertTrue(samples.contains(athenaRootSample));
                    assertEquals(samplePOMapFromAthena.get(athenaProductOrderLabVessel),labVesselPO.getValue());
                }

                // todo demux class to encapsulate indexing
            }
        }
        // todo lots more asserts: make sure that all LabVessels from the athena PO are found in mercury

        // other tests for more complex scenarios:
        // 1. multiple occurences of the same sample
        // in the bucket for different POs,
        // 2. pooling across 2 or more POs: take two POs, each with one sample,
        // run them through a few transfers, pool them, and then call
        // poResolver.findProductOrders() on the pool and verify that each
        // component sample maps to the right PO.
        // 3. dev
    }

    /**
     * Get some unindexed samples from bsp, then apply indexes for them.
     * Does the PDO mapping find the right thing?
     */
    @Test(enabled = false)
    public void test_incoming_sample_unindexed_index_added_in_workflow() {
        fail();
    }

    /**
     * Gin up some pre-indexed samples, run through an exome
     * workflow and verify that the right PDOs are found.
     */
    @Test(enabled = false)
    public void test_incoming_sample_indexed_already() {
        fail();
    }

    /**
     * Put the same sample into the bucket twice, one for PDO x
     * and one for PDO y.  After they're pooled halfway through
     * and exome workflow, does the code find the right
     * PDO for each sample?
     */
    @Test(enabled = false)
    public void test_sample_in_bucket_for_two_different_pdos() {
        fail();
    }

    /**
     * Put two different samples into a bucket, each with a different
     * PDO.  Pool them in an exome workflow.  Does the code
     * find the right PDO for each sample?
     */
    @Test(enabled = false)
    public void test_pool_across_pdos() {
        fail();
    }

    /**
     * Take an unindexed sample and run it through
     * the exome workflow, adding an index along the way.
     * At a point in the event graph after the indexing,
     * create a {@link LabEvent} that sets a different
     * {@link LabEvent#productOrderId}, and then apply some
     * events and transfers after that.  The branch
     * below this is considered a dev branch.
     *
     * Does the code map the stuff on the dev branch
     * to the dev PDO?  Does the stuff on the earlier
     * production branch map to the production PDO?
     */
    @Test(enabled = false)
    public void test_development_branch() {
        fail();
    }

    /**
     * Given 3 samples from 3 different PDOs, run
     * a workflow that pools two of them.  Then
     * do a few transfers and add the third
     * sample to the pool at a later step.
     * Does the pool find the right PDO for each
     * of the 3 samples?  For an interim vessel
     * that contains only two samples, does the
     * code find the right 2 samples and map them
     * to the right PDOs?
     */
    @Test(enabled = false)
    public void test_multistep_pooling() {
        fail();
    }




}
