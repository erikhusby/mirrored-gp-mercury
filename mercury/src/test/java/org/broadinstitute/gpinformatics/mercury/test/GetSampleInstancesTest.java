package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Test LabVessel.getSampleInstances
 */
public class GetSampleInstancesTest {
    @Test
    public void testBasic() {
        // sample initiation PDO
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(2, "PDO-SI", Workflow.ICE, 101L,
                "Test research project", "Test research project", false, "SamInit", "1");

        // receive samples
        Set<LabVessel> starterVessels = new HashSet<>();
        int i = 1;
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            TwoDBarcodedTube tube = new TwoDBarcodedTube("R" + i);
            tube.addSample(new MercurySample(productOrderSample.getSampleKey()));
            starterVessels.add(tube);
            i++;
        }
        LabBatch receiptBatch = new LabBatch("SK-1", starterVessels, LabBatch.LabBatchType.SAMPLES_RECEIPT);

        // extraction PDO
        Product extractionProduct = ProductTestFactory.createDummyProduct(Workflow.ICE, "EXTR-01");
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            productOrderSamples.add(new ProductOrderSample(productOrderSample.getSampleKey()));
        }
        new ProductOrder(101L, "Extraction PDO", productOrderSamples, "SEQ-01", extractionProduct,
                productOrder.getResearchProject());
        // extraction set?
        LabEvent labEvent = new LabEvent(LabEventType.SAMPLES_EXTRACTION_END_TRANSFER, new Date(), "SUPERMAN", 1L,
                101L, "Bravo");

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToSourceTube = new HashMap<>();
        Iterator<LabVessel> iterator = starterVessels.iterator();
        mapPositionToSourceTube.put(VesselPosition.A01, (TwoDBarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A02, (TwoDBarcodedTube) iterator.next());
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToSourceTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTargetTube = new HashMap<>();
        mapPositionToTargetTube.put(VesselPosition.A01, new TwoDBarcodedTube("X1"));
        mapPositionToTargetTube.put(VesselPosition.A02, new TwoDBarcodedTube("X2"));
        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToTargetTube, RackOfTubes.RackType.Matrix96);

        labEvent.getSectionTransfers().add(new SectionTransfer(sourceTubeFormation.getContainerRole(), SBSSection.ALL96,
                targetTubeFormation.getContainerRole(), SBSSection.ALL96, labEvent));

        // sequencing PDO
        // LCSET

        // re-array to add control
        // Import
        // P7 index
        // P5 index
        // bait
    }
}
