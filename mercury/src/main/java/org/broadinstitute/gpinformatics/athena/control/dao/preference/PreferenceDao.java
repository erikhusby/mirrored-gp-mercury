/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
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
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PreferenceDao extends GenericDao {

    /**
     * This grabs ALL preferences that are associated to the specified user.
     *
     * @param associatedUser The user.
     *
     * @return The list of preferences that match.
     *
     * @throws Exception Any errors.
     */
    public List<Preference> getPreferences(@Nonnull Long associatedUser) throws Exception {
        return getPreferences(associatedUser, null);
    }

    /**
     * @param associatedUser The user.
     * @param preferenceType The preference type.
     *
     * @return The list of preferences that match.
     *
     * @throws Exception Any errors.
     */
    public List<Preference> getPreferences(
            @Nonnull Long associatedUser, @Nullable PreferenceType preferenceType) throws Exception {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Preference> criteriaQuery = criteriaBuilder.createQuery(Preference.class);
        Root<Preference> preferenceRoot = criteriaQuery.from(Preference.class);

        List<Predicate> terms = new ArrayList<>();
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.associatedUser), associatedUser));
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.preferenceType), preferenceType));

        return populateOrderedPreferences(criteriaBuilder, criteriaQuery, preferenceRoot, terms);
    }

    /**
     * Get the preferences attached to an object.
     *
     * @param object1Id      The main object.
     * @param object2Id      An optional second object.
     * @param preferenceType The preference type.
     *
     * @return The list of preferences that are found.
     *
     * @throws Exception Any errors.
     */
    public List<Preference> getPreferences(
            @Nonnull Long object1Id, @Nullable Long object2Id, @Nonnull PreferenceType preferenceType)
            throws Exception {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Preference> criteriaQuery = criteriaBuilder.createQuery(Preference.class);
        Root<Preference> preferenceRoot = criteriaQuery.from(Preference.class);

        List<Predicate> terms = new ArrayList<>();
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.object1Id), object1Id));
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.preferenceType), preferenceType));

        if (object2Id != null) {
            terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.object2Id), object2Id));
        }

        return populateOrderedPreferences(criteriaBuilder, criteriaQuery, preferenceRoot, terms);
    }

    /**
     * Get a global preference.
     *
     * @param preferenceType type of preference to get
     *
     * @return a single preference
     *
     */
    public Preference getGlobalPreference(PreferenceType preferenceType) {
        return findSingle(Preference.class, Preference_.preferenceType, preferenceType);
    }

    /**
     * Common code used by all methods to set up the preference criteria.
     *
     * @param criteriaBuilder The builder object.
     * @param criteriaQuery   The query.
     * @param preferenceRoot  The root object for preference.
     * @param terms           All terms.
     *
     * @return The preferences being returned.
     *
     * @throws Exception Any errors.
     */
    private List<Preference> populateOrderedPreferences(CriteriaBuilder criteriaBuilder,
                                                        CriteriaQuery<Preference> criteriaQuery,
                                                        Root<Preference> preferenceRoot, List<Predicate> terms)
            throws Exception {

        EntityManager entityManager = getEntityManager();

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

    /**
     * This grabs ALL preferences of a specific type, regardless of user
     *
     * @param preferenceType The preference type to get.
     *
     * @return The list of preferences that match.
     *
     * @throws Exception Any errors.
     */
    public List<Preference> getPreferences( @Nonnull PreferenceType preferenceType ) throws Exception {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Preference> criteriaQuery = criteriaBuilder.createQuery(Preference.class);
        Root<Preference> preferenceRoot = criteriaQuery.from(Preference.class);

        List<Predicate> terms = new ArrayList<>();
        terms.add(criteriaBuilder.equal(preferenceRoot.get(Preference_.preferenceType), preferenceType));

        return populateOrderedPreferences(criteriaBuilder, criteriaQuery, preferenceRoot, terms);
    }
}
