package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dao Free test of receiving samples from BSP
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleReceiptResourceTest {

    public static final String BARCODE1 = "1234_";
    public static final String BARCODE2 = "2345_";
    public static final String SAMPLE1 = "SM-1234-";
    public static final String SAMPLE2 = "SM-2345-";

    @Test
    public void testReceiveTubesNoPdo() {
        SampleReceiptResource sampleReceiptResource = new SampleReceiptResource();
        SampleReceiptBean sampleReceiptBean = buildTubes("");

        List<LabVessel> labVessels = sampleReceiptResource.notifyOfReceiptDaoFree(sampleReceiptBean,
                new HashMap<String, LabVessel>(), new HashMap<String, List<MercurySample>>(),
                new HashMap<String, List<ProductOrderSample>>());

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getStartingSample();
        Assert.assertEquals(mercurySample.getSampleKey(), SAMPLE1, "Wrong sample");
        Assert.assertNull(mercurySample.getProductOrderKey(), "Unexpected PDO");
    }

    @Test
    public void testReceiveTubesPdo() {
        SampleReceiptResource sampleReceiptResource = new SampleReceiptResource();
        SampleReceiptBean sampleReceiptBean = buildTubes("");

        Map<String, List<ProductOrderSample>> mapIdToListPdoSamples = new HashMap<String, List<ProductOrderSample>>();
        ProductOrderSample productOrderSample = new ProductOrderSample(SAMPLE1);
        ProductOrder productOrder = new ProductOrder();
        String pdoKey = "PDO-1234";
        productOrder.setJiraTicketKey(pdoKey);
        productOrder.addSamples(Collections.singletonList(productOrderSample));
        mapIdToListPdoSamples.put(SAMPLE1, Collections.singletonList(productOrderSample));

        List<LabVessel> labVessels = sampleReceiptResource.notifyOfReceiptDaoFree(sampleReceiptBean,
                new HashMap<String, LabVessel>(), new HashMap<String, List<MercurySample>>(),
                mapIdToListPdoSamples);

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getStartingSample();
        Assert.assertEquals(mercurySample.getSampleKey(), SAMPLE1, "Wrong sample");
        Assert.assertEquals(mercurySample.getProductOrderKey(), pdoKey, "Wrong PDO");
    }

    public static SampleReceiptBean buildTubes(String date) {
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        parentVesselBeans.add(new ParentVesselBean(BARCODE1 + date, SAMPLE1 + date, "Matrix Tube [0.75mL]", null));
        parentVesselBeans.add(new ParentVesselBean(BARCODE2 + date, SAMPLE2 + date, "Matrix Tube [0.75mL]", null));
        return new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans);
    }

    @Test
    public void testReceivePlates() {
        SampleReceiptResource sampleReceiptResource = new SampleReceiptResource();
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<ChildVesselBean>();
        String sampleId1 = "SM-1234";
        childVesselBeans.add(new ChildVesselBean(null, sampleId1, "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345", "Well [200uL]", "A02"));
        parentVesselBeans.add(new ParentVesselBean("P1234", null, "Plate 96 Well PCR [200ul]", childVesselBeans));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans);

        List<LabVessel> labVessels = sampleReceiptResource.notifyOfReceiptDaoFree(sampleReceiptBean,
                new HashMap<String, LabVessel>(), new HashMap<String, List<MercurySample>>(),
                new HashMap<String, List<ProductOrderSample>>());
        Assert.assertEquals(labVessels.size(), 1, "Wrong number of vessels");
        StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
        Assert.assertEquals(staticPlate.getContainerRole().getMapPositionToVessel().size(), 2, "Wrong number of child vessels");
        PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstance> sampleInstances = plateWell.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        Assert.assertEquals(sampleInstances.iterator().next().getStartingSample().getSampleKey(), sampleId1, "Wrong sample");
    }
}
