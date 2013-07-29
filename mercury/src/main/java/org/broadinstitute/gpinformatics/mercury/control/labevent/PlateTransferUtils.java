package org.broadinstitute.gpinformatics.mercury.control.labevent;

import com.google.common.base.Predicate;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlateTransferUtils {


    /**
     * Returns the shortest paths of LabEvents from the specified labVessel to source vessels satisfying the predicate.
     * In the event that multiple paths of equal length are found, all paths will be returned.
     *
     * The Lists of LabEvents in the returned Set are ordered with the most recent LabEvents first
     * (i.e. the source LabVessel satisfying the predicate is among the sources on the last LabEvent).
     */
    public static Set<List<LabEvent>> shortestPathsToVesselsSatisfyingPredicate(@Nonnull LabVessel labVessel,
                                                                                @Nonnull Predicate<LabVessel> predicate) {

        Set<List<LabEvent>> foundEventLists = new HashSet<>();

        // Keep track of the List of LabEvents that lead to a source potentially satisfying the predicate.
        Set<Map<LabVessel, List<LabEvent>>> currentTransfers = new HashSet<>();

        for (LabEvent labEvent : labVessel.getTransfersTo()) {
            for (LabVessel vessel : labEvent.getSourceLabVessels()) {
                Map<LabVessel, List<LabEvent>> currentTransfer = new HashMap<>();
                currentTransfer.put(vessel, Collections.singletonList(labEvent));
                currentTransfers.add(currentTransfer);
            }
        }

        // Continue until finding LabEvents satisfying the predicate or running out of LabEvents to test.
        while (true) {
            // Search for LabVessels satisfying the predicate.
            for (Map<LabVessel, List<LabEvent>> currentTransfer : currentTransfers) {
                for (Map.Entry<LabVessel, List<LabEvent>> entry : currentTransfer.entrySet()) {
                    if (predicate.apply(entry.getKey())) {
                        foundEventLists.add(entry.getValue());
                    }
                }
            }

            // If any satisfying LabEvents at this depth were found, terminate the search.
            if (!foundEventLists.isEmpty()) {
                break;
            }

            // Search for transfers one level deeper from the current set of transfers.
            Set<Map<LabVessel, List<LabEvent>>> previousTransfers = currentTransfers;
            currentTransfers = new HashSet<>();

            for (Map<LabVessel, List<LabEvent>> previousTransfer : previousTransfers) {

                for (Map.Entry<LabVessel, List<LabEvent>> entry : previousTransfer.entrySet()) {
                    LabVessel vessel = entry.getKey();
                    List<LabEvent> labEventsToVessel = entry.getValue();
                    Map<LabVessel, List<LabEvent>> vesselMap = new HashMap<>();

                    for (LabEvent labEvent : vessel.getTransfersTo()) {
                        for (LabVessel sourceVessel : labEvent.getSourceLabVessels()) {
                            if (vesselMap.containsKey(sourceVessel)) {
                                vesselMap.get(sourceVessel).add(labEvent);
                            } else {
                                ArrayList<LabEvent> labEvents = new ArrayList<>(labEventsToVessel);
                                labEvents.add(labEvent);
                                vesselMap.put(sourceVessel, labEvents);
                            }
                        }
                    }

                    // Add non-empty Maps from vessels to LabEvents to the Set of current transfers.
                    if (!vesselMap.isEmpty()) {
                        currentTransfers.add(vesselMap);
                    }
                }
            }

            // If there are no transfers left to search, break out.
            if (currentTransfers.isEmpty()) {
                break;
            }
        }

        return foundEventLists;
    }


    public static class IsLabVesselARackPredicate implements Predicate<LabVessel> {
        @Override
        public boolean apply(@Nullable LabVessel labVessel) {
            return labVessel != null && labVessel.getType() == LabVessel.ContainerType.TUBE_FORMATION;
        }
    }
}
