package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Functionality required to traverse ancestor and/or descendant events of a set of starting lab events.
 * Starts with a List of LabEvent objects and returns an event date-location-disambiguator sorted List
 *    of labEventID (Long) values to use for pagination.
 */
public class LabEventTraversalEvaluator extends ConfigurableSearchDefinition.TraversalEvaluator<List<?>> {

    @Override
    public List<?> evaluate(List<?> rootEntities, boolean doAncestorTraversal, boolean doDescendantTraversal) {

        TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                new TransferTraverserCriteria.LabEventDescendantCriteria();

        List<LabEvent> rootEvents = (List<LabEvent>) rootEntities;

        // Add each starting event so it gets date sorted with descendants
        for (LabEvent startingEvent : rootEvents) {
            eventTraversalCriteria.getAllEvents().add(startingEvent);
        }

        // Do the traversals as selected by user
        if( doDescendantTraversal ) {
            traverse( eventTraversalCriteria, rootEvents, TransferTraverserCriteria.TraversalDirection.Descendants);
        }
        if( doAncestorTraversal ) {
            traverse( eventTraversalCriteria, rootEvents, TransferTraverserCriteria.TraversalDirection.Ancestors);
        }

        Set<LabEvent> sortedSet = eventTraversalCriteria.getAllEvents();
        List<Long> idList = new ArrayList<>();

        // Replace entire original list contents
        for (LabEvent event : sortedSet) {
            idList.add(event.getLabEventId());
        }

        return idList;
    }

    private void traverse( TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria,
                           List<LabEvent> rootEvents, TransferTraverserCriteria.TraversalDirection traversalDirection ){
        for (LabEvent startingEvent : rootEvents) {
            // See if event has an in place lab vessel
            LabVessel vessel = startingEvent.getInPlaceLabVessel();

            Set<LabVessel> eventVessels;

            if( traversalDirection == TransferTraverserCriteria.TraversalDirection.Descendants) {
                eventVessels = startingEvent.getSourceLabVessels();
            } else {
                eventVessels = startingEvent.getTargetLabVessels();
            }

            // If not, pull from transfer(s)
            if( vessel == null ) {
                for( LabVessel xferVessel : eventVessels ) {
                    if( xferVessel.getContainerRole() != null ) {
                        for( LabVessel targetVessel : xferVessel.getContainerRole().getContainedVessels() ) {
                            targetVessel.evaluateCriteria(eventTraversalCriteria, traversalDirection);
                        }
                    } else {
                        xferVessel.evaluateCriteria(eventTraversalCriteria, traversalDirection);
                    }
                }
            } else {
                vessel.evaluateCriteria(eventTraversalCriteria, traversalDirection);
            }
        }
    }
}
