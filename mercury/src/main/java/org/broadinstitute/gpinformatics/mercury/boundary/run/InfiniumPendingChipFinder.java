package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lists infinium chips pending autocall. E.g. chips with an xstain event but not an InfiniumAutoCallAllStarted.
 */
public class InfiniumPendingChipFinder {

    @Inject
    private LabEventDao labEventDao;

    public List<LabVessel> listPendingXStainChips() {
        List<LabVessel> list = new ArrayList<>();
        List<LabEvent> labEvents = labEventDao.findByEventType(LabEventType.INFINIUM_XSTAIN);
        for (LabEvent labEvent: labEvents) {
            boolean foundAllStartedEvent = false;
            LabVessel labVessel = labEvent.getInPlaceLabVessel();
            Set<LabEvent> inPlaceLabEvents = labVessel.getInPlaceLabEvents();
            for (LabEvent inPlaceLabEvent: inPlaceLabEvents) {
                if (inPlaceLabEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_ALL_STARTED) {
                    foundAllStartedEvent = true;
                    break;
                }
            }
            if (!foundAllStartedEvent) {
                list.add(labVessel);
            }
        }
        return list;
    }
}
