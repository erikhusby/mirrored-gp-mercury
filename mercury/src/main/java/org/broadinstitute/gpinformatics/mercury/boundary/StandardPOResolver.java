package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.*;

/**
 * For product work (as opposed to development work), this
 * is what we use to figure out the POs for a {@link LabVessel}.
 */
public class StandardPOResolver extends ProductOrderResolver {

    @Override
    public Map<LabVessel, String> findProductOrders ( LabVessel labVessel ) {
        final Map<LabVessel,String> vesselToPOMap = new HashMap<LabVessel, String>();
        final Collection<? extends LabVessel> allRoots = labVessel.getChainOfCustodyRoots();
        final Set<LabVesselPOHopCount> vesselPOHopCounts = new HashSet<LabVesselPOHopCount>();

        // todo also iterate through non-transfer events
        // todo make this actually walk all the way to root
        for (LabEvent labEvent : labVessel.getTransfersTo()) {
            final String productOrderForEvent = labEvent.getProductOrderId();
            if (productOrderForEvent != null) {
                // todo test scenarios for rack of tubes to plate
                for (LabVessel sourceVessel : labEvent.getSourceLabVessels()) {
                    vesselPOHopCounts.add(new LabVesselPOHopCount(sourceVessel,productOrderForEvent,-1 /* todo hopcount from transfer walker */));
                }
            }
            // else skip it
        }

        // go through all roots from the input vessel and find the nearest
        // event that declares the PO/vessel relationship
        for (LabVessel incomingRoot : allRoots) {
            int minDistance = Integer.MAX_VALUE;
            String productOrderId = null;
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
        throw new RuntimeException("not implemented");
    }

    private  class LabVesselPOHopCount {

        private LabVessel vessel;

        private String po;

        private int hopCount;

        public LabVesselPOHopCount(LabVessel vessel, String productOrderId,int hopCount) {
            this.vessel = vessel;
            this.po = productOrderId;
            this.hopCount = hopCount;
        }

        private LabVessel getVessel() {
            return vessel;
        }

        private int getHopCount() {
            return hopCount;
        }


        private String getProductOrderId () {
            return po;
        }

    }
}
