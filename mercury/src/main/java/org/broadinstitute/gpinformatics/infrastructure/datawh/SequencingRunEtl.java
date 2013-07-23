package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;

@Stateful
public class SequencingRunEtl extends GenericEntityEtl<SequencingRun, SequencingRun> {

    public SequencingRunEtl() {
    }

    @Inject
    public SequencingRunEtl(IlluminaSequencingRunDao dao) {
        super(SequencingRun.class, "sequencing_run", "sequencing_run_aud", "sequencing_run_id", dao);
    }

    @Override
    Long entityId(SequencingRun entity) {
        return entity.getSequencingRunId();
    }

    @Override
    Path rootId(Root<SequencingRun> root) {
        return root.get(SequencingRun_.sequencingRunId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(SequencingRun.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, SequencingRun entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, SequencingRun entity) {
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
