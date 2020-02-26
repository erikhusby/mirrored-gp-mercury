package org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules;

import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DnaQuantPostDequeueHandler extends AbstractPostDequeueHandler {

    @Inject
    private BSPRestClient bspRestClient;

    @Override
    public void process(List<QueueEntity> completedQueueEntities) {
        if (false) {
            // This code is not used for All of Us
            List<String> sampleIdsToInformBspAbout = new ArrayList<>();

            for (QueueEntity completedQueueEntity : completedQueueEntities) {
                LabVessel labVessel = completedQueueEntity.getLabVessel();
                Set<MercurySample> mercurySamples = labVessel.getMercurySamples();
                for (MercurySample mercurySample : mercurySamples) {
                    if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                        sampleIdsToInformBspAbout.add(mercurySample.getSampleKey());
                    }
                }
            }

            // todo jmt figure out how to inform
//        bspRestClient.informUsersOfPicoCompletion(sampleIdsToInformBspAbout);
        }
    }
}
