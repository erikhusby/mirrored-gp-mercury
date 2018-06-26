package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SequencingRunEtl extends GenericEntityEtl<IlluminaSequencingRun, IlluminaSequencingRun> {

    public SequencingRunEtl() {
    }

    @Inject
    public SequencingRunEtl(IlluminaSequencingRunDao dao) {
        super(IlluminaSequencingRun.class, "sequencing_run", "sequencing_run_aud", "sequencing_run_id", dao);
    }

    @Override
    Long entityId(IlluminaSequencingRun entity) {
        return entity.getSequencingRunId();
    }

    @Override
    Path rootId(Root<IlluminaSequencingRun> root) {
        return root.get(SequencingRun_.sequencingRunId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(IlluminaSequencingRun.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, IlluminaSequencingRun entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, IlluminaSequencingRun entity) {
        Collection<String> records = new ArrayList<>();

        if (entity != null) {
            records.add(genericRecord(etlDateStr, isDelete,
                    entity.getSequencingRunId(),
                    format(entity.getRunName()),
                    format(entity.getRunBarcode()),
                    format(entity.getRunDate()),
                    format(entity.getMachineName()),
                    format(entity.getSetupReadStructure()),
                    format(entity.getActualReadStructure())
            ));
        }

        return records;
    }
}
