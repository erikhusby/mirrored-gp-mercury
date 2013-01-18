package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
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

    @BeforeMethod(groups = DATABASE_FREE)
    public void setup() {
        plate3 = new StaticPlate("plate3", Eppendorf96);

        doSectionTransfer(makeTubeFormation(new TwoDBarcodedTube("tube")), plate3);
        doSectionTransfer(new StaticPlate("plate1", Eppendorf96), plate3);
        doSectionTransfer(new StaticPlate("plate2", Eppendorf96), plate3);
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

    @Test(groups = DATABASE_FREE)
    public void testFetchSourceTubesForPlate() {

    }
}
