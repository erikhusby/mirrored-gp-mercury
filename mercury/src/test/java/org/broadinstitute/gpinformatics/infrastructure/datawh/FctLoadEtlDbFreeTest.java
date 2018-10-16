package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;

/**
 * DBFree unit test for physically loading a flowcell ticket library vessel onto flowcell ETL
 * Tied to LabEvent
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class FctLoadEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 445566L;
    private long labBatchVesselId = 112233L;
    private String flowcellLabel = "H16FDADXX";
    private VesselPosition flowcellLane = VesselPosition.LANE1;
    private FctLoadEtl tst;
    private long flowcellDesignationId = 556677L;

    private LabEventDao dao = createMock(LabEventDao.class);
    private LabEvent labEvent = createMock(LabEvent.class);

    private IlluminaFlowcell flowcell = createMock(IlluminaFlowcell.class);
    private VesselContainer flowcellContainer = createMock(VesselContainer.class);

    private LabVessel dilutionTube = createMock(BarcodedTube.class);

    private StripTube stripTube = createMock(StripTube.class);
    private VesselContainer stripTubeContainer = createMock(VesselContainer.class);

    private LabBatchStartingVessel labBatchStartingVessel = createMock(LabBatchStartingVessel.class);
    private FlowcellDesignation flowcellDesignation = createMock(FlowcellDesignation.class);

    private Object[] mocks = new Object[]{dao, labEvent, flowcell, flowcellContainer
            , dilutionTube, stripTube, stripTubeContainer, labBatchStartingVessel, flowcellDesignation };

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
        tst = new FctLoadEtl(dao);
    }

    public void testEtlFlags() throws Exception {
        expect(labEvent.getLabEventId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabEvent.class);
        assertEquals(tst.baseFilename, "fct_load");
        assertEquals(tst.entityId(labEvent), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabEvent.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testNonFlowcellEvent() throws Exception {
        expect(labEvent.getLabEventType()).andReturn(LabEventType.FLOWCELL_LOADED);

        replay(mocks);

        Collection<String> dataRecords = tst.dataRecords(etlDateString, false, labEvent);
        assertEquals(dataRecords.size(), 0, "Non-flowcell event should not produce ETL data");

        verify(mocks);
    }

    public void testFctDilutionLoadedEtl() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(labEvent);
        expect(labEvent.getLabEventType()).andReturn(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER).anyTimes();

        // Mock out logic in getFlowcellAndSourceTubes()
        // Flowcell transfer target
        Set<LabVessel> targets = new HashSet<>();
        targets.add(flowcell);
        expect(labEvent.getTargetLabVessels()).andReturn(targets);
        expect(flowcell.getContainerRole()).andReturn(flowcellContainer).anyTimes();

        // Flowcell transfer source
        // Dummy up with a null source container so falls through strip tube logic into dilution tube logic
        LabVessel.VesselEvent ancestorVesselEvent = new LabVessel.VesselEvent(dilutionTube, null,null,labEvent,flowcell, flowcellContainer, VesselPosition.LANE1);
        expect(flowcellContainer.getAncestors(flowcellLane)).andReturn(Collections.singletonList(ancestorVesselEvent)).anyTimes();
        Map<VesselPosition,LabVessel> loadedVesselsAndPositions = new HashMap<>();
        loadedVesselsAndPositions.put(flowcellLane, dilutionTube);
        expect(flowcellContainer.getPositions()).andReturn(loadedVesselsAndPositions.keySet()).anyTimes();
        expect(flowcell.getNearestTubeAncestorsForLanes()).andReturn(loadedVesselsAndPositions);
        expect( dilutionTube.getDilutionReferences()).andReturn(Collections.singleton(labBatchStartingVessel));

        // ETL fields
        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(labBatchVesselId).anyTimes();
        expect(labBatchStartingVessel.getFlowcellDesignation()).andReturn(flowcellDesignation).anyTimes();
        expect(flowcell.getLabel()).andReturn(flowcellLabel);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testFctStripTubeLoadedEtl() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(labEvent);
        expect(labEvent.getLabEventType()).andReturn(LabEventType.DILUTION_TO_FLOWCELL_TRANSFER).anyTimes();

        // Mock out logic in getFlowcellAndSourceTubes()
        // Flowcell transfer target
        Set<LabVessel> targets = new HashSet<>();
        targets.add(flowcell);
        expect(labEvent.getTargetLabVessels()).andReturn(targets);
        expect(flowcell.getContainerRole()).andReturn(flowcellContainer).anyTimes();

        // Flowcell transfer source
        // Strip tube source container will invoke strip tube logic
        LabVessel.VesselEvent ancestorVesselEvent = new LabVessel.VesselEvent(stripTube, stripTubeContainer, VesselPosition.TUBE1,
                labEvent, flowcell, flowcellContainer, VesselPosition.LANE1);
        expect(flowcellContainer.getPositions()).andReturn(Collections.singleton(VesselPosition.LANE1)).anyTimes();
        expect(flowcellContainer.getAncestors(flowcellLane)).andReturn(Collections.singletonList(ancestorVesselEvent)).anyTimes();
        expect(stripTubeContainer.getEmbedder()).andReturn(stripTube).times(2);
        expect(stripTube.getType()).andReturn(LabVessel.ContainerType.STRIP_TUBE);

        expect(stripTube.getDilutionReferences()).andReturn(Collections.singleton(labBatchStartingVessel));

        // ETL fields
        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(labBatchVesselId).anyTimes();
        expect(labBatchStartingVessel.getFlowcellDesignation()).andReturn(flowcellDesignation).anyTimes();
        expect(flowcell.getLabel()).andReturn(flowcellLabel);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(labBatchVesselId));
        assertEquals(parts[i++], String.valueOf(flowcellLabel));
        assertEquals(parts.length, i);
    }
}

