package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.persistence.Id;
import javax.persistence.metamodel.SetAttribute;
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
    public static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    public static final String NULL_REPRESTATION = "null";
    private static final List<Class> mercuryAthenaClasses = new ArrayList<>();
    private static final Map<String, Class> mercuryAthenaEntityClassnameToClass = new HashMap<>();
    private static final Map<Class, List<Field>> mapClassToPersistedFields = new HashMap<>();
    private static final Map<String, Class> generatedEntityClassnameToClass = new HashMap<>();

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
                if (CollectionUtils.isNotEmpty(getFieldsOfType(aClass, SingularAttribute.class)) ||
                    CollectionUtils.isNotEmpty(getFieldsOfType(aClass, SetAttribute.class))) {
                    int idx = aClass.getCanonicalName().lastIndexOf("_");
                    if (idx < aClass.getCanonicalName().length() - 1) {
                        throw new RuntimeException("Unexpected generated class name: " + aClass.getCanonicalName());
                    }
                    generatedEntityClassnameToClass.put(aClass.getCanonicalName(), aClass);
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

    /** Returns the JPA generated entity classes. */
    public static Map<String, Class> getGeneratedEntityClassnameToClass() {
        return generatedEntityClassnameToClass;
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
    public static List<Field> getPersistedFieldsForClass(Class persistedClass) {
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
            Class generatedClass = generatedEntityClassnameToClass.get(persistedClass.getCanonicalName() + "_");
            if (generatedClass == null) {
                throw new RuntimeException("Cannot find jpa generated class '" +
                                           persistedClass.getCanonicalName() + "_'");
            }
            for (Field jpaField : generatedClass.getDeclaredFields()) {
                try {
                    persistedFields.add(persistedClass.getDeclaredField(jpaField.getName()));
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Missing field " + jpaField.getName() +
                                               " on class " + persistedClass.getSimpleName(), e);
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
        if (!OrmUtil.proxySafeIsInstance(entity, entityClass)) {
            throw new RuntimeException("Cannot cast object as " + entityClass.getCanonicalName());
        }

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
                    // For now just make a list of each Map.Entry as a plain string.
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
                        fieldList.addAll(makeEntityField(field.getName(), fieldItemClass, fieldListItem));
                    }
                    Collections.sort(fieldList, EntityField.BY_VALUE);
                    list.add(new EntityField(field.getName(), null, null, fieldList));
                } else {
                    // Object is not a collection, so make either an entity reference, or a plain value.
                    String classname = fieldType.toString().replaceFirst("^class ", "");
                    if (classname.contains("<")) {
                        classname = classname.substring(0, classname.indexOf('<'));
                    }
                    Class fieldClass = getMercuryAthenaEntityClass(classname);
                    list.addAll(makeEntityField(field.getName(), fieldClass, fieldObj));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Reflection cannot access field " + field.getName() +
                                           " on class " + entityClass.getCanonicalName());
            }
        }

        // Puts the entityId field first, followed by other fields sorted by name.
        // Also makes the entityId be an entity reference, not just a Long.
        EntityField entityIdEntityField = null;
        Field entityIdField = getEntityIdField(entityClass);
        if (entityIdField != null) {
            for (EntityField entityField : list) {
                if (entityField.getFieldName().equals(entityIdField.getName())) {
                    entityIdEntityField = entityField;
                    entityField.setCanonicalClassname(entityClass.getCanonicalName());
                    break;
                }
            }
        }
        list.remove(entityIdEntityField);
        Collections.sort(list, EntityField.BY_NAME);
        if (entityIdEntityField != null) {
            list.add(0, entityIdEntityField);
        }
        return list;
    }

    private static List<EntityField> makeEntityField(String fieldName, Class fieldClass, Object fieldObj) {
        if (fieldClass != null && fieldObj != null) {
            // Special case the embedded VesselContainer.  Put its persistent fields in the returned list.
            if (fieldClass.getCanonicalName().equals(VesselContainer.class.getCanonicalName())) {
                return formatFields(fieldObj, fieldClass);
            } else {
                // If the fieldObj is a known entity class subtype, use it.  The subtype classname is
                // gotten from the object's toString(), before the '@'.
                Class subtypeClass = getMercuryAthenaEntityClass(
                        fieldObj.toString().split("@")[0].replaceFirst("^class ", ""));
                if (subtypeClass != null) {
                    fieldClass = subtypeClass;
                }
                fieldObj = OrmUtil.proxySafeCast(fieldObj, fieldClass);
                Long longId = getEntityId(fieldObj, fieldClass);
                String entityId = (longId != null) ? String.valueOf(longId) : NULL_REPRESTATION;
                return Collections.singletonList(
                        new EntityField(fieldName, entityId, fieldClass.getCanonicalName(), null));
            }
        } else {
            return Collections.singletonList(new EntityField(fieldName, getNonEntityValue(fieldObj), null, null));
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
