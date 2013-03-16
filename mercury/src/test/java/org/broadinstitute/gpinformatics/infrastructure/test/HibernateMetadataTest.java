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

import org.apache.commons.lang.ArrayUtils;
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

    /** Add exceptions to this list; the goal is to keep this list empty. */
    private static final String[] ignoredEntities = new String[0];

    /**
     * Method to allow user to ignore certain classes/entities in the testEverything method.
     * Default is to not ignore anything.
     *
     * @param entityName the name of the entity to ignore
     *
     * @return true if we should ignore this entity
     */
    public boolean ignoreEntity(String entityName) {
        return ArrayUtils.contains(ignoredEntities, entityName);
    }

    /**
     * This test iterates through all JPA'd classes validates them
     */
    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, description = "Tests all the hibernate mappings")
    public void testEverything() throws Exception {
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        Map<String, String> failedEntityMap = new HashMap<String, String>();
        Map<String, ClassMetadata> metadata = sessionFactory.getAllClassMetadata();
        EntityPersister entityPersister;
        for (Object o : metadata.values()) {
            entityPersister = (EntityPersister) o;
            String className = entityPersister.getClassMetadata().getEntityName();
            if (!ignoreEntity(className)) {
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
