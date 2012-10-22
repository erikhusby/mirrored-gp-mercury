package org.broadinstitute.gpinformatics.mercury.entity;


import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.StandardPOResolver;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.person.PersonDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.*;

public class PlasticToProductOrderTest {


    @Test(groups = {TestGroups.DATABASE_FREE},enabled = false)
    public void test_simple_tube_in_rack_maps_to_pdo() {
        Bucket bucket = new Bucket();
        BucketResource bucketResource = new BucketResource();
        BettaLimsMessageFactory messageFactory = new BettaLimsMessageFactory();
        LabEventFactory eventFactory = new LabEventFactory(new PersonDAO());

        Set<MercurySample> rootSamples = new HashSet<MercurySample>();
        rootSamples.add(new MercurySample("PO1", "TCGA123")); //StubSampleMetadata("TCGA123","Human",null));
        Map<String,TwoDBarcodedTube> barcodeToTubeMap = new HashMap<String, TwoDBarcodedTube>();
        String destinationPlateBarcode = "Plate0000";
        TwoDBarcodedTube tube = new TwoDBarcodedTube("0000");
        tube.addAllSamples(rootSamples);
        barcodeToTubeMap.put(tube.getLabel(),tube);


        PlateTransferEventType plateXfer = messageFactory.buildRackToPlate(LabEventType.POND_REGISTRATION.getName(),"RackBarcode",barcodeToTubeMap.keySet(),destinationPlateBarcode);


        LabEvent rackToPlateTransfer = eventFactory.buildFromBettaLimsRackToPlateDbFree(plateXfer, barcodeToTubeMap, null);

        ProductOrderId productOrder = new ProductOrderId("PO1");

        BucketEntry bucketEntry = bucketResource.add(tube, productOrder,bucket);
        assertTrue(bucket.contains(bucketEntry));

        final Map<LabVessel,ProductOrderId> samplePOMapFromAthena = new HashMap<LabVessel, ProductOrderId>();
        samplePOMapFromAthena.put(bucketEntry.getLabVessel(),bucketEntry.getProductOrderId());

        bucketResource.start(bucketEntry);

        boolean doesEventHavePDO = false;
        for (LabEvent labEvent : bucketEntry.getLabVessel().getEvents()) {
            if (labEvent.getProductOrderId()!= null) {
                if (productOrder.equals(labEvent.getProductOrderId())) {
                    doesEventHavePDO = true;
                }
            }
            // make sure that adding the vessel to the bucket created
            // a new event and tagged it with the appropriate PDO
            assertTrue(doesEventHavePDO);
        }

        assertFalse(bucket.contains(bucketEntry));
        // pull tubes from bucket: this creates a LabEvent for every
        // container pulled from the bucket, and the LabEvent calls out
        // a PDO for each sample
        // here we set the order explicitly, but this should be done by bucketResource.start()
        rackToPlateTransfer.setProductOrderId(productOrder);

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

        Map<LabVessel,ProductOrderId> vesselPOMap = poResolver.findProductOrders(targetVessel);

        for (Map.Entry<LabVessel, ProductOrderId> labVesselPO : vesselPOMap.entrySet()) {
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
     * {@link LabEvent#productOrder}, and then apply some
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
