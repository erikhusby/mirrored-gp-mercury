package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;

import java.util.Map;
import java.util.Set;

/**
 * This class is the brains the figures out the "why"
 * for a {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel}.
 * If you want to know the {@link org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId} for a given
 * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel},
 * this is your man.
 *
 * The {@link LabVessel}s returned in the Maps in this class are
 * the vessels that were placed into a bucket with an association
 * to a PDO at some point.
 */
public abstract class ProductOrderResolver {

    /**
     * What are all the {@link org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId product orders}
     * for the given {@link LabVessel}?  This will return <b>all</b>
     * product orders.  If you want to filter out only the "active"
     * product orders, take the result and then hit athena for product
     * details and narrow the list.
     *
     * A Map is returned instead of a single {@link org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId product order} because
     * it's often the case that in a pool of samples, each sample comes from a different
     * product order.
     *
     * The keys of the map are the {@link LabVessel root lab vessels}.  Some clients
     * may need to know the mapping between a root sample and its product orders, while
     * others might only flatten the list of {@link org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId product orders}.
     * @param labVessel
     * @return
     */
    public abstract Map<LabVessel,ProductOrderId> findProductOrders(LabVessel labVessel);

    /**
     * What's the {@link WorkflowStepDef} for the given  {@link LabVessel}
     * for the given {@link ProductOrderId product order}?  Given a product
     * order and a root lab vessel, there should be only one unambiguous
     * workflow state.  If multiple workflow states are somehow found for a root, we have
     * failed to meet a major requirement.
     *
     * The input {@link LabVessel} need not be a root.  A Map is returned so that
     * the client can ask for the status of any {@link LabVessel} and get a root-specific
     * answer.  This seems overly complex at first, but consider something like QTP,
     * where multiple libraries from different {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef}s
     * may come together on a {@link org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaRunChamber}.
     * @param productOrder
     * @param root
     * @return
     */
    public abstract Map<LabVessel,WorkflowStepDef> getStatus(ProductOrderId productOrder,
                                                    LabVessel root);

}
