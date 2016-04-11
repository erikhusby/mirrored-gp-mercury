package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Expands a list of vessels to mutate single plates into their multiple contained wells, useful for showing individual pico measurements on plate wells.
 */
public class PlateContaineeEvaluator extends TraversalEvaluator {

    public PlateContaineeEvaluator(){
        this.setLabel("Break Down Plates by Wells");
        this.setHelpNote("When a plate is returned in the search, if possible, break it down into a row for each well");
    }


    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
        Set<Object> vessels = new HashSet<>();
        for( LabVessel vessel : (List<LabVessel>) rootEntities ) {
            if( vessel.getContainerRole() == null
                    || vessel.getType() != LabVessel.ContainerType.STATIC_PLATE
                    || vessel.getContainerRole().getContainedVessels().isEmpty() ) {
                vessels.add(vessel);
            } else {
                vessels.addAll( vessel.getContainerRole().getContainedVessels() );
            }
        }

        return vessels;
    }

    @Override
    public List<Object> buildEntityIdList(Set<? extends Object> entities) {
        List<Object> idList = new ArrayList<>();

        for( LabVessel vessel : (Set<LabVessel>) entities ) {
            idList.add(vessel.getLabel());
        }

        return idList;
    }
}
