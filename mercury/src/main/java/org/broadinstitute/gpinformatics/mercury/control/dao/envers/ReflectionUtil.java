package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.persistence.Id;
import javax.persistence.metamodel.SingularAttribute;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReflectionUtil {
    private static final List<Class> mercuryAthenaClasses = new ArrayList<>();
    private static final List<Class> mercuryAthenaEntityClasses = new ArrayList<>();

    private static void setupClasses() {
        if (mercuryAthenaClasses.size() == 0) {
            // Collects all the classes in the athena.entity and mercury.entity packages.
            String[] packagesToScan = new String[] {
                    "org.broadinstitute.gpinformatics.athena.entity",
                    "org.broadinstitute.gpinformatics.mercury.entity"
            };
            for (String packageToScan : packagesToScan) {
                mercuryAthenaClasses.addAll(getClasses(packageToScan));
            }

            // Finds the persistence classes.  First finds then generated classes by searching for ones having
            // SingularAttribute fields.  The persisted entity class has the same name but without the trailing '_'
            List<String> classnames = new ArrayList<>();
            for (Class cls : mercuryAthenaClasses) {
                if (CollectionUtils.isNotEmpty(ReflectionUtil.getFieldsOfType(cls, SingularAttribute.class))) {
                    int idx = cls.getCanonicalName().lastIndexOf("_");
                    if (idx < cls.getCanonicalName().length() - 1) {
                        throw new RuntimeException("Unexpected generated class name: " + cls.getCanonicalName());
                    }
                    classnames.add(cls.getCanonicalName().substring(0, idx));
                }
            }
            for (Class cls : mercuryAthenaClasses) {
                if (classnames.contains(cls.getCanonicalName())) {
                    mercuryAthenaEntityClasses.add(cls);
                }
            }
        }
    }

    /** Returns all classes found in mercury.entity and athena.entity packages. */
    public static List<Class> getMercuryAthenaClasses() {
        setupClasses();
        return mercuryAthenaClasses;
    }

    /** Returns all entity classes found in mercury.entity and athena.entity packages. */
    public static List<Class> getMercuryAthenaEntityClasses() {
        setupClasses();
        return mercuryAthenaEntityClasses;
    }

    /** Finds the classes from the given list that are persisted by Mercury. */
    public static List<String> getEntityClassnames(List<Class> classesToScan) {
        setupClasses();
        List<Class> classes = new ArrayList<>(classesToScan);
        classes.retainAll(mercuryAthenaEntityClasses);
        List<String> entityClassnames = new ArrayList<>();
        for (Class cls : classes) {
            entityClassnames.add(cls.getCanonicalName());
        }
        return entityClassnames;
    }

    /**
     * Extracts the value of entity id from the entity.
     * Returns null if there is no @Id on the class or superclasses.
     * Also returns null if the @Id field is not a Long.
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

    /**
     * Returns a sorted list of EntityField suitable for generic display of a mercury/athena persisted object.
     */
    public static List<EntityField> formatFields(Object entity, Class entityClass) {
        setupClasses();
        List<EntityField> list = new ArrayList<>();
        if (mercuryAthenaEntityClasses.contains(entityClass) && OrmUtil.proxySafeIsInstance(entity, entityClass)) {
            for (Field field : getFields(entityClass)) {
                try {
                    field.setAccessible(true);
                    Type fieldType = field.getGenericType();
                    Class fieldClass = fieldType.getClass();
                    Object fieldObj = field.get(entity);

                    // Checks if the field is some type of collection.
                    if (fieldType instanceof ParameterizedType && fieldObj instanceof Iterable) {
                        Class fieldItemClass = ((ParameterizedType)fieldType).getActualTypeArguments()[0].getClass();
                        List<EntityField> fieldList = new ArrayList<>();
                        for (Object fieldListItem : (Iterable)fieldObj) {
                            fieldList.add(makeListItem(field, fieldListItem, fieldItemClass));
                        }
                        Collections.sort(fieldList, EntityField.BY_VALUE);
                        list.add(new EntityField(field.getName(), null, null, fieldList));
                    } else {
                        EntityField entityField = makeEntityReference(field, entity, entityClass);
                        if (entityField == null) {
                            // Makes a basic value or reference.
                            entityField = new EntityField(field.getName(), makeStringValue(field, entity), null, null);
                        }
                        list.add(entityField);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Reflection cannot access field " + field.getName() +
                                               " on class " + entityClass.getCanonicalName());
                }
            }
        }
        Collections.sort(list, EntityField.BY_NAME);
        return list;
    }

    /** Returns all fields for the class and superclasses. */
    public static Collection<Field> getFields(Class cls) {
        Set<Field> fields = new HashSet<>();
        getFields(fields, cls);
        return fields;
    }

    /** Returns the entity id field (annotated @Id in entity class) from class and superclasses, or null if none. */
    public static Field getEntityIdField(Class cls) {
        return getFieldHavingAnnotation(getFields(cls), Id.class);
    }

    /** Returns the Field having the specified annotation, or null if not found. */
    public static Field getFieldHavingAnnotation(Collection<Field> fields, Class annotationClass) {
        for (Field field : fields) {
            for (Annotation annotation : field.getDeclaredAnnotations()) {
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

    // Handles a list item which can be a reference or a basic type.
    private static EntityField makeListItem(Field field, Object entity, Class entityClass)
            throws IllegalAccessException {

        EntityField entityField = makeEntityReference(field, entity, entityClass);
        if (entityField == null) {
            entityField = new EntityField(field.getName(), makeStringValue(field, entity), null, null);
        }
        return entityField;
    }

    private static EntityField makeEntityReference(Field field, Object entity, Class entityClass) {
        setupClasses();
        if (mercuryAthenaEntityClasses.contains(entityClass)) {
            Long entityId = getEntityId(entity, entityClass);
            String value = String.valueOf(entityId);
            return new EntityField(field.getName(), value, entityClass.getCanonicalName(), null);
        }
        return null;
    }

    private static String makeStringValue(Field field, Object entity) throws IllegalAccessException {
        Type type = field.getGenericType();
        Class cls = type.getClass();
        if (cls.equals(Boolean.class)) {
            return String.valueOf(field.getBoolean(entity));
        }
        if (cls.equals(Byte.class)) {
            return String.valueOf(field.getByte(entity));
        }
        if (cls.equals(Character.class)) {
            return String.valueOf(field.getChar(entity));
        }
        if (cls.equals(Double.class)) {
            return String.valueOf(field.getDouble(entity));
        }
        if (cls.equals(Float.class)) {
            return String.valueOf(field.getFloat(entity));
        }
        if (cls.equals(Integer.class)) {
            return String.valueOf(field.getInt(entity));
        }
        if (cls.equals(Long.class)) {
            return String.valueOf(field.getLong(entity));
        }
        if (cls.equals(Short.class)) {
            return String.valueOf(field.getShort(entity));
        }
        if (field.isEnumConstant()) {
            return String.valueOf(field.getInt(entity)); //xxx todo lookup ParameterizedType?
        }
        return field.get(entity).toString();
    }

    private static void getFields(Collection<Field> fields, Class cls) {
        if (cls != null) {
            fields.addAll(Arrays.asList(cls.getDeclaredFields()));
            getFields(fields, cls.getSuperclass());
        }
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
