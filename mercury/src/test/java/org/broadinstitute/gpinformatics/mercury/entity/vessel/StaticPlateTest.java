package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.*;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.PlateType.*;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.A01;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;

/**
 * @author breilly
 */
public class StaticPlateTest {

    private StaticPlate plate1;
    private StaticPlate plate2;
    private StaticPlate plate3;
    private TubeFormation tubeRack;

    @BeforeMethod
    public void setup() {
        TwoDBarcodedTube tube = new TwoDBarcodedTube("tube");
        Map<VesselPosition, TwoDBarcodedTube> positionMap = new HashMap<VesselPosition, TwoDBarcodedTube>();
        positionMap.put(A01, tube);
        tubeRack = new TubeFormation(positionMap, Matrix96);

        plate1 = new StaticPlate("plate1", Eppendorf96);
        plate2 = new StaticPlate("plate2", Eppendorf96);
        plate3 = new StaticPlate("plate3", Eppendorf96);
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

    @Test
    public void testGetImmediatePlateParentsNoParents() throws Exception {
        assertThat(plate1.getImmediatePlateParents().size(), is(0));
    }

    @Test
    public void testGetImmediatePlateParentsSingle() {
        addTransfer(plate1, plate2);
        assertThat(plate2.getImmediatePlateParents(), equalTo(Arrays.asList(plate1)));
    }

    @Test
    public void testGetImmediatePlateParentsMultiple() {
        addTransfer(plate1, plate3);
        addTransfer(plate2, plate3);

        List<StaticPlate> parents = plate3.getImmediatePlateParents();
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem(plate1));
        assertThat(parents, hasItem(plate2));
    }

    @Test
    public void testGetImmediatePlateParentsMixed() {
        addTransfer(tubeRack, plate3);
        addTransfer(plate1, plate3);
        addTransfer(plate2, plate3);

        List<StaticPlate> parents = plate3.getImmediatePlateParents();
        assertThat(parents.size(), equalTo(2));
        assertThat(parents, hasItem(plate1));
        assertThat(parents, hasItem(plate2));
    }
}
