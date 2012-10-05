package org.broadinstitute.gpinformatics.mercury.entity;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.HashSet;
import java.util.Set;

/**
 * For product work (as opposed to development work), this
 * is what we use to figure out the POs for a {@link LabVessel}.
 */
public class StandardPOResolver extends ProductOrderResolver {
    @Override
    public Set<ProductOrderId> findProductOrders(LabVessel labVessel) {
        // todo get traverser stuff moved to LabVessel



        for (LabEvent labEvent : labVessel.getEvents()) {
            if (labEvent.getProductOrderId() != null) {

            }
        }

    }

    @Override
    public WorkflowStepDef getStatus(ProductOrderId productOrder, LabVessel root) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
