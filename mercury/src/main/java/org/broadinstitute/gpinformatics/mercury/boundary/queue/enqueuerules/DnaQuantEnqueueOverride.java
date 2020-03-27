package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.bsp.client.response.ExomeExpressCheckResponse;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Logic for overriding default handling for enqueueing for the Pico Queue.
 */
@RequestScoped
public class DnaQuantEnqueueOverride extends AbstractEnqueueOverride {

    @Inject
    private BSPRestClient bspRestClientImpl;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    public DnaQuantEnqueueOverride() {
    }

    public DnaQuantEnqueueOverride(BSPRestClient bspRestClientImpl) {
        this.bspRestClientImpl = bspRestClientImpl;
    }

    /**
     * Currently only specialization is if something is FFPE
     */
    @Nullable
    public QueueSpecialization determineDnaQuantQueueSpecialization(Collection<LabVessel> targetLabVessels) {
        List<MercurySample> mercurySamples = new ArrayList<>();

        QueueSpecialization queueSpecialization = null;

        for (LabVessel labVessel : targetLabVessels) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                mercurySamples.add(sampleInstanceV2.getNearestMercurySample());
            }
        }

        Map<String, SampleData> sampleDataMap = sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, BSPSampleSearchColumn.MATERIAL_TYPE, BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE);

        // Check for FFPE
        for (SampleData sampleData : sampleDataMap.values()) {
            if (Boolean.TRUE.equals(sampleData.getFfpeStatus())) {
                queueSpecialization = QueueSpecialization.FFPE;
            }
        }
        return queueSpecialization;
    }

    @NotNull
    @Override
    public QueuePriority[] getQueuePriorityOrder() {
        return new QueuePriority[] { QueuePriority.CLIA, QueuePriority.EXOME_EXPRESS };
    }

    /**
     * Current special priority types are: CLIA & ExomeExpress.
     */
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

                if (response != null && response.containsAnyExomeExpressSamples()) {
                    queuePriority = QueuePriority.EXOME_EXPRESS;
                }
            }
        }

        return queuePriority;
    }
}
