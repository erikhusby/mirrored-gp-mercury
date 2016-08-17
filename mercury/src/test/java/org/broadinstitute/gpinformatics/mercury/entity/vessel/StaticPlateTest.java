package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.doSectionTransfer;
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.Eppendorf96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

/**
 * @author breilly
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class StaticPlateTest {

    private BarcodedTube tube1;
    private BarcodedTube tube2;
    private BarcodedTube tube3;

    private StaticPlate plate1;
    private StaticPlate plate2;
    private StaticPlate plate3;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setup() {
        tube1 = new BarcodedTube("tube1");
        tube2 = new BarcodedTube("tube2");
        tube3 = new BarcodedTube("tube3");

        plate1 = new StaticPlate("plate1", Eppendorf96);
        plate2 = new StaticPlate("plate2", Eppendorf96);
        plate3 = new StaticPlate("plate3", Eppendorf96);
    }

    /*
     * Tests for getImmediatePlateParents()
     */

    @Test(groups = DATABASE_FREE)
    public void testGetImmediatePlateParentsNoParents() throws Exception {
        assertThat(plate1.getImmediatePlateParents().size(), is(0));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetImmediatePlateParentsSingle() {
        doSectionTransfer(plate1, plate2);
        assertThat(plate2.getImmediatePlateParents(), equalTo(Arrays.asList(plate1)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetImmediatePlateParentsMultiple() {
        doSectionTransfer(plate1, plate3);
        doSectionTransfer(plate2, plate3);

        List<StaticPlate> parents = plate3.getImmediatePlateParents();
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem(plate1));
        assertThat(parents, hasItem(plate2));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetImmediatePlateParentsMixed() {
        doSectionTransfer(makeTubeFormation(tube1), plate3);
        doSectionTransfer(plate1, plate3);
        doSectionTransfer(plate2, plate3);

        List<StaticPlate> parents = plate3.getImmediatePlateParents();
        assertThat(parents.size(), is(2));
        assertThat(parents, hasItem(plate1));
        assertThat(parents, hasItem(plate2));
    }

    /*
     * Tests for getWellHasRackContentStatus()
     */

    @Test(groups = DATABASE_FREE)
    public void testGetHasRackContentByWellNone() {
        Map<VesselPosition, Boolean> hasRackContentByWell = plate1.getHasRackContentByWell();
        assertThat(hasRackContentByWell.size(), is(96));
        for (Map.Entry<VesselPosition, Boolean> entry : hasRackContentByWell.entrySet()) {
            assertThat("Entry for " + entry.getKey() + " has wrong value", entry.getValue(), is(false));
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testGetHasRackContentByWell() {
        doSectionTransfer(makeTubeFormation(tube1), plate1);
        Map<VesselPosition, Boolean> hasRackContentByWell = plate1.getHasRackContentByWell();
        assertThat(hasRackContentByWell.size(), is(96));
        assertThat(hasRackContentByWell.get(A01), is(true));
        for (Map.Entry<VesselPosition, Boolean> entry : hasRackContentByWell.entrySet()) {
            if (entry.getKey() != A01) {
                assertThat("Entry for " + entry.getKey() + " has wrong value", entry.getValue(), is(false));
            }
        }
    }

    /**
     * Make sure that a parallel plate transfer does not overwrite a true status from a rack transfer. This is somewhat
     * of a regression case from an earlier implementation, but the behavior is non-deterministic. See implementation
     * note in
     * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.HasRackContentByWellCriteria#evaluateVesselPreOrder(org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.Context)}
     * for more details.
     */
    @Test(groups = DATABASE_FREE)
    public void testGetHasRackContentByWellWithParallelPlateTransfer() {
        doSectionTransfer(makeTubeFormation(tube1), plate2);
        doSectionTransfer(plate1, plate2);
        Map<VesselPosition, Boolean> hasRackContentByWell = plate2.getHasRackContentByWell();
        assertThat(hasRackContentByWell.size(), is(96));
        assertThat(hasRackContentByWell.get(A01), is(true));
        for (Map.Entry<VesselPosition, Boolean> entry : hasRackContentByWell.entrySet()) {
            if (entry.getKey() != A01) {
                assertThat("Entry for " + entry.getKey() + " has wrong value", entry.getValue(), is(false));
            }
        }
    }

    /*
     * Tests for getNearestTubeAncestors()
     */

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsNone() {
        assertThat(plate1.getNearestTubeAncestors().size(), is(0));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsMultipleWellsSingleAncestors() {
        TubeFormation rack = makeTubeFormation(
                new VesselPosition[]{A01, B02, C03},
                new BarcodedTube[]{tube1, tube2, tube3});
        doSectionTransfer(rack, plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(3));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, B02)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube3, C03)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsDuplicate() {
        doSectionTransfer(makeTubeFormation(tube1), plate1);
        doSectionTransfer(makeTubeFormation(tube1), plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors, equalTo(Arrays.asList(new VesselAndPosition(tube1, A01))));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsMultiple() {
        doSectionTransfer(makeTubeFormation(tube1), plate1);
        doSectionTransfer(makeTubeFormation(tube2), plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(2));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, A01)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsDifferentBranches() {
        doSectionTransfer(makeTubeFormation(tube1), plate1);
        doSectionTransfer(makeTubeFormation(tube2), plate2);
        doSectionTransfer(plate1, plate3);
        doSectionTransfer(plate2, plate3);
        List<VesselAndPosition> ancestors = plate3.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(2));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, A01)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsIgnoringDistant() {
        TubeFormation rack = makeTubeFormation(tube2);
        doSectionTransfer(makeTubeFormation(tube1), rack);
        doSectionTransfer(rack, plate2);
        List<VesselAndPosition> ancestors = plate2.getNearestTubeAncestors();
        assertThat(ancestors, equalTo(Arrays.asList(new VesselAndPosition(tube2, A01))));
    }

    /*
     * Tests for fetchTransfers()
     */

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersNone() {
        assertThat(plate1.getUpstreamPlateTransfers(8).size(), is(0));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersSingle() {
        LabEvent event = doSectionTransfer(plate1, plate2);
        List<SectionTransfer> transfers = plate2.getUpstreamPlateTransfers(8);
        assertThat(transfers.size(), is(1));
        assertThat(transfers, hasItem(getOnly(event.getSectionTransfers())));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersMultipleAtSameDepth() {
        LabEvent event1 = doSectionTransfer(plate1, plate3);
        LabEvent event2 = doSectionTransfer(plate2, plate3);
        List<SectionTransfer> transfers = plate3.getUpstreamPlateTransfers(8);
        assertThat(transfers.size(), is(2));
        assertThat(transfers, hasItems(getOnly(event1.getSectionTransfers())));
        assertThat(transfers, hasItems(getOnly(event2.getSectionTransfers())));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersMultipleAtDifferentDepths() {
        LabEvent event1 = doSectionTransfer(plate1, plate2);
        LabEvent event2 = doSectionTransfer(plate2, plate3);
        List<SectionTransfer> transfers = plate3.getUpstreamPlateTransfers(8);
        assertThat(transfers.size(), is(2));
        // transfers should be in order from nearest to furthest
        assertThat(transfers.get(0), equalTo(getOnly(event2.getSectionTransfers())));
        assertThat(transfers.get(1), equalTo(getOnly(event1.getSectionTransfers())));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersThroughRackTransfer() {
        LabEvent event1 = doSectionTransfer(makeTubeFormation(tube1), plate1);
        TubeFormation rack = makeTubeFormation(tube2);
        LabEvent event2 = doSectionTransfer(plate1, rack);
        LabEvent event3 = doSectionTransfer(rack, plate2);
        List<SectionTransfer> transfers = plate2.getUpstreamPlateTransfers(8);
        assertThat(transfers.size(), is(3));
        // transfers should be in order from nearest to furthest
        assertThat(transfers.get(0), equalTo(getOnly(event3.getSectionTransfers())));
        assertThat(transfers.get(1), equalTo(getOnly(event2.getSectionTransfers())));
        assertThat(transfers.get(2), equalTo(getOnly(event1.getSectionTransfers())));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetTransfersLimitDepth() {
        doSectionTransfer(plate1, plate2);
        LabEvent event = doSectionTransfer(plate2, plate3);
        List<SectionTransfer> transfers = plate3.getUpstreamPlateTransfers(1);
        assertThat(transfers.size(), is(1));
        assertThat(transfers, hasItem(getOnly(event.getSectionTransfers())));
    }

    private <T> T getOnly(Collection<T> collection) {
        assertThat(collection.size(), is(1));
        return collection.iterator().next();
    }
}
