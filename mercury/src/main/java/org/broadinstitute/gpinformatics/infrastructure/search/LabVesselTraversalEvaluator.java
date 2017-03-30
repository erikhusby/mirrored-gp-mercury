package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Traverses all ancestor and descendant vessels of a set of starting vessels. <br />
 * Results include starting vessels
 */
public class LabVesselTraversalEvaluator extends TraversalEvaluator {

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
        Set<Object> resultLabVessels = new LinkedHashSet<>();
        List<LabVessel> startingVessels = (List<LabVessel>) rootEntities;
        resultLabVessels.addAll(startingVessels);

        for (LabVessel startingLabVessel : startingVessels) {
            if( getTraversalDirection() == TransferTraverserCriteria.TraversalDirection.Ancestors ) {
                if( startingLabVessel.getContainerRole() != null ) {
                    TransferTraverserCriteria.LabVesselAncestorCriteria containerCriteria = new TransferTraverserCriteria.LabVesselAncestorCriteria();
                    startingLabVessel.getContainerRole().applyCriteriaToAllPositions(containerCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors );
                    resultLabVessels.addAll(containerCriteria.getLabVesselAncestors());
                } else {
                    resultLabVessels.addAll(startingLabVessel.getAncestorVessels());
                }
            } else {
                if( startingLabVessel.getContainerRole() != null ) {
                    TransferTraverserCriteria.LabVesselDescendantCriteria containerCriteria = new TransferTraverserCriteria.LabVesselDescendantCriteria();
                    startingLabVessel.getContainerRole().applyCriteriaToAllPositions(containerCriteria, TransferTraverserCriteria.TraversalDirection.Descendants );
                    resultLabVessels.addAll(containerCriteria.getLabVesselDescendants());
                } else {
                    resultLabVessels.addAll(startingLabVessel.getDescendantVessels());
                }
            }
        }

        return resultLabVessels;
    }

    @Override
    public List<Object> buildEntityIdList(Set<? extends Object> entities) {
        List<Object> idList = new ArrayList<>();
        for( LabVessel labVessel : (Set<LabVessel>)entities ) {
            idList.add(labVessel.getLabel());
        }
        return idList;
    }

    /**
     * Implementation of the Lab Vessel ancestor traversal evaluator
     */
    public static class AncestorTraversalEvaluator extends LabVesselTraversalEvaluator{
        // ID = "ancestorOptionEnabled"
        public AncestorTraversalEvaluator() {
            setHelpNote("Lab Vessels that precede the starting Lab Vessel in the chain of transfers (ancestors)");
            setLabel("Traverse Ancestors");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Ancestors;
        }
    }

    /**
     * Implementation of the Lab Vessel descendant traversal evaluator
     */
    public static class DescendantTraversalEvaluator extends LabVesselTraversalEvaluator{
        // ID = "descendantOptionEnabled"
        public DescendantTraversalEvaluator() {
            setHelpNote("Lab Vessels that follow the starting Lab Vessel in the chain of transfers (descendants)");
            setLabel("Traverse Descendants");
            traversalDirection = TransferTraverserCriteria.TraversalDirection.Descendants;
        }
    }
}
