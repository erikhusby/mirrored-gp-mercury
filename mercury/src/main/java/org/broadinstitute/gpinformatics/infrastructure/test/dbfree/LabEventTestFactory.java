package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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

    public static LabEvent addInPlaceEvent(LabVessel target) {
        return addInPlaceEvent(A_BASE, target);
    }

    public static LabEvent addInPlaceEvent(LabEventType labEventType, LabVessel target) {
        LabEvent event = new LabEvent(labEventType, new Date(), "LabEventTestFactory", 0L, 0L, "labEventTestFactory");
        target.addInPlaceEvent(event);
        return event;
    }

    public static TubeFormation makeTubeFormation(BarcodedTube... tubes) {
        Map<VesselPosition, BarcodedTube> positionMap = new HashMap<>();
        int i = 0;
        for (BarcodedTube tube : tubes) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        return new TubeFormation(positionMap, Matrix96);
    }

    public static TubeFormation makeTubeFormation(VesselPosition[] positions, BarcodedTube[] tubes) {
        assertThat("There must be at least as many positions as tubes",
                positions.length, greaterThanOrEqualTo(tubes.length));
        Map<VesselPosition, BarcodedTube> positionMap = new HashMap<>();
        for (int i = 0; i < tubes.length; i++) {
            BarcodedTube tube = tubes[i];
            VesselPosition position = positions[i];
            positionMap.put(position, tube);
        }
        return new TubeFormation(positionMap, Matrix96);
    }

    public static LabEvent doSectionTransfer(LabVessel source, LabVessel destination) {
        return doSectionTransfer(A_BASE, source, destination);
    }

    public static LabEvent doSectionTransfer(LabEventType labEventType, LabVessel source, LabVessel destination) {
        LabEvent event = new LabEvent(labEventType, new Date(), "StaticPlateTest", 1L, 1L, "labEventTestFactory");
        event.getSectionTransfers().add(new SectionTransfer(source.getContainerRole(), ALL96, null,
                destination.getContainerRole(), ALL96, null, event));
        return event;
    }

    public static TubeFormation makeTubeFormation(TubeFormation normTubeFormation, BarcodedTube... tubes) {
        Map<VesselPosition, BarcodedTube> positionMap = new HashMap<>();
        int i = 0;
        for (BarcodedTube tube : normTubeFormation.getContainerRole().getContainedVessels()) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        for (BarcodedTube tube : tubes) {
            positionMap.put(VesselPosition.values()[i], tube);
            i++;
        }
        return new TubeFormation(positionMap, Matrix96);
    }
}
