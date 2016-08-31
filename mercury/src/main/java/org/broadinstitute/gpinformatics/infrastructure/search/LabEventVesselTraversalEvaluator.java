package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Functionality required to traverse ancestor and descendant events of a set of starting lab vessels.
 * Starts with a List of LabVessel objects and returns an event date-location-disambiguator sorted List
 *    of labEventID (Long) values to use for pagination.
 * (As opposed to LabEventTraversalEvaluator which starts with LabEvent entities)
 * Directionality implemented in AncestorTraversalEvaluator and DescendantTraversalEvaluator nested classes
 */
public class LabEventVesselTraversalEvaluator extends TraversalEvaluator {

    public LabEventVesselTraversalEvaluator(){ }

    /**
     * Find events for a supplied list of lab vessels.
     * Traverse ancestor and/or descendants of supplied lab vessel list (or neither if no options selected).
     * @param rootEntities A group of starting vessels from which to obtain ancestor or descendant lab events
     * @param searchInstance The search instance used for this search.  <br />
     *                       Used to determine if ancestors, and/or descendants, or neither are to be added to results.
     * @return The lab events related to the list of vessels supplied
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities, SearchInstance searchInstance) {

        List<LabVessel> rootEventVessels = (List<LabVessel>) rootEntities;
        Set sortedSet = new TreeSet<>( LabEvent.BY_EVENT_DATE_LOC );
        List<String> lcsetNames = null;
        for (SearchInstance.SearchValue searchValue : searchInstance.getSearchValues()) {
            if (searchValue.getSearchTerm().getName().equals(
                    LabEventSearchDefinition.MultiRefTerm.LCSET.getTermRefName())) {
                lcsetNames = searchValue.getValues();
            }
        }


        // Get base events for vessels
        for( LabVessel vessel : rootEventVessels ) {
            sortedSet.addAll(vessel.getEvents());
        }

        if( searchInstance.getTraversalEvaluatorValues()
                .get(LabEventSearchDefinition.TraversalEvaluatorName.ANCESTORS.getId()) ) {
            sortedSet.addAll(traverseRootVessels(rootEventVessels,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, lcsetNames));
        }

        if( searchInstance.getTraversalEvaluatorValues()
                .get(LabEventSearchDefinition.TraversalEvaluatorName.DESCENDANTS.getId()) ) {
            sortedSet.addAll(traverseRootVessels(rootEventVessels,
                    TransferTraverserCriteria.TraversalDirection.Descendants, lcsetNames));
        }

        // Apply in-memory search terms from searchInstance
        SearchTerm.Evaluator<Boolean> traversalFilterExpression = null;
        for (SearchInstance.SearchValue searchValue : searchInstance.getSearchValues()) {
            for (SearchInstance.SearchValue value : searchValue.getChildren()) {
                traversalFilterExpression = value.getSearchTerm().getTraversalFilterExpression();
                break;
            }
        }
        if (traversalFilterExpression != null) {
            Iterator<LabEvent> iterator = sortedSet.iterator();
            while (iterator.hasNext()) {
                LabEvent labEvent = iterator.next();
                if (!traversalFilterExpression.evaluate(labEvent, searchInstance.getEvalContext())) {
                    iterator.remove();
                }
            }
        }

        return sortedSet;
    }

    /**
     * Convert a collection of LabEvent objects to ids (Long)
     */
    @Override
    public List<Long> buildEntityIdList( Set entities ) {
        List<Long> idList = new ArrayList<>();
        for( LabEvent labEvent : (Set<LabEvent>)entities ) {
            idList.add(labEvent.getLabEventId());
        }
        return idList;
    };

    /**
     * Gather all chain of custody events for a list of lab vessels
     * @param rootVessels The list of lab vessels produced by the alternate search definition criteria
     * @param traversalDirection The traversal direction to use for chain of custody events
     * @param lcsetNames list of LCSETs by which to filter events, no filtering if empty
     * @return The chain of custody events resulting from traversal
     */
    private Set<LabEvent> traverseRootVessels(List<LabVessel> rootVessels,
            TransferTraverserCriteria.TraversalDirection traversalDirection, List<String> lcsetNames){
        TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                new TransferTraverserCriteria.LabEventDescendantCriteria();
        // todo jmt enable this after Hibernate 5 upgrade, performance is poor with 4
//        eventTraversalCriteria.setLcsetNames(lcsetNames);

        for (LabVessel startingEventVessel : rootVessels) {
            if (startingEventVessel.getContainerRole() != null) {
                traverseContainer(eventTraversalCriteria, startingEventVessel.getContainerRole()
                        , traversalDirection);
            } else {
                startingEventVessel.evaluateCriteria(eventTraversalCriteria
                        , traversalDirection);
            }
        }

        return eventTraversalCriteria.getAllEvents();
    }

    private void traverseContainer(TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria
            , VesselContainer<?> vesselContainer, TransferTraverserCriteria.TraversalDirection traversalDirection){

        // Add any container in place events
        eventTraversalCriteria.getAllEvents().addAll(vesselContainer.getEmbedder().getInPlaceLabEvents());

        if( vesselContainer.getMapPositionToVessel().isEmpty() ) {
            vesselContainer.applyCriteriaToAllPositions(eventTraversalCriteria, traversalDirection);
        } else {
            for (LabVessel targetVessel : vesselContainer.getContainedVessels()) {
                targetVessel.evaluateCriteria(eventTraversalCriteria, traversalDirection);
            }
        }
    }

}
