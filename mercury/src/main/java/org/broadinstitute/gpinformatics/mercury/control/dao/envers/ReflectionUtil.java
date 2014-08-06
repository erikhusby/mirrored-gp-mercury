package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class ReflectionUtil {

    /**
     * Returns the entity id of the persisted entity, or null if no @Id present, or not a Long type.
     */
    public static Long getEntityId(Object entity, Class cls) {
        Field field = getEntityIdField(cls);
        if (field != null) {
            if (field.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
                try {
                    field.setAccessible(true);
                    return (Long)field.get(entity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Reflection cannot access field " + field.getName() +
                                               " on class " + cls.getCanonicalName());
                }
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Cannot find @Id annotation on class " + cls.getCanonicalName());
        }
    }

    /** Returns the entity id (annotated @Id in entity class) from class, using its superclasses if necessary. */
    public static Field getEntityIdField(Class cls) {
        if (cls != null) {
            Field field = getFieldHavingAnnotation(cls, Id.class);
            if (field != null) {
                return field;
            }
            return getEntityIdField(cls.getSuperclass());
        }
        return null;
    }

    /** Returns the Field having the specified annotation, or null if not found. */
    public static Field getFieldHavingAnnotation(Class objectClass, Class annotationClass) {
        for (Field field : objectClass.getDeclaredFields()) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getCanonicalName().equals(annotationClass.getCanonicalName())) {
                    return field;
                }
            }
        }
        return null;
    }

    /** Returns all Fields having the given type. */
    public static Collection<Field> getFieldsOfType(Class objectClass, Class fieldType) {
        Collection<Field> fields = new ArrayList<>();
        for (Field field : objectClass.getDeclaredFields()) {
            if (fieldType != null && field.getType().getCanonicalName().equals(fieldType.getCanonicalName())) {
                fields.add(field);
            }
        }
        return fields;
    }

    public static List<Class> getMercuryAthenaClasses() {
        String[] packagesToScan = new String[] {
                "org.broadinstitute.gpinformatics.athena.entity",
                "org.broadinstitute.gpinformatics.mercury.entity"
        };
        List<Class> classesFromPkg = new ArrayList<>();
        for (String packageToScan : packagesToScan) {
            classesFromPkg.addAll(getClasses(packageToScan));
        }
        return classesFromPkg;
    }

    public static List<String> getEntityClassnames(List<Class> classesToScan) {
        List<String> entityClassnames = new ArrayList<>();

        // Finds the persistence classes by searching for SingularAttribute fields.
        // That class is generated, but strip off the trailing '_' and that's the entity class.
        for (Class cls : classesToScan) {
            if (CollectionUtils.isNotEmpty(ReflectionUtil.getFieldsOfType(cls, SingularAttribute.class))) {
                int idx = cls.getCanonicalName().lastIndexOf("_");
                if (idx < cls.getCanonicalName().length() - 1) {
                    throw new RuntimeException("Unexpected generated class name: " + cls.getCanonicalName());
                }
                entityClassnames.add(cls.getCanonicalName().substring(0, idx));
            }
        }
        return entityClassnames;
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     */
    private static Collection<Class> getClasses(String packageName) {
        Collection<Class> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Collection<File> dirs = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot load " + path, e);
        }

        for (File directory : dirs) {
            try {
                classes.addAll(recurseDirectories(directory, packageName));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return classes;
    }

    /** Returns all classes in the directory hierarchy. */
    private static List<Class> recurseDirectories(File directory, String packageName) {
        List<Class> classes = new ArrayList<>();
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    if (!file.getName().contains(".")) {
                        classes.addAll(recurseDirectories(file, packageName + "." + file.getName()));
                    }
                } else if (file.getName().endsWith(".class")) {
                    int idx = file.getName().lastIndexOf(".class");
                    String classname = packageName + '.' + file.getName().substring(0, idx);
                    try {
                        classes.add(Class.forName(classname));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unknown class '" + classname + "' : " + e);
                    }
                }
            }
        }
        return classes;
    }


}
