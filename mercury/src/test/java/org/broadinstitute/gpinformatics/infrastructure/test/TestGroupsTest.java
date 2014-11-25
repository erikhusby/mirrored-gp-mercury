/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.test;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tests if there are any tests without testGroups. If that is the case then they will not be run!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class TestGroupsTest {

    public void testHasTestGroup() throws ClassNotFoundException, java.io.IOException {
        List<String> errors = new ArrayList<>();
        // Find the path to target/test-classes (use "META-INF/persistence.xml" if you need to find target/classes).
        URL[] urls = ClasspathUrlFinder.findResourceBases("arquillian.xml");
        AnnotationDB db = new AnnotationDB();
        db.scanArchives(urls);
        db.setScanMethodAnnotations(true);
        Set<String> entityClasses =
                db.getAnnotationIndex().get(org.testng.annotations.Test.class.getName());

        for (String entityClass : entityClasses) {
            Class clazz = Class.forName(entityClass);
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation.annotationType().equals(Test.class)) {
                    String[] groups = ((Test) annotation).groups();
                    boolean enabled = ((Test) annotation).enabled();
                    if (!enabled) {
                        errors.add(String.format("Test in class %s has class-level enabled=false, this can prevent " +
                                "@BeforeSuite from being run, which breaks Arquillian.\n", clazz.getName()));
                    }
                    if (groups.length == 0 && enabled) {
                        errors.add(String.format("Test in class %s has no test groups defined.\n", clazz.getName()));
                    }
                }
            }

        }
        Assert.assertTrue(errors.isEmpty(),
                String.format("%d classes found with no group defined: %s", errors.size(), errors.toString()));
    }
}
