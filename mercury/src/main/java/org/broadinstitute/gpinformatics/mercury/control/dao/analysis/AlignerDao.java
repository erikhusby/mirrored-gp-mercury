package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@LocalBean
@RequestScoped
public class AlignerDao extends GenericDao implements BusinessKeyFinder<Aligner> {
    public List<Aligner> findAll() {
        return findAll(Aligner.class);
    }

    /**
     * Find the current {@link Aligner} of a given business key.  The name is also the business key.
     *
     * @param businessKey the business key of the {@link Aligner}
     * @return The current {@link Aligner}s if it exists or null if it is not found
     */
    @Override
    public Aligner findByBusinessKey(String businessKey) {
        return findSingle(Aligner.class, Aligner_.name, businessKey);
    }
}
