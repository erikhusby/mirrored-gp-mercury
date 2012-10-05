package org.broadinstitute.gpinformatics.mercury.entity;


import static junit.framework.Assert.*;

import org.broadinstitute.gpinformatics.infrastructure.StubSampleMetadata;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.StandardPOResolver;
import org.broadinstitute.gpinformatics.mercury.control.dao.person.PersonDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.infrastructure.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.testng.annotations.Test;

import java.util.*;

public class PlasticToProductOrderTest {


    @Test(groups = {TestGroups.DATABASE_FREE})
    public void test_simple_tube_in_rack_maps_to_pdo() {
        Set<SampleMetadata> rootSamples = new HashSet<SampleMetadata>();
        rootSamples.add(new StubSampleMetadata("TCGA123","Human",null));
        BettaLimsMessageFactory messageFactory = new BettaLimsMessageFactory();
        LabEventFactory eventFactory = new LabEventFactory(new PersonDAO());
        Map<String,TwoDBarcodedTube> barcodeToTubeMap = new HashMap<String, TwoDBarcodedTube>();
        String destinationPlateBarcode = "Plate0000";
        TwoDBarcodedTube tube = new TwoDBarcodedTube("0000");
        tube.setSamples(rootSamples);
        barcodeToTubeMap.put(tube.getLabel(),tube);


        PlateTransferEventType plateXfer = messageFactory.buildRackToPlate(LabEventType.POND_REGISTRATION.getName(),"RackBarcode",barcodeToTubeMap.keySet(),destinationPlateBarcode);


        LabEvent rackToPlateTransfer = eventFactory.buildFromBettaLimsRackToPlateDbFree(plateXfer, barcodeToTubeMap, null);

        ProductOrderId productOrder = new ProductOrderId("PO1");

        // create product order...in reality, use boundary that athena uses to push a PO into mercury
        final Map<LabVessel,ProductOrderId> samplePOMapFromAthena = new HashMap<LabVessel, ProductOrderId>();
        samplePOMapFromAthena.put(tube,productOrder);

        // pull tubes from bucket: this creates a LabEvent for every
        // container pulled from the bucket, and the LabEvent calls out
        // a PDO for each sample
        rackToPlateTransfer.setProductOrderId(productOrder);

        // put the tubes in a rack

        // transfer from a rack to a plate

        // ask the plate for the PDOs associated with each well on the plate.

        StandardPOResolver poResolver = new StandardPOResolver();


        assertEquals(1, rackToPlateTransfer.getTargetLabVessels().size());
        assertEquals(1, rackToPlateTransfer.getSourceLabVessels().size());

        LabVessel targetVessel = rackToPlateTransfer.getTargetLabVessels().iterator().next();
        targetVessel.setChainOfCustodyRoots(barcodeToTubeMap.values());
        targetVessel.setSamples(rootSamples); // in reality we wouldn't set the samples list; it would be derived from a history walk
        // todo I'm setting this on the rack here, but it should just work on the tube
        LabVessel source = rackToPlateTransfer.getSourceLabVessels().iterator().next();
        source.setChainOfCustodyRoots(barcodeToTubeMap.values());
        source.setSamples(rootSamples); // in reality we wouldn't set the samples list; it would be derived from a history walk

        Map<LabVessel,ProductOrderId> vesselPOMap = poResolver.findProductOrders(targetVessel);

        for (Map.Entry<LabVessel, ProductOrderId> labVesselPO : vesselPOMap.entrySet()) {
            assertTrue(samplePOMapFromAthena.containsKey(labVesselPO.getKey())); // make sure that the LabVessel called out in the PO
                                                                                 // is found by the resolver
            Set<SampleMetadata> samples = labVesselPO.getKey().getSamples();

            for (LabVessel athenaProductOrderLabVessel : samplePOMapFromAthena.keySet()) {
                // athenaProductOrderLabVessel is the mercury LabVessel created when BSP pushes the sample
                // over to mercury during sample receipt
                for (SampleMetadata athenaRootSample : athenaProductOrderLabVessel.getSamples()) {
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

}
