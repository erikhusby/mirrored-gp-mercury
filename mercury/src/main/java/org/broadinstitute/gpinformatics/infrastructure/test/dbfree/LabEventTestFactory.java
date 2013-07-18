package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.A_BASE;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes.RackType.Matrix96;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL96;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * @author breilly
 */
public class LabEventTestFactory {

    public static TubeFormation makeTubeFormation(TwoDBarcodedTube... tubes) {
        Map<VesselPosition, TwoDBarcodedTube> positionMap = new HashMap<VesselPosition, TwoDBarcodedTube>();
        int i = 0;
        for (TwoDBarcodedTube tube : tubes) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        return new TubeFormation(positionMap, Matrix96);
    }

    public static TubeFormation makeTubeFormation(VesselPosition[] positions, TwoDBarcodedTube[] tubes) {
        assertThat("There must be at least as many positions as tubes",
                positions.length, greaterThanOrEqualTo(tubes.length));
        Map<VesselPosition, TwoDBarcodedTube> positionMap = new HashMap<VesselPosition, TwoDBarcodedTube>();
        for (int i = 0; i < tubes.length; i++) {
            TwoDBarcodedTube tube = tubes[i];
            VesselPosition position = positions[i];
            positionMap.put(position, tube);
        }
        return new TubeFormation(positionMap, Matrix96);
    }

    public static LabEvent doSectionTransfer(LabVessel source, LabVessel destination) {
        LabEvent event = new LabEvent(A_BASE, new Date(), "StaticPlateTest", 1L, 1L);
        event.getSectionTransfers().add(
                new SectionTransfer(source.getContainerRole(), ALL96, destination.getContainerRole(), ALL96, event));
        return event;
    }

    public static TubeFormation makeTubeFormation(TubeFormation normTubeFormation, TwoDBarcodedTube... tubes) {
        Map<VesselPosition, TwoDBarcodedTube> positionMap = new HashMap<VesselPosition, TwoDBarcodedTube>();
        int i = 0;
        for (TwoDBarcodedTube tube : normTubeFormation.getContainerRole().getContainedVessels()) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        for (TwoDBarcodedTube tube : tubes) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        return new TubeFormation(positionMap, Matrix96);
    }
}
