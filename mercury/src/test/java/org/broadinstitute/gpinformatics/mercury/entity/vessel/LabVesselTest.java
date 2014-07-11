package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("FeatureEnvy")
public class LabVesselTest {
    @Test
    public void testFindVesselsForLabEventTypes() {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        BarcodedTube sourceTube1 = new BarcodedTube("ST1", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapPositionToTube.put(VesselPosition.A01, sourceTube1);
        BarcodedTube sourceTube2 = new BarcodedTube("ST2", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapPositionToTube.put(VesselPosition.A02, sourceTube2);
        TubeFormation sourceTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        mapPositionToTube = new HashMap<>();
        BarcodedTube destinationTube1 = new BarcodedTube("DT1", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapPositionToTube.put(VesselPosition.A01, destinationTube1);
        BarcodedTube destinationTube2 = new BarcodedTube("DT2", BarcodedTube.BarcodedTubeType.MatrixTube);
        mapPositionToTube.put(VesselPosition.A02, destinationTube2);
        TubeFormation targetTubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        LabEvent labEvent = new LabEvent(LabEventType.AUTO_DAUGHTER_PLATE_CREATION, new Date(), "SUPERMAN", 1L, 101L,
                "Bravo");

        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTubeFormation.getContainerRole(), VesselPosition.A01,
                targetTubeFormation.getContainerRole(), VesselPosition.A01, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTubeFormation.getContainerRole(), VesselPosition.A02,
                targetTubeFormation.getContainerRole(), VesselPosition.A02, labEvent));

        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans.add(new ChildVesselBean(destinationTube1.getLabel(), "SM-1234",
                destinationTube1.getTubeType().getDisplayName(), VesselPosition.A01.name()));
        childVesselBeans.add(new ChildVesselBean(destinationTube2.getLabel(), "SM-2345",
                destinationTube2.getTubeType().getDisplayName(), VesselPosition.A02.name()));
        ParentVesselBean parentVesselBean = new ParentVesselBean("Rack1", null, "rack", childVesselBeans);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(destinationTube1.getLabel(), destinationTube1);
        mapBarcodeToVessel.put(destinationTube2.getLabel(), destinationTube2);
        mapBarcodeToVessel.put(targetTubeFormation.getLabel(), targetTubeFormation);
        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                mapBarcodeToVessel, new HashMap<String, List<MercurySample>>(),
                new HashMap<String, Set<ProductOrderSample>>(), "jowalsh",
                new Date(), Collections.singletonList(parentVesselBean), LabEventType.SAMPLE_IMPORT);
        Map<LabEvent, Set<LabVessel>> vesselsForLabEventType = sourceTube1.findVesselsForLabEventType(
                LabEventType.SAMPLE_IMPORT);
        Assert.assertEquals(vesselsForLabEventType.size(), 1);
    }
}