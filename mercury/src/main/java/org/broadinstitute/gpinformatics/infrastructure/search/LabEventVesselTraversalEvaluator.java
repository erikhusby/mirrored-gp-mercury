package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
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

        // True if "In-Place Vessel Barcode" search term is present
        boolean inPlaceVesselsOnly = false;
        for( SearchInstance.SearchValue searchValue : searchInstance.getSearchValues() ) {
            if( searchValue.getName().equals("In-Place Vessel Barcode")) {
                inPlaceVesselsOnly = true;
                break;
            }
        }

        // Get base events for vessels
        for( LabVessel vessel : rootEventVessels ) {
            if( inPlaceVesselsOnly ) {
                sortedSet.addAll(vessel.getInPlaceAndTransferToEvents());
            } else {
                sortedSet.addAll(vessel.getEvents());
            }
        }

        if( searchInstance.getTraversalEvaluatorValues()
                .get(LabEventSearchDefinition.TraversalEvaluatorName.ANCESTORS.getId()) ) {

            TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                    new TransferTraverserCriteria.LabEventDescendantCriteria();

            for (LabVessel startingEventVessel : rootEventVessels) {
                if (startingEventVessel.getContainerRole() != null) {
                    traverseContainer(eventTraversalCriteria, startingEventVessel.getContainerRole()
                            , TransferTraverserCriteria.TraversalDirection.Ancestors);
                } else {
                    startingEventVessel.evaluateCriteria(eventTraversalCriteria
                            , TransferTraverserCriteria.TraversalDirection.Ancestors);
                }
            }
            sortedSet.addAll(eventTraversalCriteria.getAllEvents());
        }

        if( searchInstance.getTraversalEvaluatorValues()
                .get(LabEventSearchDefinition.TraversalEvaluatorName.DESCENDANTS.getId()) ) {

            TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                    new TransferTraverserCriteria.LabEventDescendantCriteria();

            for (LabVessel startingEventVessel : rootEventVessels) {
                if (startingEventVessel.getContainerRole() != null) {
                    traverseContainer(eventTraversalCriteria, startingEventVessel.getContainerRole()
                            , TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    startingEventVessel.evaluateCriteria(eventTraversalCriteria
                            , TransferTraverserCriteria.TraversalDirection.Descendants);
                }
            }
            sortedSet.addAll( eventTraversalCriteria.getAllEvents());
        }
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

    private void traverseContainer( TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria
            , VesselContainer<?> vesselContainer, TransferTraverserCriteria.TraversalDirection traversalDirection){

        // Add any container in place events
        eventTraversalCriteria.getAllEvents().addAll(vesselContainer.getEmbedder().getInPlaceLabEvents());

        if( vesselContainer.getMapPositionToVessel().isEmpty() ) {
            for( VesselPosition vesselPosition
                    : vesselContainer.getEmbedder().getVesselGeometry().getVesselPositions() ) {
                vesselContainer.evaluateCriteria(vesselPosition, eventTraversalCriteria,
                        traversalDirection, null, 0);
            }
        } else {
            for (LabVessel targetVessel : vesselContainer.getContainedVessels()) {
                targetVessel.evaluateCriteria(eventTraversalCriteria, traversalDirection);
            }
        }
    }

}
