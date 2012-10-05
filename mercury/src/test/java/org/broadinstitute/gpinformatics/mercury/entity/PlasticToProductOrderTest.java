package org.broadinstitute.gpinformatics.mercury.entity;


import static junit.framework.Assert.*;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class PlasticToProductOrderTest {

    @Test(enabled = false)
    public void test_simple_tube_in_rack_maps_to_pdo() {
        int numTubes = 5;
        RackOfTubes sourceRackOfTubes;
        StaticPlate destinationPlate;

        // create product order
        Collection<ProductOrderId> productOrderIdsReturnedFromAthena = new ArrayList<ProductOrderId>();

        // pull tubes from bucket: this creates a LabEvent for every
        // container pulled from the bucket, and the LabEvent calls out
        // a PDO for each sample

        // put the tubes in a rack

        // transfer from a rack to a plate

        // ask the plate for the PDOs associated with each well on the plate.

        StandardPOResolver poResolver = new StandardPOResolver();


        int rootCount = 0;
        for (LabVessel root : destinationPlate.getChainOfCustodyRoots()) {
            rootCount++;
            Set<ProductOrderId> productOrders = poResolver.findProductOrders(root);
            assertEquals(productOrderIdsReturnedFromAthena.size(),productOrders.size());

            for (ProductOrderId productOrder : productOrders) {
                assertTrue(productOrderIdsReturnedFromAthena.contains(productOrder));
            }

        }

        for (LabEvent labEvent : destinationPlate.getEvents()) {

        }


        assertEquals(numTubes,rootCount);
       /*
       0. trash project plan and starting sample.  what's left in SampleInstance?
        */

    }
}
