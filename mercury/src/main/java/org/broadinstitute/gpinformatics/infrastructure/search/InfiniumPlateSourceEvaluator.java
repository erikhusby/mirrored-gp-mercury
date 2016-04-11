package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Expands an array process related vessel (DNA plate, amplification plate, or Infinium chip) by walking back
 * to the DNA plate from the starting vessel(s) and building a list of DNA plate positions.
 */
public class InfiniumPlateSourceEvaluator extends TraversalEvaluator {

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
        // Holds the DNA plate wells
        Set<Object> vessels = new HashSet<>();

        // Get the DNA plate by traversing ancestors of current entities back to the array plating event
        TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                = new TransferTraverserCriteria.VesselForEventTypeCriteria(
                    Collections.singletonList(LabEventType.ARRAY_PLATING_DILUTION), true);

        for( LabVessel vessel : (List<LabVessel>) rootEntities ) {

            if( vessel.getContainerRole() != null ) {
                vessel.getContainerRole().applyCriteriaToAllPositions(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            } else {
                // This shouldn't happen for array vessels, they should al be containers
                vessel.evaluateCriteria(eventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            }

            for(Map.Entry<LabEvent, Set<LabVessel>> eventEntry : eventTypeCriteria.getVesselsForLabEventType().entrySet()) {
                vessels.addAll(eventEntry.getValue());
            }
        }

        return vessels;
    }

    @Override
    public List<Object> buildEntityIdList(Set<? extends Object> entities) {
        List<Object> idList = new ArrayList<>();

        // Filter out the containers, otherwise we get the DNA plate container in addition to the wells
        for( LabVessel vessel : (Set<LabVessel>) entities ) {
            if( vessel.getContainerRole() == null ) {
                idList.add(vessel.getLabel());
            }
        }

        return idList;
    }
}
