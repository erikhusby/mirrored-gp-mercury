package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Functionality required to traverse a list of events and build a distinct list of reagents.
 * The event list is replaced by a reagent list for processing in the displayversalEvaluator nested classes
 */
public class LcsetReagentTraversalEvaluator extends LabEventTraversalEvaluator {

    public LcsetReagentTraversalEvaluator(){
        traversalDirection = TransferTraverserCriteria.TraversalDirection.Descendants;
    }

    /**
     * Traverse supplied lab event list and capture all reagents
     * @param rootEntities A group of events obtained from LCSET from which to begin descendant traversal
     * @param searchInstance The search instance used for this search (not used in this case)
     * @return
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities, SearchInstance searchInstance) {

        // Use existing event LCSET descendant traversal to find all descendant events
        Set<Object> rootEvents = super.evaluate(rootEntities, searchInstance);

        Set<Object> reagents = new HashSet<>();
        for ( Object event : rootEvents ) {
            // A reagent query for each event is avoided by @BatchSize on LabEvent.reagents
            for( Reagent reagent : ( (LabEvent) event ).getReagents() ) {
                reagents.add(reagent);
            }
        }
        return reagents;
    }

    /**
     * Convert a collection of Reagent objects to unique ids (Long)
     * @param entities
     * @return
     */
    @Override
    public List<Long> buildEntityIdList( Set entities ) {

        // Need to sort list by name because no database sort possible on this search term
        //   alternate search definition.
        // Otherwise, if results are longer than 1 page no sorting will be done.
        List<Reagent> reagentList = new ArrayList<>((Set<Reagent>)entities);

        Collections.sort( reagentList, Reagent.BY_NAME_LOT_EXP );

        List<Long> idList = new ArrayList<>();
        for( Reagent reagent : reagentList ) {
            idList.add( reagent.getReagentId() );
        }

        return idList;
    }

}
