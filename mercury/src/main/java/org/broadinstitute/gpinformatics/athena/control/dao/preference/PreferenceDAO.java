/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.control.dao.preference;

import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO to save and retrieve Preference objects.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class PreferenceDAO extends GenericDao {

    public List<Preference> getUserPreferences(@Nonnull Long associatedUser) throws Exception {
        return getUserPreferences(associatedUser, null);
    }

    public List<Preference> getUserPreferences(
        @Nonnull Long associatedUser, @Nullable PreferenceType preferenceType) throws Exception {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Preference> criteriaQuery = criteriaBuilder.createQuery(Preference.class);
        Root<Preference> preferenceRoot = criteriaQuery.from(Preference.class);

        List<Predicate> terms = new ArrayList<Predicate>();
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.associatedUser), associatedUser));
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.preferenceType), preferenceType));

        return populateOrderedPreferences(entityManager, criteriaBuilder, criteriaQuery, preferenceRoot, terms);
    }

    public List<Preference> getPreferencesByObjects(
        @Nonnull Long object1Id, @Nullable Long object2Id, @Nonnull PreferenceType preferenceType) throws Exception {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Preference> criteriaQuery = criteriaBuilder.createQuery(Preference.class);
        Root<Preference> preferenceRoot = criteriaQuery.from(Preference.class);

        List<Predicate> terms = new ArrayList<Predicate>();
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.object1Id), object1Id));
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.preferenceType), preferenceType));

        if (object2Id != null) {
            terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.object2Id), object2Id));
        }

        return populateOrderedPreferences(entityManager, criteriaBuilder, criteriaQuery, preferenceRoot, terms);
    }

    private List<Preference> populateOrderedPreferences(
            EntityManager entityManager, CriteriaBuilder criteriaBuilder, CriteriaQuery<Preference> criteriaQuery,
            Root<Preference> preferenceRoot, List<Predicate> terms) throws Exception {

        criteriaQuery.where(terms.toArray(new Predicate[terms.size()]));

        // Order by the modified date, descending. This allows for updates to push the list up in the recent queue
        criteriaQuery.orderBy(criteriaBuilder.desc(preferenceRoot.get(Preference_.modifiedDate)));

        List<Preference> preferences = entityManager.createQuery(criteriaQuery).getResultList();

        // Populate the data for the preferences
        for (Preference preference : preferences) {
            preference.getPreferenceDefinition();
        }

        return preferences;
    }
}
