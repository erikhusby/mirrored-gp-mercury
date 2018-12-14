package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ArrayPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * dbfree unit test of AbandonVessel and AbandonVesselPosition etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ArrayProcessFlowEtlDbFreeTest extends BaseEventTest {

    private String etlDateStr;
    private Long pdoId = 999L;


    private ArrayProcessFlowEtl arrayProcessFlowEtl;

    private final LabEventDao dao = EasyMock.createMock(LabEventDao.class);

    private final Object[] mocks = new Object[]{ dao };

    public ArrayProcessFlowEtlDbFreeTest(){
        Calendar calendar = Calendar.getInstance();
        etlDateStr = ExtractTransform.formatTimestamp(calendar.getTime());

    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        EasyMock.reset(mocks);
        arrayProcessFlowEtl = new ArrayProcessFlowEtl(dao);
    }

    public void testCantMakeEtlRecords() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, -1L).size(), 0);
        EasyMock.verify(mocks);
    }

    public void testNotAnInfiniumEvent() throws Exception {
        LabEvent notAnInfiniumEvent = new LabEvent(LabEventType.A_BASE, new Date(), "OZ", 1l, 1L, "Python Deck");
        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(notAnInfiniumEvent);
        EasyMock.replay(mocks);

        Assert.assertEquals(arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L).size(), 0);
        EasyMock.verify(mocks);
    }

    /**
     *  Tests upstream DNA plate data mapped to PDO, LCSET, and LCSET Sample name from an Infinium bucketed DNA plate well
     **/
    public void testBucketEvent() throws Exception {
        expectedRouting = SystemRouter.System.MERCURY;
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumProductOrder(numSamples);

        Field pdoIdField = ProductOrder.class.getDeclaredField("productOrderId");
        pdoIdField.setAccessible(true);
        pdoIdField.set(productOrder, pdoId );

        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Infinium Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.INFINIUM);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        ArrayPlatingEntityBuilder arrayPlatingEntityBuilder =
                runArrayPlatingProcess(mapBarcodeToDaughterTube, "Infinium");

        LabBatch arrayBatch = new LabBatch("ArrayBatch", new HashSet<>(),LabBatch.LabBatchType.WORKFLOW);

        arrayPlatingEntityBuilder.bucketPlateWells(productOrder, arrayBatch);

        InfiniumEntityBuilder infiniumEntityBuilder = runInfiniumProcess(
                arrayPlatingEntityBuilder.getArrayPlatingPlate(), "Infinium");

        StaticPlate dnaPlate = arrayPlatingEntityBuilder.getArrayPlatingPlate();
        PlateWell wellA01 = dnaPlate.getContainerRole().getMapPositionToVessel().get(VesselPosition.A01);

        LabEvent bucketEvent = wellA01.getInPlaceLabEvents().iterator().next();

        EasyMock.expect(dao.findById(LabEvent.class, 1L)).andReturn(bucketEvent);
        EasyMock.replay(mocks);

        Collection<String> records = arrayProcessFlowEtl.dataRecords(etlDateStr, false, 1L);
        Assert.assertEquals(records.size(), 1);

        String[] parts;
        Iterator<String> iter = records.iterator();

        // Plating
        parts = iter.next().split(",",12);
        Assert.assertEquals( parts[0], etlDateStr);
        Assert.assertEquals( parts[1], "F");
        Assert.assertEquals( parts[2], pdoId.toString());
        Assert.assertEquals( parts[3], arrayBatch.getBatchName());
        Assert.assertFalse(parts[4].isEmpty(), "A value is expected for sample name.");
        Assert.assertEquals( parts[4],  parts[5]);
        // Ignore event ID
        Assert.assertEquals( parts[7], LabEventType.ARRAY_PLATING_DILUTION.getName());
        // Ignore event location
        // Ignore event date
        // Ignore labVesselId
        Assert.assertEquals( parts[11], VesselPosition.A01.toString());

        EasyMock.verify(mocks);
    }

}
