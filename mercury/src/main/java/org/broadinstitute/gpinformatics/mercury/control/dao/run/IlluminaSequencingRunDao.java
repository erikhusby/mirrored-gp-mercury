package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for sequencing runs
 */
@Stateful
@RequestScoped
public class IlluminaSequencingRunDao extends GenericDao{

    public IlluminaSequencingRun findByRunName(String runName) {
        return findSingle(IlluminaSequencingRun.class, IlluminaSequencingRun_.runName, runName);
    }

    public IlluminaSequencingRun findByBarcode(String runBarcode) {
        return findSingle(IlluminaSequencingRun.class, IlluminaSequencingRun_.runBarcode, runBarcode);
    }
}
