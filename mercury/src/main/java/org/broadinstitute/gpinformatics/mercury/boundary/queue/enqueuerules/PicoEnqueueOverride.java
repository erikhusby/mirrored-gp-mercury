package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Logic for overriding default handling for enqueueing for the Pico Queue.
 */
public class PicoEnqueueOverride extends AbstractEnqueueOverride {

    @NotNull
    @Override
    public QueuePriority[] getQueuePriorityOrder() {
        return new QueuePriority[] { QueuePriority.CLIA, QueuePriority.EXOME_EXPRESS };
    }

    /**
     * Allowed types are CLIA, EXOME_EXPRESS, and STANDARD
     */
    @Override
    protected QueuePriority determineQueuePriorityType(QueueGrouping queueGrouping) {
        QueuePriority finalPriorityType = QueuePriority.STANDARD;
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            QueuePriority priorityType = checkForSpecialPriorityType(queueEntity.getLabVessel().getMercurySamples());
            // Clia has highest priority, so we drop as soon as we find.
            if (priorityType == QueuePriority.CLIA) {
                return priorityType;
            } else if (priorityType == QueuePriority.EXOME_EXPRESS) {
                // Ex Ex. isn't the highest, so we cache to return later
                finalPriorityType = priorityType;
            }

            for (SampleInstanceV2 sampleInstanceV2 : queueEntity.getLabVessel().getSampleInstancesV2()) {
                priorityType = checkForSpecialPriorityType(sampleInstanceV2.getRootMercurySamples());
                // Clia has highest priority, so we drop as soon as we find.
                if (priorityType == QueuePriority.CLIA) {
                    return priorityType;
                } else if (priorityType == QueuePriority.EXOME_EXPRESS) {
                    // Ex Ex. isn't the highest, so we cache to return later
                    finalPriorityType = priorityType;
                }
            }
        }

        return finalPriorityType;
    }

    /**
     * Checks the ProductOrderSamples passed in for either of the two special cases.
     *
     * @param mercurySamples    All the ProductOrderSample objects to review
     * @return                  Priority Type found
     */
    private QueuePriority checkForSpecialPriorityType(Collection<MercurySample> mercurySamples) {
        QueuePriority queuePriority = QueuePriority.STANDARD;
        for (MercurySample mercurySample : mercurySamples) {
            // Clia has the highest priority, so once found we can just return it.
            if (mercurySample.isClinicalSample()) {
                return QueuePriority.CLIA;
            }
            if (mercurySample.getProductOrderSamples() != null) {
                for (ProductOrderSample productOrderSample : mercurySample.getProductOrderSamples()) {
                    // Ex Ex. isn't the highest, so we cache to return later
                    if (productOrderSample.getProductOrder().getProduct().isExomeExpress()) {
                        queuePriority = QueuePriority.EXOME_EXPRESS;
                    }
                }
            } else {
                // TODO:  call BSP w/ lab vessels to find out if in ExEx work request
            }
        }
        return queuePriority;
    }
}
