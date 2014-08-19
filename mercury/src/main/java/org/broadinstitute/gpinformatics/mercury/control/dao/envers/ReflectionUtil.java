package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import oracle.net.aso.s;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

import javax.persistence.Embeddable;
import javax.persistence.Id;
import javax.persistence.metamodel.StaticMetamodel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

public class ReflectionUtil {
    private static final Logger logger = Logger.getLogger(ReflectionUtil.class.getName());
    public static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    public static final String NULL_REPRESTATION = "null";
    private static final Map<String, Class> mercuryAthenaEntityClassnameToClass = new HashMap<>();
    private static final Map<Class, List<Field>> mapClassToPersistedFields = new HashMap<>();
    private static final Map<String, Class> entityClassnameToGeneratedClass = new HashMap<>();
    private static final List<Class> embeddableEntities = new ArrayList<>();

    private static final String[] auditedEntityPackages = new String[] {
            "org.broadinstitute.gpinformatics.athena.entity",
            "org.broadinstitute.gpinformatics.mercury.entity"
    };

    static {
        // Finds all the classes that will have Mercury audit data, and their associated JPA generated classes.
        if (mercuryAthenaEntityClassnameToClass.size() == 0) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                URL resource = ClasspathUrlFinder.findClassBase(LabEvent.class);
                AnnotationDB db = new AnnotationDB();
                db.setScanClassAnnotations(true);
                db.setScanFieldAnnotations(false);
                db.setScanMethodAnnotations(false);
                db.setScanParameterAnnotations(false);
                db.scanArchives(resource);

                // The JPA generated classes have a StaticMetamodel annotation that contains the
                // persisted entity classname.
                for (String classname : db.getAnnotationIndex().get(StaticMetamodel.class.getName())) {
                    // Restricts search to athena and mercury entity packages since audit
                    // data is only obtained from the Mercury/Athena database.
                    if (isInAuditedEntityPackage(classname)) {
                        System.out.println(classname);
                        Class aClass = classLoader.loadClass(classname);
                        StaticMetamodel  annotation  = (StaticMetamodel) aClass.getAnnotation(StaticMetamodel.class);
                        if (annotation != null) {
                            Class entityClass = annotation.value();
                            mercuryAthenaEntityClassnameToClass.put(entityClass.getCanonicalName(), entityClass);
                            entityClassnameToGeneratedClass.put(entityClass.getCanonicalName(), aClass);

                            // Also keeps track of the @Embeddable entities.
                            if (entityClass.getAnnotation(Embeddable.class) != null) {
                                embeddableEntities.add(entityClass);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to load entity classes: " + e);
            }
        }
        logger.info("Found " + mercuryAthenaEntityClassnameToClass.size() + " audited entity classes.");
    }

    private static boolean isInAuditedEntityPackage(String classname) {
        for (String pkg : auditedEntityPackages) {
            if (classname.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /** Returns all entity classes found in mercury.entity and athena.entity packages. */
    public static Collection<Class> getMercuryAthenaEntityClasses() {
        return mercuryAthenaEntityClassnameToClass.values();
    }

    /** Returns the mercury/athena entity canonical classnames. */
    public static Collection<String> getMercuryAthenaEntityClassnames() {
        return mercuryAthenaEntityClassnameToClass.keySet();
    }

    /** Returns the entity class by canonical classname. */
    public static Class getMercuryAthenaEntityClass(String canonicalClassname) {
        return mercuryAthenaEntityClassnameToClass.get(canonicalClassname);
    }

    public static List<Class> getEmbeddableEntities() {
        return embeddableEntities;
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
            // Goes to the corresponding generated jpa model class and collects the field names,
            // then finds the fields on the persistedClass that have the same name.
            Class generatedClass = entityClassnameToGeneratedClass.get(persistedClass.getCanonicalName());
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
                    SortedMap<String, EntityField> fieldMap = new TreeMap<>();
                    Class mapKeyClass = getMercuryAthenaEntityClass(parameterClassnames[1]);
                    for (Map.Entry item : (Set<Map.Entry>)((Map) fieldObj).entrySet()) {
                        fieldMap.put(getNonEntityValue(item.getKey()),
                                makeEntityField(field.getName(), mapKeyClass, item.getValue()).get(0));
                    }
                    list.add(new EntityField(field.getName(), fieldMap));
                } else if (fieldObj instanceof Iterable) {
                    // Makes an EntityField for each item instance and puts them in fieldList.
                    List<EntityField> fieldList = new ArrayList<>();
                    Class fieldItemClass = getMercuryAthenaEntityClass(parameterClassnames[0]);
                    for (Object fieldListItem : (Iterable)fieldObj) {
                        fieldList.addAll(makeEntityField(field.getName(), fieldItemClass, fieldListItem));
                    }
                    Collections.sort(fieldList, EntityField.BY_VALUE);
                    list.add(new EntityField(field.getName(), fieldList));
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
            // For the embedded entities, instead of linking to them, add their persistent fields into
            // the returned list so they appear as fields on the entity.
            if (embeddableEntities.contains(fieldClass)) {
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
                        new EntityField(fieldName, entityId, fieldClass.getCanonicalName()));
            }
        } else {
            return Collections.singletonList(new EntityField(fieldName, getNonEntityValue(fieldObj)));
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

}
