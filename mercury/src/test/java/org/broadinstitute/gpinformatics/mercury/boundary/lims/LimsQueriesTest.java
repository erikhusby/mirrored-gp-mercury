package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.END_REPAIR_CLEANUP;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.SHEARING_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.A01;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;

/**
 * Tests for LimsQueries boundary interface.
 *
 * @author breilly
 */
public class LimsQueriesTest {

    private StaticPlate plate3;

    @BeforeMethod
    public void setup() {
        Map<VesselPosition, TwoDBarcodedTube> positionMap = new HashMap<VesselPosition, TwoDBarcodedTube>();
        positionMap.put(A01, new TwoDBarcodedTube("tube"));

        plate3 = new StaticPlate("plate3", Eppendorf96);

        addTransfer(new TubeFormation(positionMap, Matrix96), plate3);
        addTransfer(new StaticPlate("plate1", Eppendorf96), plate3);
        addTransfer(new StaticPlate("plate2", Eppendorf96), plate3);
    }

    private LabEvent addTransfer(TubeFormation sourceTubes, StaticPlate destination) {
        LabEvent event = new LabEvent(SHEARING_TRANSFER, new Date(), "StaticPlateTest", 1L, 1L);
        event.getSectionTransfers().add(
                new SectionTransfer(sourceTubes.getContainerRole(), ALL96, destination.getContainerRole(), ALL96, event));
        return event;
    }

    private LabEvent addTransfer(StaticPlate source, StaticPlate destination) {
        LabEvent event = new LabEvent(END_REPAIR_CLEANUP, new Date(), "StaticPlateTest", 1L, 1L);
        event.getSectionTransfers().add(
                new SectionTransfer(source.getContainerRole(), ALL96, destination.getContainerRole(), ALL96, event));
        return event;
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParents() throws Exception {
        StaticPlateDAO staticPlateDAO = createMock(StaticPlateDAO.class);
        expect(staticPlateDAO.findByBarcode("plate3")).andReturn(plate3);
        replay(staticPlateDAO);

        LimsQueries limsQueries = new LimsQueries(staticPlateDAO);

        List<String> parents = limsQueries.findImmediatePlateParents("plate3");
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem("plate1"));
        assertThat(parents, hasItem("plate2"));
    }
}
