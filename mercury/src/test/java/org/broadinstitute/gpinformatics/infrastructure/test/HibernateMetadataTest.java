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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

public class HibernateMetadataTest extends ContainerTest {
    @PersistenceContext(unitName = "mercury_pu")
    private EntityManager entityManager;

    // todo: make this an empty list!
    private static final List<String> ignoredEntities =
            Arrays.asList("org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue_AUD",
                    "WorkCompleteMessage_MessageDataValue_AUD");

    /**
     * Method to allow user to ignore certain classes/entities in the testEverything method
     * Default is to not ignore anything
     *
     * @param entityName the name of the entity to ignore
     *
     * @return true if we should ignore this entity
     */
    public boolean ignoreEntity(String entityName) {
        return ignoredEntities.contains(entityName);
    }

    /**
     * This test iterates through all JPA'd classes validates them
     */
    @Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, description = "Tests all the hibernate mappings")
    public void testEverything() throws Exception {
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        Map<String, String> failedEntityMap = new HashMap<String, String>();
        Map metadata = sessionFactory.getAllClassMetadata();
        EntityPersister entityPersister;
        String className = "";
        for (Object o : metadata.values()) {
            try {
                entityPersister = (EntityPersister) o;
                className = entityPersister.getClassMetadata().getEntityName();
                if (!ignoreEntity(className)) {
                    Query query = session.createQuery("from " + className + " c");
                    query.setMaxResults(10);
                    query.list();
                    session.clear();
                }
            } catch (Exception e) {
                failedEntityMap.put(className, e.getMessage());
            }
        }
        if (!failedEntityMap.isEmpty()) {
            String errors = "";
            for (String key : failedEntityMap.keySet()) {
                errors += String.format("Found problems in entity: %s\n\t%s\n", key, failedEntityMap.get(key));
            }
            Assert.fail(errors);
        }
    }
}
