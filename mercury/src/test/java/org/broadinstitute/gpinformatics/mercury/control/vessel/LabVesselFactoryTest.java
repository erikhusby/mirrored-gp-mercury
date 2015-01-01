package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptBean;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
 * Dao Free test of creating lab vessels from web service DTOs
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LabVesselFactoryTest {

    public static final String BARCODE1 = "1234_";
    public static final String BARCODE2 = "2345_";
    public static final String SAMPLE1 = "SM-1234-";
    public static final String SAMPLE2 = "SM-2345-";

    @Test
    public void testReceiveTubesNoPdo() {
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        SampleReceiptBean sampleReceiptBean = buildTubes("");

        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP);

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getStartingSample();
        Assert.assertEquals(mercurySample.getSampleKey(), SAMPLE1, "Wrong sample");
    }

    @Test
    public void testReceiveTubesTestAlternativeTubeType() {

        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        long date = System.currentTimeMillis();
        parentVesselBeans.add(new ParentVesselBean(BARCODE1 + date, SAMPLE1 + date,
                "Vacutainer EDTA Tube Purple-Top [3mL]", null));
        parentVesselBeans.add(new ParentVesselBean(BARCODE2 + date, SAMPLE2 + date,
                "VacutainerBloodTubeEDTA_3", null));
        SampleReceiptBean sampleReceiptBean =  new SampleReceiptBean(new Date(), "SK-123-" + date, parentVesselBeans,
                "jowalsh");

        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP);

        Assert.assertEquals(((BarcodedTube)labVessels.get(0)).getTubeType(),
                BarcodedTube.BarcodedTubeType.VacutainerBloodTubeEDTA_3);
        Assert.assertEquals(((BarcodedTube)labVessels.get(1)).getTubeType(),
                BarcodedTube.BarcodedTubeType.VacutainerBloodTubeEDTA_3);
    }

    @Test
    public void testReceiveTubesPdo() {
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        SampleReceiptBean sampleReceiptBean = buildTubes("");

        Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples = new HashMap<>();
        ProductOrderSample productOrderSample = new ProductOrderSample(SAMPLE1);
        ProductOrder productOrder = new ProductOrder();
        String pdoKey = "PDO-1234";
        productOrder.setJiraTicketKey(pdoKey);
        productOrder.addSamples(Collections.singletonList(productOrderSample));
        mapIdToListPdoSamples.put(SAMPLE1, Collections.singleton(productOrderSample));

        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                mapIdToListPdoSamples, sampleReceiptBean.getReceivingUserName(), sampleReceiptBean.getReceiptDate(),
                sampleReceiptBean.getParentVesselBeans(), LabEventType.SAMPLE_RECEIPT,
                MercurySample.MetadataSource.BSP);

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstance> sampleInstances = labVessel.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getStartingSample();
        Assert.assertEquals(mercurySample.getSampleKey(), SAMPLE1, "Wrong sample");
    }

    public static SampleReceiptBean buildTubes(String date) {
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans.add(new ParentVesselBean(BARCODE1 + date, SAMPLE1 + date, "Matrix Tube [0.75mL]", null));
        parentVesselBeans.add(new ParentVesselBean(BARCODE2 + date, SAMPLE2 + date, "Matrix Tube [0.75mL]", null));
        return new SampleReceiptBean(new Date(), "SK-123-" + date, parentVesselBeans, "jowalsh");
    }

    @Test
    public void testReceivePlates() {
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        String sampleId1 = "SM-1234";
        childVesselBeans.add(new ChildVesselBean(null, sampleId1, "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345", "Well [200uL]", "A02"));
        parentVesselBeans.add(new ParentVesselBean("P1234", null, "Plate 96 Well PCR [200ul]", childVesselBeans));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans, "jowalsh");

        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP);
        Assert.assertEquals(labVessels.size(), 1, "Wrong number of vessels");
        StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
        Assert.assertEquals(staticPlate.getContainerRole().getMapPositionToVessel().size(), 2,
                "Wrong number of child vessels");
        PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstance> sampleInstances = plateWell.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        Assert.assertEquals(sampleInstances.iterator().next().getStartingSample().getSampleKey(), sampleId1, "Wrong sample");
    }

    @Test
    public void testReceivePlatesTestPlateType() {
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans.add(new ChildVesselBean(null, "SM-1234", "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345", "Well [200uL]", "A02"));
        parentVesselBeans.add(new ParentVesselBean("P1234", null, "Plate96Well200PCR", childVesselBeans));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans, "jowalsh");

        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP);
        StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
        Assert.assertEquals(staticPlate.getPlateType(), StaticPlate.PlateType.Plate96Well200PCR);
    }

}
