package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class AuditReaderDbFreeTest {
    private final Collection<Class> unauditableClasses = new ArrayList<Class>() {{
        // todo make a Long primary key on JiraTicket and remove this special case.
        add(JiraTicket.class);
        // VesselContainer is embeddable.
        add(VesselContainer.class);
    }};


    @Test
    public void areAllEntitiesAuditable() throws Exception {
        String[] packagesToScan = new String[] {
                "org.broadinstitute.gpinformatics.athena.entity",
                "org.broadinstitute.gpinformatics.mercury.entity"
        };
        Collection<Class> classesFromPkg = new ArrayList<>();
        for (String packageToScan : packagesToScan) {
            classesFromPkg.addAll(getClasses(packageToScan));
        }

        Collection<String> failingClasses = new ArrayList<>();
        Collection<String> entityClassnames = new ArrayList<>();

        // Finds the persistence classes by searching for SingularAttribute fields.
        // From this generated class, strip off the trailing '_' and that's the entity class.
        for (Class cls : classesFromPkg) {
            if (CollectionUtils.isNotEmpty(ReflectionUtil.getFieldsOfType(cls, SingularAttribute.class))) {
                int idx = cls.getCanonicalName().lastIndexOf("_");
                if (idx < cls.getCanonicalName().length() - 1) {
                    throw new RuntimeException("Unexpected generated class name: " + cls.getCanonicalName());
                }
                entityClassnames.add(cls.getCanonicalName().substring(0, idx));
            }
        }
        Assert.assertTrue(entityClassnames.size() > 0);

        for (Class cls : classesFromPkg) {
            if (entityClassnames.contains(cls.getCanonicalName()) && !unauditableClasses.contains(cls)) {
                Field field = ReflectionUtil.getFieldHavingAnnotation(cls, Id.class);
                if (field == null) {
                    field = ReflectionUtil.getEntityIdField(cls);
                }
                if (field == null) {
                    failingClasses.add(cls.getName());
                }
            }
        }

        if (failingClasses.size() > 0) {
            Assert.fail("Entity definition error -- missing @Id on Long field which is the primary key in class " +
                        StringUtils.join(failingClasses, ", "));
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     */
    private Collection<Class> getClasses(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(classLoader);
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        Collection<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        Collection<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(recurseDirectories(directory, packageName));
        }
        return classes;
    }

    /** Returns all classes in the directory hierarchy. */
    private static List<Class> recurseDirectories(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    Assert.assertTrue(!file.getName().contains("."),
                            "Unexpected class resource name contains '.': " + file.getName());
                    classes.addAll(recurseDirectories(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    int idx = file.getName().lastIndexOf(".class");
                    String classname = packageName + '.' + file.getName().substring(0, idx);
                    classes.add(Class.forName(classname));
                }
            }
        }
        return classes;
    }
}
