/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

public class HibernateMetadataTest extends ContainerTest {
    @PersistenceContext(unitName = "mercury_pu")
    private EntityManager entityManager;

    @PersistenceContext(unitName = "metrics_pu")
    private EntityManager metricsEntityManager;

    /** Add exceptions to this list; the goal is to keep this list empty. */
    private static final String[] ignoredEntities = new String[0];

    /**
     * Entities that should be checked against the metrics persistence unit and, therefore, should not be checked
     * against the mercury persistence unit.
     */
    private static final String[] metricsEntities =
            {"org.broadinstitute.gpinformatics.infrastructure.metrics.Aggregation"};

    /**
     * Method to allow user to ignore certain classes/entities in the test methods.
     * Default is to check everything.
     *
     * @param entityName the name of the entity to check
     * @param blackList  classes that should be ignored
     * @param whiteList  classes that should be included
     * @return true if we should check this entity
     */
    public boolean shouldCheckEntity(String entityName, String[] blackList, String[] whiteList) {
        if (ArrayUtils.contains(ignoredEntities, entityName)) {
            return false;
        } else if (whiteList != null) {
            return ArrayUtils.contains(whiteList, entityName);
        } else {
            return !ArrayUtils.contains(blackList, entityName);
        }
    }

    /**
     * This test iterates through all JPA'd classes validates them
     */
    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, description = "Tests all the hibernate mappings")
    public void testMercuryPersistenceUnit() throws Exception {
        Session session = entityManager.unwrap(Session.class);
        testPersistenceUnit(session, metricsEntities, null);
    }

    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, description = "Tests all the hibernate mappings")
    public void testMetricsPersistenceUnit() throws Exception {
        Session session = metricsEntityManager.unwrap(Session.class);
        testPersistenceUnit(session, null, metricsEntities);
    }

    private void testPersistenceUnit(Session session, String[] blackList, String[] whiteList) {
        SessionFactory sessionFactory = session.getSessionFactory();
        Map<String, String> failedEntityMap = new HashMap<>();
        Map<String, ClassMetadata> metadata = sessionFactory.getAllClassMetadata();
        EntityPersister entityPersister;
        for (Object o : metadata.values()) {
            entityPersister = (EntityPersister) o;
            String className = entityPersister.getClassMetadata().getEntityName();
            if (shouldCheckEntity(className, blackList, whiteList)) {
                try {
                    Query query = session.createQuery("from " + className + " c");
                    query.setMaxResults(10);
                    query.list();
                    session.clear();
                } catch (Exception e) {
                    failedEntityMap.put(className, e.getMessage());
                }
            }
        }
        if (!failedEntityMap.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (String key : failedEntityMap.keySet()) {
                errors.append(String.format("Found problems in entity: %s\n\t%s\n", key, failedEntityMap.get(key)));
            }
            Assert.fail(errors.toString());
        }
    }
}
