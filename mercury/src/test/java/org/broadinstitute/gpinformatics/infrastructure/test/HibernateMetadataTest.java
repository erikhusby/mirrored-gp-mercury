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

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;

public class HibernateMetadataTest extends ContainerTest {

    @PersistenceContext(unitName = "mercury_pu")
    private EntityManager entityManager;

    /**
     * Method to allow user to ignore certain classes in the testEverything method
     * Default is to not ignore any classes
     * user may implement by doing something like:
     * return className.endsWith("GenoTypingExportGeneric")
     *
     * @param className the name of the class to ignore
     *
     * @return
     */
    public Boolean ignoreClass(String className) {
        return Boolean.FALSE;
    }

    /**
     * @throws Exception
     */
    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION, description = "Tests all the hibernate mappings")
    public void testEverything() throws Exception {
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        Map<String, Exception> failedClassExceptionMap = new HashMap<String, Exception>();
        Map metadata = sessionFactory.getAllClassMetadata();

        for (Object o : metadata.values()) {
            String className = null;

            try {
                EntityPersister persister = (EntityPersister) o;
                className = persister.getClassMetadata().getEntityName();
                if (!ignoreClass(className)) {
                    Query query = session.createQuery("from " + className + " c");
                    query.setMaxResults(10);
                    query.list();
                    session.clear();
                }
            } catch (Exception e) {
                failedClassExceptionMap.put(className, e);
            }
        }
        Assert.assertTrue(failedClassExceptionMap.isEmpty(),
                "Problems found with " + failedClassExceptionMap.size() + " classes: \n" + StringUtils
                        .replace(failedClassExceptionMap.toString(), ", ", ",\n"));
    }
}
