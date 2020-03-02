package org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SrsPostDequeueHandler extends AbstractPostDequeueHandler {

    @Override
    public void process(List<QueueEntity> completedQueueEntities) {
        Set<LabVessel> labVessels = completedQueueEntities.stream()
                .map(QueueEntity::getLabVessel)
                .collect(Collectors.toSet());
        if (labVessels.isEmpty()) {
            return;
        }
        Set<String> productTypes = labVessels.stream().flatMap(
                lv -> Arrays.stream(lv.getMetadataValues(Metadata.Key.PRODUCT_TYPE))).collect(Collectors.toSet());
        productTypes.add(MayoManifestEjb.AOU_ARRAY); // TODO Remove
        if (!productTypes.isEmpty()) {
            if (productTypes.size() == 1) {
                String productType = productTypes.iterator().next();
                QueueType queueType = null;
                if (productType.equals(MayoManifestEjb.AOU_ARRAY)) {
                    queueType = QueueType.ARRAY_PLATING;
                } else if (productType.equals(MayoManifestEjb.AOU_GENOME)) {
                    queueType = QueueType.SEQ_PLATING;
                }
                if (queueType != null) {
                    MessageCollection messageCollection = new MessageCollection();
                    QueueEjb queueEjb = ServiceAccessUtility.getBean(QueueEjb.class);
                    queueEjb.enqueueLabVessels(labVessels, queueType,
                             "SRS completed on " + DateUtils.convertDateTimeToString(new Date()),
                            messageCollection, QueueOrigin.OTHER, null);
                }
            }
        }
    }
}
