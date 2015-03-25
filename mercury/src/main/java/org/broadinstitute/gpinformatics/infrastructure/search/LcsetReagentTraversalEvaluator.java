package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
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
     * @param rootEntities
     * @return
     */
    @Override
    public Set<Object> evaluate(List<?> rootEntities) {

        // Use existing event LCSET descendant traversal to find all descendant events
        Set<Object> rootEvents = super.evaluate(rootEntities);

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
        Set<Long> idSet = new HashSet<>();
        for( Reagent reagent : (Set<Reagent>)entities ) {
            idSet.add(reagent.getReagentId());
        }
        List<Long> idList = new ArrayList<>();
        idList.addAll(idSet);
        return idList;
    }


}
