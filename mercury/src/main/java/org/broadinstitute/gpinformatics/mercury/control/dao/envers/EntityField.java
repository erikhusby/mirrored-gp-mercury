package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;

/** DTO that describes an entity's field for generic display purposes. */
public class EntityField {
    private String fieldName;
    // Value is the number, text, formatted date, or entityId in case this is a reference.
    private String value;
    // Non-null if this field is a reference.
    private String canonicalClassname;
    // Instead of a single value/reference, display a list when entityFieldList is non-null.
    private List<EntityField> entityFieldList;
    // Instead of a single value/reference, display a list of pairs represented by this Map.
    private SortedMap<String, EntityField> entityFieldMap;

    public EntityField(@Nonnull String fieldName, String value, String canonicalClassname,
                       List<EntityField> entityFieldList, SortedMap<String, EntityField> entityFieldMap) {
        this.fieldName = fieldName;
        this.value = value;
        setCanonicalClassname(canonicalClassname);
        this.entityFieldList = entityFieldList;
        this.entityFieldMap = entityFieldMap;
    }

    public EntityField(@Nonnull String fieldName, SortedMap<String, EntityField> entityFieldMap) {
        this(fieldName, null, null, null, entityFieldMap);
    }

    public EntityField(@Nonnull String fieldName, List<EntityField> entityFieldList) {
        this(fieldName, null, null, entityFieldList, null);
    }

    public EntityField(@Nonnull String fieldName, String value, String canonicalClassname) {
        this(fieldName, value, canonicalClassname, null, null);
    }

    public EntityField(@Nonnull String fieldName, String value) {
        this(fieldName, value, null, null, null);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(@Nonnull String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCanonicalClassname() {
        return canonicalClassname;
    }

    /**
     * Sets canonicalClassname but not if entityId (=value field) is null or non-numeric.
     * This prevents audit trail jsp from making a link to the entity that cannot be displayed.
     */
    public void setCanonicalClassname(String canonicalClassname) {
        this.canonicalClassname = (value != null && StringUtils.isNumeric(value)) ? canonicalClassname : null;
    }

    public List<EntityField> getEntityFieldList() {
        return entityFieldList;
    }

    public void setEntityFieldList(List<EntityField> entityFieldList) {
        this.entityFieldList = entityFieldList;
    }

    public SortedMap<String, EntityField> getEntityFieldMap() {
        return entityFieldMap;
    }

    public void setEntityFieldMap(SortedMap<String, EntityField> entityFieldMap) {
        this.entityFieldMap = entityFieldMap;
    }

    public static Comparator<EntityField> BY_NAME = new Comparator<EntityField>() {
        @Override
        public int compare(EntityField o1, EntityField o2) {
            return o1.getFieldName().compareTo(o2.getFieldName());
        }
    };

    public static Comparator<EntityField> BY_VALUE = new Comparator<EntityField>() {
        @Override
        public int compare(EntityField o1, EntityField o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityField)) {
            return false;
        }

        EntityField that = (EntityField) o;

        if (!fieldName.equals(that.fieldName)) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = fieldName.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
