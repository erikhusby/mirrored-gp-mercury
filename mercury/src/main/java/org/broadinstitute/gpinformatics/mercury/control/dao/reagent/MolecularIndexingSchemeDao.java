package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

/**
 * A collection of methods that query for MolecularIndexingSchemes. For all
 * find*IndexScheme() methods, the methods run queries that locate a
 * MolecularIndexingScheme having one, two or three component indexes, each
 * with the supplied index sequence in the given position. The
 * indexPosition* argument must be a string generated by a call to
 * IndexPosition.name().
 */
@Stateful
@RequestScoped
public class MolecularIndexingSchemeDao extends GenericDao {

    /**
     * @see MolecularIndexingSchemeDao
     */
    public MolecularIndexingScheme findSingleIndexScheme(
            String indexPosition,
            String indexSequence) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery(
                "MolecularIndexingScheme.findSingleIndexScheme");
        query.setParameter("indexPosition", indexPosition);
        query.setParameter("indexSequence", indexSequence);
        MolecularIndexingScheme molecularIndexingScheme = null;
        try {
            molecularIndexingScheme = (MolecularIndexingScheme) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndexingScheme;
    }

    /**
     * @see MolecularIndexingSchemeDao
     */
    public MolecularIndexingScheme findDualIndexScheme(
            String indexPosition1,
            String indexSequence1,
            String indexPosition2,
            String indexSequence2) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery(
                "MolecularIndexingScheme.findDualIndexScheme");
        query.setParameter("indexPosition1", indexPosition1);
        query.setParameter("indexSequence1", indexSequence1);
        query.setParameter("indexPosition2", indexPosition2);
        query.setParameter("indexSequence2", indexSequence2);
        MolecularIndexingScheme molecularIndexingScheme = null;
        try {
            molecularIndexingScheme = (MolecularIndexingScheme) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndexingScheme;
    }

    /**
     * @see MolecularIndexingSchemeDao
     */
    public MolecularIndexingScheme findTripleIndexScheme(
            String indexPosition1,
            String indexSequence1,
            String indexPosition2,
            String indexSequence2,
            String indexPosition3,
            String indexSequence3) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery(
                "MolecularIndexingScheme.findTripleIndexScheme");
        query.setParameter("indexPosition1", indexPosition1);
        query.setParameter("indexSequence1", indexSequence1);
        query.setParameter("indexPosition2", indexPosition2);
        query.setParameter("indexSequence2", indexSequence2);
        query.setParameter("indexPosition3", indexPosition3);
        query.setParameter("indexSequence3", indexSequence3);
        MolecularIndexingScheme molecularIndexingScheme = null;
        try {
            molecularIndexingScheme = (MolecularIndexingScheme) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndexingScheme;
    }

    /**
     * Queries and returns a MolecularIndexingScheme with the given name.
     */
    public MolecularIndexingScheme findByName(String name) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery(
                "MolecularIndexingScheme.findByName");
        query.setParameter("name", name);
        MolecularIndexingScheme molecularIndexingScheme = null;
        try {
            molecularIndexingScheme = (MolecularIndexingScheme) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndexingScheme;
    }

    public List<MolecularIndexingScheme> findAllIlluminaSchemes() {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery(
                "MolecularIndexingScheme.findAllIlluminaSchemes");
        return query.getResultList();
    }

}
