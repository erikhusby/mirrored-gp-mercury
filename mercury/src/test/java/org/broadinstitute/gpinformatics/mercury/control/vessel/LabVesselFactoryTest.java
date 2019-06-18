package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
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
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP).getLeft();

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getRootOrEarliestMercurySample();
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
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP).getLeft();

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
                MercurySample.MetadataSource.BSP).getLeft();

        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
        LabVessel labVessel = labVessels.get(0);
        Assert.assertEquals(labVessel.getLabel(), BARCODE1, "Wrong barcode");
        Set<SampleInstanceV2> sampleInstances = labVessel.getSampleInstancesV2();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        MercurySample mercurySample = sampleInstances.iterator().next().getRootOrEarliestMercurySample();
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

        Pair<List<LabVessel>,List<LabVessel>> VesselsToPersist = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP);
        List<LabVessel> primaryLabVessels = VesselsToPersist.getLeft();
        List<LabVessel> secondaryLabVessels = VesselsToPersist.getRight();
        Assert.assertEquals(primaryLabVessels.size(), 1, "Wrong number of vessels");
        Assert.assertEquals(secondaryLabVessels.size(), 0, "Static plate should have no secondary vessels to persist");
        StaticPlate staticPlate = (StaticPlate) primaryLabVessels.get(0);
        Assert.assertEquals(staticPlate.getContainerRole().getMapPositionToVessel().size(), 2,
                "Wrong number of child vessels");
        PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstanceV2> sampleInstances = plateWell.getSampleInstancesV2();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        Assert.assertEquals(sampleInstances.iterator().next().getRootOrEarliestMercurySampleName(), sampleId1, "Wrong sample");
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
                LabEventType.SAMPLE_RECEIPT, MercurySample.MetadataSource.BSP).getLeft();
        StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
        Assert.assertEquals(staticPlate.getPlateType(), StaticPlate.PlateType.Plate96Well200PCR);
    }

    @Test
    public void testImportRackOfTubes() {
        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<>();

        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        childVesselBeans.add(new ChildVesselBean("TUBE-1234", "SM-1234", "Matrix Tube Screw cap [0.5mL]", "A01"));
        mapPositionToTube.put(VesselPosition.A01,new BarcodedTube("TUBE-1234"));
        childVesselBeans.add(new ChildVesselBean("TUBE-2345", "SM-2345", "Matrix Tube Screw cap [0.5mL]", "A02"));
        mapPositionToTube.put(VesselPosition.A02,new BarcodedTube("TUBE-2345"));

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        String tubeFormationDigest = tubeFormation.getLabel();

        parentVesselBeans.add(new ParentVesselBean("RACK-01", null, "2D Matrix 96 Slot Rack [0.5ml SC]", childVesselBeans));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans, "jowalsh");

        Pair<List<LabVessel>,List<LabVessel>> VesselsToPersist = labVesselFactory.buildLabVesselDaoFree(
                new HashMap<String, LabVessel>(), new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), sampleReceiptBean.getReceivingUserName(),
                sampleReceiptBean.getReceiptDate(), sampleReceiptBean.getParentVesselBeans(),
                LabEventType.SAMPLE_IMPORT, MercurySample.MetadataSource.BSP);

        List<LabVessel> primaryLabVessels = VesselsToPersist.getLeft();
        Assert.assertEquals(primaryLabVessels.size(), 2, "Wrong number of vessels");
        int testCount = 0;
        for( LabVessel tube : primaryLabVessels ) {
            if( tube.getLabel().equals("TUBE-1234")) testCount++;
            if( tube.getLabel().equals("TUBE-2345")) testCount++;
        }
        Assert.assertEquals(testCount, 2, "Unexpected tubes in vessels");

        List<LabVessel> secondaryLabVessels = VesselsToPersist.getRight();
        Assert.assertEquals(secondaryLabVessels.size(), 2, "Secondary vessels to persist should include 2 vessels");
        testCount = 0;
        for( LabVessel secondaryVessel : secondaryLabVessels ) {
            if (secondaryVessel.getLabel().equals("RACK-01"))  testCount++;
            if (secondaryVessel.getLabel().equals(tubeFormationDigest)) {
                TubeFormation rackTubeFormation = (TubeFormation) secondaryVessel;
                Assert.assertEquals(rackTubeFormation.getLabel(), tubeFormationDigest);
                testCount++;
            }
        }
        Assert.assertEquals(testCount, 2, "A rack and a tube formation are expected in secondary vessels");

    }

}
