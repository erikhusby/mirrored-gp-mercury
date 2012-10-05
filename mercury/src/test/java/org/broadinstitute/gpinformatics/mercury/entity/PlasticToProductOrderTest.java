package org.broadinstitute.gpinformatics.mercury.entity;


import static junit.framework.Assert.*;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.jboss.weld.util.collections.ArraySet;
import org.testng.annotations.Test;

import java.util.*;

public class PlasticToProductOrderTest {

    @Test(enabled = false)
    public void test_simple_tube_in_rack_maps_to_pdo() {
        int numTubes = 5;
        RackOfTubes sourceRackOfTubes;
        StaticPlate destinationPlate;
        LabEvent transferEvent;

        // create product order...use boundary that athena uses to push a PO into mercury
        final Map<LabVessel,ProductOrderId> samplePOMapFromAthena = new HashMap<LabVessel, ProductOrderId>();

        // pull tubes from bucket: this creates a LabEvent for every
        // container pulled from the bucket, and the LabEvent calls out
        // a PDO for each sample
        transferEvent.setProductOrderId();

        // put the tubes in a rack

        // transfer from a rack to a plate

        // ask the plate for the PDOs associated with each well on the plate.

        StandardPOResolver poResolver = new StandardPOResolver();


        Map<LabVessel,ProductOrderId> vesselPOMap = poResolver.findProductOrders(destinationPlate);

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
