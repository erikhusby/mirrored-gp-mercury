package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentration;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSetVolumeConcentrationStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsObjectFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("FeatureEnvy")
@Test(groups = TestGroups.DATABASE_FREE)
public class LabVesselTest {
    public void testGetLatestMaterialTypeForVessel() {
        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        BSPSetVolumeConcentration bspSetVolumeConcentration = new BSPSetVolumeConcentrationStub();
        LabEventFactory labEventFactory = new LabEventFactory(testUserList, bspSetVolumeConcentration);
        BarcodedTube sourceVessel = new BarcodedTube("A_SOURCE_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
        MaterialType startingMaterialType = MaterialType.FRESH_BLOOD;
        MercurySample mercurySample = SampleDataTestFactory.getTestMercurySample(startingMaterialType,
                MercurySample.MetadataSource.MERCURY);
        mercurySample.addLabVessel(sourceVessel);

        int transferCount = 0;
        BarcodedTube destinationVessel = new BarcodedTube("Just So Intellij Doesn't Think I'm Null!");
        EnumSet<LabEventType> transferEventTypes =
                EnumSet.of(LabEventType.EXTRACT_BLOOD_TO_MICRO, LabEventType.EXTRACT_BLOOD_MICRO_TO_SPIN,
                        LabEventType.EXTRACT_BLOOD_SPIN_TO_MATRIX);
        for (LabEventType labEventType : transferEventTypes) {
            destinationVessel = new BarcodedTube(String.format("TRANSFER_TUBE_%d", transferCount++),
                    BarcodedTube.BarcodedTubeType.MatrixTube075);

            LabVesselTest.doVesselToVesselTransfer(sourceVessel, destinationVessel,
                    startingMaterialType, labEventType, MercurySample.MetadataSource.MERCURY, labEventFactory);
            startingMaterialType = labEventType.getResultingMaterialType();
            sourceVessel=destinationVessel;
        }

        TransferTraverserCriteria.NearestMaterialTypeTraverserCriteria traverserCriteria =
                destinationVessel.evaluateMaterialTypeTraverserCriteria();
        assertThat(traverserCriteria.getMaterialType(), is(MaterialType.DNA));
        assertThat(destinationVessel.isDNA(), is(true));
    }

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
                sourceTubeFormation.getContainerRole(), VesselPosition.A01, null,
                targetTubeFormation.getContainerRole(), VesselPosition.A01, null, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTubeFormation.getContainerRole(), VesselPosition.A02, null,
                targetTubeFormation.getContainerRole(), VesselPosition.A02, null, labEvent));

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
                mapBarcodeToVessel, new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), "jowalsh",
                new Date(), Collections.singletonList(parentVesselBean), LabEventType.SAMPLE_IMPORT,
                MercurySample.MetadataSource.BSP).getLeft();
        Map<LabEvent, Set<LabVessel>> vesselsForLabEventType = sourceTube1.findVesselsForLabEventType(
                LabEventType.SAMPLE_IMPORT, true);
        Assert.assertEquals(vesselsForLabEventType.size(), 1);
    }

    @Test
    public void testDaughterPlateWithDifferentTypes() {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        BarcodedTube sourceTube1 = new BarcodedTube("ST1", "Conical50");
        mapPositionToTube.put(VesselPosition._8_2, sourceTube1);
        BarcodedTube sourceTube2 = new BarcodedTube("ST2", "Conical [50mL]");
        mapPositionToTube.put(VesselPosition._8_3, sourceTube2);
        TubeFormation sourceTubeFormation =
                new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Conical50ml_8x3rack);

        mapPositionToTube = new HashMap<>();
        BarcodedTube destinationTube1 = new BarcodedTube("DT1", "Vacutainer EDTA Tube Purple-Top [3mL]");
        mapPositionToTube.put(VesselPosition.A31, destinationTube1);
        BarcodedTube destinationTube2 = new BarcodedTube("DT2", "VacutainerBloodTubeEDTA_3");
        mapPositionToTube.put(VesselPosition.A32, destinationTube2);
        TubeFormation targetTubeFormation =
                new TubeFormation(mapPositionToTube, RackOfTubes.RackType.HamiltonSampleCarrier32);

        LabEvent labEvent = new LabEvent(LabEventType.AUTO_DAUGHTER_PLATE_CREATION, new Date(), "SUPERMAN", 1L, 101L,
                "Bravo");

        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTubeFormation.getContainerRole(), VesselPosition._8_2, null,
                targetTubeFormation.getContainerRole(), VesselPosition.A31, null, labEvent));
        labEvent.getCherryPickTransfers().add(new CherryPickTransfer(
                sourceTubeFormation.getContainerRole(), VesselPosition._8_3, null,
                targetTubeFormation.getContainerRole(), VesselPosition.A32, null, labEvent));

        LabVesselFactory labVesselFactory = new LabVesselFactory();
        labVesselFactory.setBspUserList(new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans.add(new ChildVesselBean(destinationTube1.getLabel(), "SM-1234",
                destinationTube1.getTubeType().getDisplayName(), VesselPosition._8_2.name()));
        childVesselBeans.add(new ChildVesselBean(destinationTube2.getLabel(), "SM-2345",
                destinationTube2.getTubeType().getDisplayName(), VesselPosition._8_3.name()));
        ParentVesselBean parentVesselBean = new ParentVesselBean("Rack1", null, "Conical50ml_8x3rack",
                childVesselBeans);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(destinationTube1.getLabel(), destinationTube1);
        mapBarcodeToVessel.put(destinationTube2.getLabel(), destinationTube2);
        mapBarcodeToVessel.put(targetTubeFormation.getLabel(), targetTubeFormation);
        List<LabVessel> labVessels = labVesselFactory.buildLabVesselDaoFree(
                mapBarcodeToVessel, new HashMap<String, MercurySample>(),
                new HashMap<String, Set<ProductOrderSample>>(), "jowalsh",
                new Date(), Collections.singletonList(parentVesselBean), LabEventType.SAMPLE_IMPORT,
                MercurySample.MetadataSource.BSP).getLeft();

        BarcodedTube tube1 = (BarcodedTube)labVessels.get(0);
        Assert.assertEquals(tube1.getTubeType(), BarcodedTube.BarcodedTubeType.VacutainerBloodTubeEDTA_3);

        Map <LabEvent, Set<LabVessel>> vesselsForLabEventType = sourceTube1.findVesselsForLabEventType(
                LabEventType.SAMPLE_IMPORT, true);
        Assert.assertEquals(vesselsForLabEventType.values().iterator().next().iterator().next().getLabel(),
                tube1.getLabel());
    }

    public void testIsDnaCanHandleNulls() {
        LabVessel labVessel = new LabVessel() {
            @Override
            public Date getCreatedOn() {
                return new Date();
            }

            @Override
            public VesselGeometry getVesselGeometry() {return null;}

            @Override
            public ContainerType getType() { return null; }
        };

        String sampleKey = "SM-1342";
        labVessel.addSample(new MercurySample(sampleKey, Collections.<Metadata>emptySet()));
        Assert.assertEquals(labVessel.isDNA(), false);
    }

    public static LabEvent doVesselToVesselTransfer(LabVessel sourceVessel,
                                                    LabVessel destinationVessel,
                                                    MaterialType sampleMaterialType,
                                                    LabEventType labEventType,
                                                    MercurySample.MetadataSource metadataSource,
                                                    LabEventFactory labEventFactory) {
        MercurySample destinationSample =
                new MercurySample("SM-2", metadataSource);
        destinationSample.addLabVessel(destinationVessel);

        ReceptacleTransferEventType receptacleTransferEventType = new ReceptacleTransferEventType();
        receptacleTransferEventType.setEventType(labEventType.getName());
        receptacleTransferEventType.setOperator("QADudePM");
        receptacleTransferEventType.setProgram(LabEvent.UI_PROGRAM_NAME);
        receptacleTransferEventType.setStation(LabEvent.UI_EVENT_LOCATION);
        receptacleTransferEventType.setDisambiguator(1L);
        receptacleTransferEventType.setStart(new Date());

        String materialTypeString = null;
        if (sampleMaterialType!=null) {
            materialTypeString = sampleMaterialType.getDisplayName();
        }
        String sourceReceptacleType = labEventType.getManualTransferDetails() == null ?
                BarcodedTube.BarcodedTubeType.MatrixTube.getDisplayName() :
                labEventType.getManualTransferDetails().getSourceVesselTypeGeometry().getDisplayName();
        ReceptacleType sourceReceptacle = BettaLimsObjectFactory.createReceptacleType(sourceVessel.getLabel(),
                sourceReceptacleType, "",
                materialTypeString, null, null, null,
                Collections.<ReagentType>emptyList(),
                Collections.<MetadataType>emptyList());
        receptacleTransferEventType.setSourceReceptacle(sourceReceptacle);

        String resultingMaterialTypeString = null;
        if (labEventType.getResultingMaterialType() != null) {
            resultingMaterialTypeString = labEventType.getResultingMaterialType().getDisplayName();
        }
        String targetReceptacleType = labEventType.getManualTransferDetails() == null ?
                BarcodedTube.BarcodedTubeType.MatrixTube.getDisplayName() :
                labEventType.getManualTransferDetails().getTargetVesselTypeGeometry().getDisplayName();
        ReceptacleType destinationReceptacle = BettaLimsObjectFactory.createReceptacleType(destinationVessel.getLabel(),
                targetReceptacleType, "",
                resultingMaterialTypeString, null, null, null,
                Collections.<ReagentType>emptyList(),
                Collections.<MetadataType>emptyList());
        receptacleTransferEventType.setReceptacle(destinationReceptacle);

        Map<String, LabVessel> labVesselMap =
                ImmutableMap.of(sourceVessel.getLabel(), sourceVessel, destinationVessel.getLabel(), destinationVessel);

        return labEventFactory.buildReceptacleTransferEventDbFree(receptacleTransferEventType, labVesselMap);
    }
}
