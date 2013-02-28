package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Stateful
@RequestScoped
public class LabBatchDAO extends GenericDao {

    /**
     * What are the possible active {@link LabBatch batches} for the
     * set of {@link LabVessel}s?
     * <p/>
     * When a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent} arrives,
     * based entirely on the containers we have to guess at the {@link LabBatch batches} that
     * might be active.  We don't do this one-by-one, but instead exploit the
     * batch of containers in the message because often (at least for tubes), the
     * set of {@link LabVessel containers} in the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent event}
     * itself maps to a single {@link LabBatch batch}.
     * <p/>
     * That's not always the case, however, and it remains to be seen how
     * we deal with stacks of plates.
     *
     * @param vessels
     * @return
     */
    public Collection<LabBatch> guessActiveBatchesForVessels(Collection<? extends LabVessel> vessels) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public LabBatch findByName(String batchName) {
        return findSingle(LabBatch.class, LabBatch_.batchName, batchName);
    }

    public List<LabBatch> findByListIdentifier(List<String> searchList) {
        return findListByList(LabBatch.class, LabBatch_.batchName, searchList);
    }

    public LabBatch findByBusinessKey(String businessKey) {
        return findByName(businessKey);
    }
}
