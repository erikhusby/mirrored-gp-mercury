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
 * A user selectable custom traverser to traverse chain of transfers and returns only tubes, strip tubes, and flowcells
 */
public class TubeStripTubeFlowcellTraversalEvaluator extends CustomTraversalEvaluator {

    public TubeStripTubeFlowcellTraversalEvaluator(){
        super("Tubes, Strip Tubes, and Flowcells", "tubesEtcTraverser");
    }

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
    public Set<Object> evaluate(List<? extends Object> rootEntities, TransferTraverserCriteria.TraversalDirection traversalDirection, SearchInstance searchInstance) {
        Set<Object> resultLabVessels = new LinkedHashSet<>();
        List<LabVessel> startingVessels = (List<LabVessel>) rootEntities;
        for (LabVessel startingLabVessel : startingVessels) {
            TubeStripTubeFlowcellTraversalEvaluator.LabVesselCriteria transferTraverserCriteria = new TubeStripTubeFlowcellTraversalEvaluator.LabVesselCriteria();
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

        // Filter out the containers, otherwise we get the DNA plate container in addition to the wells
        for( LabVessel vessel : (Set<LabVessel>) entities ) {
            idList.add(vessel.getLabel());
        }

        return idList;
    }

}
