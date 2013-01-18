package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.*;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.*;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.A01;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.B02;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.C03;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;

/**
 * @author breilly
 */
public class StaticPlateTest {

    private TwoDBarcodedTube tube1;
    private TwoDBarcodedTube tube2;
    private TwoDBarcodedTube tube3;

    /** A tube formation with tube1 in A01. */
    private TubeFormation tubeRack1;

    /** A tube formation with tube2 in A01. */
    private TubeFormation tubeRack2;

    /** A tube formation with tube1 in A01, tube2 in B02, and tube3 in C03. */
    private TubeFormation tubeRack3;

    private StaticPlate plate1;
    private StaticPlate plate2;
    private StaticPlate plate3;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setup() {
        tube1 = new TwoDBarcodedTube("tube1");
        tube2 = new TwoDBarcodedTube("tube2");
        tube3 = new TwoDBarcodedTube("tube3");

        Map<VesselPosition, TwoDBarcodedTube> positionMap1 = new HashMap<VesselPosition, TwoDBarcodedTube>();
        positionMap1.put(A01, tube1);
        tubeRack1 = new TubeFormation(positionMap1, Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> positionMap2 = new HashMap<VesselPosition, TwoDBarcodedTube>();
        positionMap2.put(A01, tube2);
        tubeRack2 = new TubeFormation(positionMap2, Matrix96);

        Map<VesselPosition, TwoDBarcodedTube> positionMap3 = new HashMap<VesselPosition, TwoDBarcodedTube>();
        positionMap3.put(A01, tube1);
        positionMap3.put(B02, tube2);
        positionMap3.put(C03, tube3);
        tubeRack3 = new TubeFormation(positionMap3, Matrix96);

        plate1 = new StaticPlate("plate1", Eppendorf96);
        plate2 = new StaticPlate("plate2", Eppendorf96);
        plate3 = new StaticPlate("plate3", Eppendorf96);
    }

    private LabEvent doSectionTransfer(LabVessel source, LabVessel destination) {
        LabEvent event = new LabEvent(A_BASE, new Date(), "StaticPlateTest", 1L, 1L);
        event.getSectionTransfers().add(
                new SectionTransfer(source.getContainerRole(), ALL96, destination.getContainerRole(), ALL96, event));
        return event;
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
        doSectionTransfer(tubeRack1, plate3);
        doSectionTransfer(plate1, plate3);
        doSectionTransfer(plate2, plate3);

        List<StaticPlate> parents = plate3.getImmediatePlateParents();
        assertThat(parents.size(), is(2));
        assertThat(parents, hasItem(plate1));
        assertThat(parents, hasItem(plate2));
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
        doSectionTransfer(tubeRack3, plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(3));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, B02)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube3, C03)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsDuplicate() {
        doSectionTransfer(tubeRack1, plate1);
        doSectionTransfer(tubeRack1, plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors, equalTo(Arrays.asList(new VesselAndPosition(tube1, A01))));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsMultiple() {
        doSectionTransfer(tubeRack1, plate1);
        doSectionTransfer(tubeRack2, plate1);
        List<VesselAndPosition> ancestors = plate1.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(2));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, A01)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsDifferentBranches() {
        doSectionTransfer(tubeRack1, plate1);
        doSectionTransfer(tubeRack2, plate2);
        doSectionTransfer(plate1, plate3);
        doSectionTransfer(plate2, plate3);
        List<VesselAndPosition> ancestors = plate3.getNearestTubeAncestors();
        assertThat(ancestors.size(), is(2));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube1, A01)));
        assertThat(ancestors, hasItem(new VesselAndPosition(tube2, A01)));
    }

    @Test(groups = DATABASE_FREE)
    public void testGetNearestTubeAncestorsIgnoringDistant() {
        doSectionTransfer(tubeRack1, tubeRack2);
        doSectionTransfer(tubeRack2, plate2);
        List<VesselAndPosition> ancestors = plate2.getNearestTubeAncestors();
        assertThat(ancestors, equalTo(Arrays.asList(new VesselAndPosition(tube2, A01))));
    }
}
