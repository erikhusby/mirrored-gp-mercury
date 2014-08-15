package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
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
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ReflectionUtil {
    private static final Logger logger = Logger.getLogger(ReflectionUtil.class.getName());
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private static final String NULL_REPRESTATION = "null";
    private static final List<Class> mercuryAthenaClasses = new ArrayList<>();
    private static final Map<String, Class> mercuryAthenaEntityClassnameToClass = new HashMap<>();
    private static final Map<Class, List<Field>> mapClassToPersistedFields = new HashMap<>();

    static {
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
            for (Class aClass : mercuryAthenaClasses) {
                if (CollectionUtils.isNotEmpty(getFieldsOfType(aClass, SingularAttribute.class))) {
                    int idx = aClass.getCanonicalName().lastIndexOf("_");
                    if (idx < aClass.getCanonicalName().length() - 1) {
                        throw new RuntimeException("Unexpected generated class name: " + aClass.getCanonicalName());
                    }
                    classnames.add(aClass.getCanonicalName().substring(0, idx));
                }
            }

            for (Class aClass : mercuryAthenaClasses) {
                if (classnames.contains(aClass.getCanonicalName())) {
                    mercuryAthenaEntityClassnameToClass.put(aClass.getCanonicalName(), aClass);
                }
            }
        }
    }

    /** Returns all classes found in mercury.entity and athena.entity packages. */
    public static List<Class> getMercuryAthenaClasses() {
        return mercuryAthenaClasses;
    }

    /** Returns the mercury/athena entity canonical classnames. */
    public static Collection<String> getMercuryAthenaEntityClassnames() {
        return mercuryAthenaEntityClassnameToClass.keySet();
    }

    /** Returns the entity class by canonical classname. */
    public static Class getMercuryAthenaEntityClass(String canonicalClassname) {
        return mercuryAthenaEntityClassnameToClass.get(canonicalClassname);
    }

    /** Finds the classes from the given list that are persisted by Mercury. */
    public static List<String> getEntityClassnames(List<Class> classesToScan) {
        List<Class> classes = new ArrayList<>(classesToScan);
        classes.retainAll(mercuryAthenaEntityClassnameToClass.values());
        List<String> entityClassnames = new ArrayList<>();
        for (Class aClass : classes) {
            entityClassnames.add(aClass.getCanonicalName());
        }
        return entityClassnames;
    }

    // Returns the fields that get persisted for the given entity class.
    // Fields on any superclasses are handled by caller, not here.
    private static List<Field> getPersistedFieldsForClass(Class persistedClass) {
        if (!mercuryAthenaEntityClassnameToClass.values().contains(persistedClass)) {
            return Collections.emptyList();
        }
        List<Field> persistedFields = mapClassToPersistedFields.get(persistedClass);
        // Caches the fields after they are looked up the first time.
        if (persistedFields == null) {
            persistedFields = new ArrayList<>();
            // Goes to the generated jpa model class (classname ending with an "_") and
            // collects the field names, then finds the fields on the persistedClass that
            // have the same name.
            String jpaClassname = persistedClass.getCanonicalName() + "_";
            Class jpaClass;
            try {
                jpaClass = Class.forName(jpaClassname);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Cannot find jpa model class '" + jpaClassname + "'");
            }
            for (Field jpaField : jpaClass.getDeclaredFields()) {
                try {
                    persistedFields.add(persistedClass.getDeclaredField(jpaField.getName()));
                } catch (NoSuchFieldException e) {
                    logger.warning(persistedClass.getSimpleName() + "_ has field " + jpaField.getName() +
                                   " but " + persistedClass.getSimpleName() +
                                   " does not.  Audit trail cannot display it.");
                }
            }
            mapClassToPersistedFields.put(persistedClass, persistedFields);
        }
        return persistedFields;
    }

    private static Set<Field> getPersistedFields(Class aClass) {
        if (aClass == null) {
            return new HashSet<>();
        } else {
            Set<Field> fields = getPersistedFields(aClass.getSuperclass());
            fields.addAll(getPersistedFieldsForClass(aClass));
            return fields;
        }
    }

    /**
     * Extracts the value of entity id from the entity.
     * Returns null if there is no @Id on the class or superclasses.
     * Also returns null if the @Id field is not a Long.
     */
    public static Long getEntityId(Object entity, Class aClass) {
        Long id = null;
        if (entity != null) {
            Field field = getEntityIdField(aClass);
            if (field != null && field.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
                try {
                    field.setAccessible(true);
                    id = (Long)field.get(entity);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Reflection cannot access field " + field.getName() +
                                               " on class " + aClass.getCanonicalName());
                }
            }
        }
        return id;
    }

    /**
     * Returns a sorted list of EntityField suitable for generic display of a mercury/athena persisted object.
     * Only the persisted fields will be included.
     */
    public static List<EntityField> formatFields(Object entity, Class entityClass) {
        List<EntityField> list = new ArrayList<>();
        if (mercuryAthenaEntityClassnameToClass.containsValue(entityClass) &&
            OrmUtil.proxySafeIsInstance(entity, entityClass)) {

            for (Field field : getPersistedFields(entityClass)) {
                try {
                    field.setAccessible(true);
                    Type fieldType = field.getGenericType();
                    Object fieldObj = field.get(entity);
                    // When field is a collection, makes classname for the collection content type.
                    ParameterizedType parameterizedType = null;
                    String[] parameterClassnames = null;
                    if (fieldType instanceof ParameterizedType) {
                        parameterizedType = (ParameterizedType)fieldType;
                        parameterClassnames = new String[parameterizedType.getActualTypeArguments().length];
                        for (int i = 0; i < parameterClassnames.length; ++i) {
                            Type type = parameterizedType.getActualTypeArguments()[i];
                            parameterClassnames[i] = type.toString().replaceFirst("^class ", "");
                        }
                    }
                    // Each field is either a collection or a single value.
                    // Collections consist of:
                    //   - Set or List of either entity references or single values
                    //   - Map of (enum or entity or basic type) -> (entity or collection of entity)
                    // Single values are one of these since anything else would not be persisted.
                    //   - mercury/athena entity reference
                    //   - number, date, string, boolean, enum

                    if (fieldObj instanceof Map) {
                        List<EntityField> fieldList = new ArrayList<>();
                        for (Map.Entry item : (Set<Map.Entry>)((Map) fieldObj).entrySet()) {
                            fieldList.add(new EntityField(field.getName(), getNonEntityValue(item.toString()),
                                    null, null));
                        }
                        Collections.sort(fieldList, EntityField.BY_VALUE);
                        list.add(new EntityField(field.getName(), null, null, fieldList));
                    } else if (fieldObj instanceof Iterable) {
                        // Makes an EntityField for each item instance and puts them in fieldList.
                        List<EntityField> fieldList = new ArrayList<>();
                        Class fieldItemClass = getMercuryAthenaEntityClass(parameterClassnames[0]);
                        for (Object fieldListItem : (Iterable)fieldObj) {
                            fieldList.add(makeEntityField(field.getName(), fieldItemClass, fieldListItem));
                        }
                        Collections.sort(fieldList, EntityField.BY_VALUE);
                        list.add(new EntityField(field.getName(), null, null, fieldList));
                    } else {
                        // Object is not a collection, so make either an entity reference, or a plain value.
                        Class fieldClass = getMercuryAthenaEntityClass(
                                fieldType.toString().replaceFirst("^class ", "").replaceFirst("\\<.*", ""));

                        list.add(makeEntityField(field.getName(), fieldClass, fieldObj));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Reflection cannot access field " + field.getName() +
                                               " on class " + entityClass.getCanonicalName());
                }
            }

            // Puts the entityId field first, followed by other fields sorted by name.
            String entityIdFieldName = getEntityIdField(entityClass).getName();
            EntityField entityIdField = null;
            for (EntityField entityField : list) {
                if (entityField.getFieldName().equals(entityIdFieldName)) {
                    entityIdField = entityField;
                    break;
                }
            }
            list.remove(entityIdField);
            Collections.sort(list, EntityField.BY_NAME);
            list.add(0, entityIdField);
        }
        return list;
    }

    private static EntityField makeEntityField(String fieldName, Class fieldClass, Object fieldObj) {
        if (fieldClass != null && fieldObj != null) {
            // If the fieldOjb is a known entity class subtype, use it.
            Class subtypeClass = getMercuryAthenaEntityClass(
                    fieldObj.toString().split("@")[0].replaceFirst("^class ", ""));
            if (subtypeClass != null) {
                fieldClass = subtypeClass;
            }
            fieldObj = OrmUtil.proxySafeCast(fieldObj, fieldClass);
            Long longId = getEntityId(fieldObj, fieldClass);
            String entityId = (longId != null) ? String.valueOf(longId) : NULL_REPRESTATION;
            return new EntityField(fieldName, entityId, fieldClass.getCanonicalName(), null);
        } else {
            return new EntityField(fieldName, getNonEntityValue(fieldObj), null, null);
        }
    }


    /** Returns the entity id field (annotated @Id in entity class) from class and superclasses, or null if none. */
    public static Field getEntityIdField(Class aClass) {
        return getFieldHavingAnnotation(getPersistedFields(aClass), Id.class);
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

    // Gets the value of string, number, date, boolean, or enum.
    private static String getNonEntityValue(Object nonEntityObj) {
        if (nonEntityObj == null) {
            return NULL_REPRESTATION;
        }
        if (nonEntityObj instanceof Date) {
            return DATE_FORMAT.format(nonEntityObj);
        } else if (nonEntityObj instanceof Enum) {
            return ((Enum)nonEntityObj).name();
        } else {
            return nonEntityObj.toString();
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
                classes.addAll(recurseDirectories(directory, packageName, classLoader));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return classes;
    }

    /** Returns all classes in the directory hierarchy. */
    private static List<Class> recurseDirectories(File directory, String packageName, ClassLoader classLoader) {
        List<Class> classes = new ArrayList<>();
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    if (!file.getName().contains(".")) {
                        classes.addAll(recurseDirectories(file, packageName + "." + file.getName(), classLoader));
                    }
                } else if (file.getName().endsWith(".class")) {
                    int idx = file.getName().lastIndexOf(".class");
                    String classname = packageName + '.' + file.getName().substring(0, idx);
                    try {
                        classes.add(classLoader.loadClass(classname));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unknown class '" + classname + "' : " + e);
                    }
                }
            }
        } else {
            throw new RuntimeException("Cannot find directory " + directory.getAbsolutePath() +
                                       ", required for loading classes from " + packageName);
        }
        return classes;
    }

}
