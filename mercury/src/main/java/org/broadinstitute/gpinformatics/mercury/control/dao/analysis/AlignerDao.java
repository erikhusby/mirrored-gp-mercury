package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@RequestScoped
public class AlignerDao extends GenericDao {

    public List<Aligner> findAll() {
        return super.findAll(Aligner.class);
    }

    public Aligner findByBusinessKey(String value) {
        return findSingle(Aligner.class, Aligner_.name, value);
    }
}
