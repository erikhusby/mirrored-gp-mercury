package org.broadinstitute.gpinformatics.mercury.entity;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * For product work (as opposed to development work), this
 * is what we use to figure out the POs for a {@link LabVessel}.
 */
public class StandardPOResolver extends ProductOrderResolver {

    @Override
    public Map<LabVessel, ProductOrderId> findProductOrders(LabVessel labVessel) {
        final Map<LabVessel,ProductOrderId> vesselToPOMap = new HashMap<LabVessel, ProductOrderId>();
        final Set<LabVessel> allRoots = labVessel.getChainOfCustodyRoots();
        final Set<LabVesselPOHopCount> vesselPOHopCounts = new HashSet<LabVesselPOHopCount>();

        // todo also iterate through non-transfer events
        // todo make this actually walk all the way to root
        for (LabEvent labEvent : labVessel.getTransfersTo()) {
            final ProductOrderId productOrderForEvent = labEvent.getProductOrderId();
            for (LabVessel sourceVessel : labEvent.getSourceLabVessels()) {
                vesselPOHopCounts.add(new LabVesselPOHopCount(sourceVessel,productOrderForEvent,-1 /* todo hopcount from transfer walker */));
            }


            if (productOrderForEvent != null) {

            }
            // else keep looking
        }

        // go through all roots from the input vessel and find the nearest
        // event that declares the PO/vessel relationship
        for (LabVessel incomingRoot : allRoots) {
            final Set<SampleMetadata> samples = incomingRoot.getSamples();
            int minDistance = Integer.MAX_VALUE;
            ProductOrderId productOrderId = null;
            for (LabVesselPOHopCount vesselPOHopCount : vesselPOHopCounts) {
                for (LabVessel rootVessel : vesselPOHopCount.getVessel().getChainOfCustodyRoots()) {
                    if (incomingRoot.equals(rootVessel)) {
                        // at this point, we have an event which declares the PO/vessel relationship
                        // for a vessel that matches the vessel that the client is asking for
                        if (vesselPOHopCount.getHopCount() < minDistance) {
                            productOrderId = vesselPOHopCount.getProductOrderId();
                            minDistance = vesselPOHopCount.getHopCount();
                        }
                        // else skip it

                    }
                }
            }
            // at this point we should have selected the nearest ancestor event
            // that contains a non-null PO for incomingRoot.
            vesselToPOMap.put(incomingRoot,productOrderId);
        }
        return vesselToPOMap;
    }

    @Override
    public Map<LabVessel, WorkflowStepDef> getStatus(ProductOrderId productOrder, LabVessel root) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private  class LabVesselPOHopCount {

        public LabVesselPOHopCount(LabVessel vessel,ProductOrderId productOrderId,int hopCount) {}

        private LabVessel getVessel();

        private int getHopCount();

        private ProductOrderId getProductOrderId();
    }
}
