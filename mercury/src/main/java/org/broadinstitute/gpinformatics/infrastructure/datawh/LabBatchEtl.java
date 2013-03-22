package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
public class LabBatchEtl extends GenericEntityEtl<LabBatch, LabBatch> {

    public LabBatchEtl() {
        entityClass = LabBatch.class;
        baseFilename = "lab_batch";
    }

    @Inject
    public LabBatchEtl(LabBatchDAO d) {
        this();
        dao = d;
    }

    @Override
    Long entityId(LabBatch entity) {
        return entity.getLabBatchId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(LabBatch_.labBatchId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabBatch.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabBatch entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getLabBatchId(),
                format(entity.getBatchName())
        );
    }
}
