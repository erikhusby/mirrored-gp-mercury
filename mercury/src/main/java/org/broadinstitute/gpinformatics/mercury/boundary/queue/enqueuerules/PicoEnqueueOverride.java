package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.BSPRestClientImpl;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Logic for overriding default handling for enqueueing for the Pico Queue.
 */
public class PicoEnqueueOverride extends AbstractEnqueueOverride {

    public PicoEnqueueOverride() {
    }

    @Inject
    public PicoEnqueueOverride(BSPRestClientImpl bspRestClientImpl) {
        this.bspRestClientImpl = bspRestClientImpl;
    }

    private BSPRestClientImpl bspRestClientImpl;

    @NotNull
    @Override
    public QueuePriority[] getQueuePriorityOrder() {
        return new QueuePriority[] { QueuePriority.CLIA, QueuePriority.EXOME_EXPRESS };
    }

    @Override
    protected QueuePriority checkForSpecialPriorityType(Collection<MercurySample> mercurySamples) {
        QueuePriority queuePriority = getDefaultPriority();
        List<String> barcodes = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(mercurySamples)) {
            for (MercurySample mercurySample : mercurySamples) {

                // Clia has the highest priority, so once found we can just return it.
                if (mercurySample.isClinicalSample()) {
                    return QueuePriority.CLIA;
                }
                if (!CollectionUtils.isEmpty(mercurySample.getProductOrderSamples())) {
                    for (ProductOrderSample productOrderSample : mercurySample.getProductOrderSamples()) {
                        // Ex Ex. isn't the highest, so we cache to return later
                        if (productOrderSample.getProductOrder().getProduct().isExomeExpress()) {
                            queuePriority = QueuePriority.EXOME_EXPRESS;
                        }
                    }
                }
                if (queuePriority == getDefaultPriority()) {
                    if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                        barcodes.add(mercurySample.getSampleKey());
                    }
                }
            }
        }
        if (queuePriority == getDefaultPriority()) {
            if (!barcodes.isEmpty()) {
                ExomeExpressCheckResponse response = bspRestClientImpl.callExomeExpressCheck(barcodes);

                if (response.containsAnyExomeExpressSamples()) {
                    queuePriority = QueuePriority.EXOME_EXPRESS;
                }
            }
        }

        return queuePriority;
    }
}
