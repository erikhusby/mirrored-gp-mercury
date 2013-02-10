package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Dao Free test of
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleReceiptResourceTest {

    @Test
    public void testReceiveTubes() {
        SampleReceiptResource sampleReceiptResource = new SampleReceiptResource();
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        parentVesselBeans.add(new ParentVesselBean("1234", "SM-1234", "PDO-123", "Matrix Tube [0.75mL]", null));
        parentVesselBeans.add(new ParentVesselBean("2345", "SM-2345", "PDO-123", "Matrix Tube [0.75mL]", null));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans);

        List<LabVessel> labVessels = sampleReceiptResource.notifyOfReceiptDaoFree(sampleReceiptBean, new HashMap<String, LabVessel>());
        Assert.assertEquals(labVessels.size(), 2, "Wrong number of vessels");
    }

    @Test
    public void testReceivePlates() {
        SampleReceiptResource sampleReceiptResource = new SampleReceiptResource();
        ArrayList<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<ChildVesselBean>();
        String sampleId1 = "SM-1234";
        childVesselBeans.add(new ChildVesselBean(null, sampleId1, "PDO-123", "Well [200uL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(null, "SM-2345", "PDO-123", "Well [200uL]", "A02"));
        parentVesselBeans.add(new ParentVesselBean("P1234", null, null, "Plate 96 Well PCR [200ul]", childVesselBeans));
        SampleReceiptBean sampleReceiptBean = new SampleReceiptBean(new Date(), "SK-123", parentVesselBeans);

        List<LabVessel> labVessels = sampleReceiptResource.notifyOfReceiptDaoFree(sampleReceiptBean, new HashMap<String, LabVessel>());
        Assert.assertEquals(labVessels.size(), 1, "Wrong number of vessels");
        StaticPlate staticPlate = (StaticPlate) labVessels.get(0);
        Assert.assertEquals(staticPlate.getContainerRole().getMapPositionToVessel().size(), 2, "Wrong number of child vessels");
        PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(VesselPosition.A01);
        Set<SampleInstance> sampleInstances = plateWell.getSampleInstances();
        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of samples");
        Assert.assertEquals(sampleInstances.iterator().next().getStartingSample().getSampleKey(), sampleId1, "Wrong sample");
    }
}
