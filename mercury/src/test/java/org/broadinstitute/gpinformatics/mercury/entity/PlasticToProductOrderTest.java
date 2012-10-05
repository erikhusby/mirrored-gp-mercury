package org.broadinstitute.gpinformatics.mercury.entity;


import static junit.framework.Assert.*;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.StandardPOResolver;
import org.broadinstitute.gpinformatics.mercury.control.dao.person.PersonDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.testng.annotations.Test;

import java.util.*;

public class PlasticToProductOrderTest {


    @Test(groups = {TestGroups.DATABASE_FREE})
    public void test_simple_tube_in_rack_maps_to_pdo() {
        BettaLimsMessageFactory messageFactory = new BettaLimsMessageFactory();
        LabEventFactory eventFactory = new LabEventFactory(new PersonDAO());
        Map<String,TwoDBarcodedTube> barcodeToTubeMap = new HashMap<String, TwoDBarcodedTube>();
        String destinationPlateBarcode = "Plate0000";
        barcodeToTubeMap.put("0000",new TwoDBarcodedTube("0000"));


        PlateTransferEventType plateXfer = messageFactory.buildRackToPlate(LabEventType.POND_REGISTRATION.getName(),"RackBarcode",barcodeToTubeMap.keySet(),destinationPlateBarcode);


        LabEvent rackToPlateTransfer = eventFactory.buildFromBettaLimsRackToPlateDbFree(plateXfer, barcodeToTubeMap, null);

        ProductOrderId productOrder = new ProductOrderId("PO1");

        // create product order...use boundary that athena uses to push a PO into mercury
        final Map<LabVessel,ProductOrderId> samplePOMapFromAthena = new HashMap<LabVessel, ProductOrderId>();

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
