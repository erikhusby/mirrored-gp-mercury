package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

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
}
