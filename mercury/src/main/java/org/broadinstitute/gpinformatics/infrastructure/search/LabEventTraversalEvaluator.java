package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Functionality required to traverse ancestor and descendant events of a set of starting lab events.
 * Starts with a List of LabEvent objects and returns an event date-location-disambiguator sorted List
 *    of labEventID (Long) values to use for pagination.
 * Directionality implemented in AncestorTraversalEvaluator and DescendantTraversalEvaluator nested classes
 */
public abstract class LabEventTraversalEvaluator extends TraversalEvaluator {

    protected TransferTraverserCriteria.TraversalDirection traversalDirection;

    public LabEventTraversalEvaluator(){ }

    /**
     * Traverse ancestor and/or descendants of supplied event list (or neither if no options selected).
     * @param rootEntities
     * @return
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities) {

        TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                new TransferTraverserCriteria.LabEventDescendantCriteria();

        List<LabEvent> rootEvents = (List<LabEvent>) rootEntities;

        // Add each starting event so it gets date sorted with descendants
        for (LabEvent startingEvent : rootEvents) {
            eventTraversalCriteria.getAllEvents().add(startingEvent);
        }

        // Do the traversal
        traverse( eventTraversalCriteria, rootEvents, traversalDirection);

        Set sortedSet = eventTraversalCriteria.getAllEvents();

        return sortedSet;
    }

    /**
     * Convert a collection of LabEvent objects to ids (Long)
     * @param entities
     * @return
     */
    @Override
    public List<Long> buildEntityIdList( Set entities ) {
        List<Long> idList = new ArrayList<>();
        for( LabEvent labEvent : (Set<LabEvent>)entities ) {
            idList.add(labEvent.getLabEventId());
        }
        return idList;
    };

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

    /**
     * Implementation of the Lab Event ancestor traversal evaluator
     */
    public static class AncestorTraversalEvaluator extends LabEventTraversalEvaluator{
        // ID = "ancestorOptionEnabled"
        public AncestorTraversalEvaluator() {
            setHelpNote("Lab Events leading up to a primary Lab Event (ancestors)");
            setLabel("Traverse Ancestors");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Ancestors;
        }
    }

    /**
     * Implementation of the Lab Event descendant traversal evaluator
     */
    public static class DescendantTraversalEvaluator extends LabEventTraversalEvaluator{
        // ID = "descendantOptionEnabled"
        public DescendantTraversalEvaluator() {
            setHelpNote("Lab Events following a primary Lab Event (descendants)");
            setLabel("Traverse Descendants");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Descendants;
        }
    }

}
