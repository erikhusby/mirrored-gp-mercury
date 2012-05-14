package org.broadinstitute.sequel.control.dao.workflow;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.LabBatch;

import java.util.Collection;

public interface LabBatchDAO {

    /**
     * What are the possible active {@link LabBatch batches} for the
     * set of {@link LabVessel}s?
     *
     * When a {@link org.broadinstitute.sequel.entity.labevent.LabEvent} arrives,
     * based entirely on the containers we have to guess at the {@link LabBatch batches} that
     * might be active.  We don't do this one-by-one, but instead exploit the
     * batch of containers in the message because often (at least for tubes), the
     * set of {@link LabVessel containers} in the {@link org.broadinstitute.sequel.entity.labevent.LabEvent event}
     * itself maps to a single {@link LabBatch batch}.
     *
     * That's not always the case, however, and it remains to be seen how
     * we deal with stacks of plates.
     * @param vessels
     * @return
     */
    public Collection<LabBatch> guessActiveBatchesForVessels(Collection<? extends LabVessel> vessels);
}
