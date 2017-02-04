package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Traverses ancestor and descendant vessels of a set of starting vessels.
 */
public class LabVesselTraversalEvaluator extends TraversalEvaluator {

    protected TransferTraverserCriteria.TraversalDirection traversalDirection;

    private static class LabVesselCriteria extends TransferTraverserCriteria {
        private final Map<Integer, List<LabVessel>> labVesselAtHopCount = new TreeMap<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<>();
                labVesselAtHopCount.put(context.getHopCount(), vesselList);
            }


            LabVessel contextVessel = context.getContextVessel();
            if ( contextVessel != null ) {
                if (contextVessel.getType() == LabVessel.ContainerType.TUBE) {
                    vesselList.add(contextVessel);
                }
            } else {
                LabVessel embedder = context.getContextVesselContainer().getEmbedder();
                if (embedder.getType() == LabVessel.ContainerType.FLOWCELL ||
                        embedder.getType() == LabVessel.ContainerType.STRIP_TUBE) {
                    vesselList.add(embedder);
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<LabVessel> getLabVessels() {
            Set<LabVessel> ancestors = new LinkedHashSet<>();
            // Vessel sets sorted by hop count
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                ancestors.addAll(vesselList);
            }
            return ancestors;
        }
    }


    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
        Set<Object> resultLabVessels = new LinkedHashSet<>();
        List<LabVessel> startingVessels = (List<LabVessel>) rootEntities;
        for (LabVessel startingLabVessel : startingVessels) {
            LabVesselCriteria transferTraverserCriteria = new LabVesselCriteria();
            startingLabVessel.evaluateCriteria(transferTraverserCriteria, traversalDirection);
            Collection<LabVessel> labVessels = transferTraverserCriteria.getLabVessels();
            for (LabVessel labVessel : labVessels) {
                searchInstance.getEvalContext().getPagination().addExtraIdInfo(labVessel.getLabel(),
                        startingLabVessel.getLabel());
            }

            resultLabVessels.addAll(labVessels);
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
     * Implementation of the Lab Event ancestor traversal evaluator
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
     * Implementation of the Lab Event descendant traversal evaluator
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
