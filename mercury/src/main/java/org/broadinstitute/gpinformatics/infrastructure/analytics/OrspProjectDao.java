/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.analytics;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProjectConsent;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProjectConsentKey_;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProjectConsent_;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject_;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;

import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * DAO for accessing ORSP Projects. The table that this DAO queries is a snapshot of the result from the ORSP Portal
 * project web service (http://bussysprod:8080/orsp/api/projects) that is refreshed periodically by the
 * analytics.tiger.OrspProjectAgent TigerETL script.
 *
 * OrspProject contains some properties that are converted to enum values for use by Mercury code. Since the range of
 * possible values is in the domain of the ORSP Portal, it's possible that Mercury will not have all enum values. In
 * order to avoid null enum values or exceptions being thrown during conversion, this DAO will only ever select rows
 * that have known mappings to enum values.
 */
@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OrspProjectDao {

    @PersistenceContext(unitName = "analytics_pu")
    private EntityManager entityManager;

    /**
     * Finds an ORSP project by its ORSP ID, e.g., ORSP-123. This is for finding ORSP projects to associate with
     * research projects and product orders. Therefore, projects with type "Consent Group" are not returned.
     *
     * @param id    the ORSP ID
     * @return the ORSP project, or null if not found
     */
    public OrspProject findByKey(String id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrspProject> criteria = cb.createQuery(OrspProject.class);
        Root<OrspProject> orspProject = criteria.from(OrspProject.class);
        criteria.select(orspProject)
                .where(cb.and(
                        cb.equal(orspProject.get(OrspProject_.projectKey), id),
                        restrictType(orspProject)));
        OrspProject project;
        try {
            project = entityManager.createQuery(criteria).getSingleResult();
        } catch (NoResultException e) {
            project = null;
        }
        return project;
    }

    /**
     * Finds all ORSP projects, including ones that aren't currently able to be used in Mercury because of their status
     * in the ORSP Portal.
     *
     * @return all ORSP Projects from the ORSP Portal web service
     */
    public List<OrspProject> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrspProject> criteria = cb.createQuery(OrspProject.class);
        Root<OrspProject> orspProject = criteria.from(OrspProject.class);
        criteria.select(orspProject).where(restrictType(orspProject));

        try {
            return entityManager.createQuery(criteria).getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Finds suggested ORSP Projects for the given samples. Suggestions are made based on the samples' collections which
     * come from SampleData. It is recommended to pre-fetch the sample data in bulk before calling this method in order
     * to avoid the data being fetched one sample at a time.
     *
     * @param samples    the samples to suggest ORSP Projects for
     * @return the ORSP Projects that apply to one or more of the samples
     */
    public List<OrspProject> findBySamples(Collection<? extends AbstractSample> samples) {
        Collection<String> sampleCollections = Collections2.transform(samples, new Function<AbstractSample, String>() {
            @Override
            public String apply(@Nullable AbstractSample input) {
                return input.getSampleData().getCollectionId();
            }
        });

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrspProject> criteria = cb.createQuery(OrspProject.class);
        Root<OrspProject> orspProject = criteria.from(OrspProject.class);
        CollectionJoin<OrspProject, OrspProjectConsent> consents = orspProject.join(OrspProject_.consents);
        criteria.select(orspProject)
                .where(cb.and(
                        consents.get(OrspProjectConsent_.key).get(OrspProjectConsentKey_.sampleCollection)
                                .in(new HashSet<>(sampleCollections)),
                        restrictType(orspProject)));
        try {
            return entityManager.createQuery(criteria).getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
    }

    private Predicate restrictType(Root<OrspProject> orspProject) {
        return orspProject.get(OrspProject_.rawType).in(RegulatoryInfo.Type.getOrspServiceTypeIds());
    }
}
