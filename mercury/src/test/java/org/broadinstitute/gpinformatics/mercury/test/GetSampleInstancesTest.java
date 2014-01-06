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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test LabVessel.getSampleInstances
 */
public class GetSampleInstancesTest {
    @Test
    public void testBasic() {
        // sample initiation PDO
        ProductOrder sampleInitProductOrder = ProductOrderTestFactory.createDummyProductOrder(2, "PDO-SI",
                Workflow.ICE, 101L, "Test research project", "Test research project", false, "SamInit", "1");

        // receive samples
        Set<LabVessel> starterVessels = new HashSet<>();
        int i = 1;
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            TwoDBarcodedTube tube = new TwoDBarcodedTube("R" + i);
            tube.addSample(new MercurySample(productOrderSample.getSampleKey()));
            starterVessels.add(tube);
            i++;
        }
        LabBatch receiptBatch = new LabBatch("SK-1", starterVessels, LabBatch.LabBatchType.SAMPLES_RECEIPT);

        // extraction PDO
        Product extractionProduct = ProductTestFactory.createDummyProduct(Workflow.ICE, "EXTR-01");
        List<ProductOrderSample> extractionProductOrderSamples = new ArrayList<>();
        for (ProductOrderSample productOrderSample : sampleInitProductOrder.getSamples()) {
            extractionProductOrderSamples.add(new ProductOrderSample(productOrderSample.getSampleKey()));
        }
        ProductOrder extractionProductOrder = new ProductOrder(101L, "Extraction PDO", extractionProductOrderSamples,
                "SEQ-01", extractionProduct, sampleInitProductOrder.getResearchProject());
        // extraction set?
        LabEvent labEvent = new LabEvent(LabEventType.SAMPLES_EXTRACTION_END_TRANSFER, new Date(), "SUPERMAN", 1L,
                101L, "Bravo");

        // Extraction transfer
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToSourceTube = new HashMap<>();
        Iterator<LabVessel> iterator = starterVessels.iterator();
        mapPositionToSourceTube.put(VesselPosition.A01, (TwoDBarcodedTube) iterator.next());
        mapPositionToSourceTube.put(VesselPosition.A02, (TwoDBarcodedTube) iterator.next());
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToSourceTube, RackOfTubes.RackType.Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTube = new HashMap<>();
        TwoDBarcodedTube tube1 = new TwoDBarcodedTube("X1");
        tube1.addSample(new MercurySample("SM-X1"));
        mapPositionToExtractTube.put(VesselPosition.A01, tube1);
        TwoDBarcodedTube tube2 = new TwoDBarcodedTube("X2");
        tube2.addSample(new MercurySample("SM-X2"));
        mapPositionToExtractTube.put(VesselPosition.A02, tube2);
        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToExtractTube, RackOfTubes.RackType.Matrix96);

        labEvent.getSectionTransfers().add(new SectionTransfer(sourceTubeFormation.getContainerRole(), SBSSection.ALL96,
                targetTubeFormation.getContainerRole(), SBSSection.ALL96, labEvent));

        // sequencing PDO
        Product sequencingProduct = ProductTestFactory.createDummyProduct(Workflow.HYBRID_SELECTION, "HYBSEL-01");
        List<ProductOrderSample> sequencingProductOrderSamples = new ArrayList<>();
        for (TwoDBarcodedTube twoDBarcodedTube : mapPositionToExtractTube.values()) {
            sequencingProductOrderSamples.add(
                    new ProductOrderSample(twoDBarcodedTube.getMercurySamples().iterator().next().getSampleKey()));
        }

        ProductOrder sequencingProductOrder = new ProductOrder(101L, "Sequencing PDO", sequencingProductOrderSamples,
                "SEQ-01", sequencingProduct, sampleInitProductOrder.getResearchProject());
        // LCSET

        // re-array to add control
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToExtractTubeControl = new HashMap<>(mapPositionToExtractTube);

        TwoDBarcodedTube controlTube = new TwoDBarcodedTube("CONTROL");
        controlTube.addSample(new MercurySample("SM-C"));
        mapPositionToExtractTubeControl.put(VesselPosition.A03, controlTube);
        // Import
        LabBatch importLabBatch = new LabBatch("EX-1", new HashSet<LabVessel>(mapPositionToExtractTubeControl.values()),
                LabBatch.LabBatchType.SAMPLES_IMPORT);

        new TubeFormation(mapPositionToExtractTubeControl, RackOfTubes.RackType.Matrix96);
        // P7 index
        // P5 index
        // bait
    }
}
