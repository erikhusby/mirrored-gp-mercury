package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

/** DTO that describes an entity's field for generic display purposes. */
public class EntityField {
    private String fieldName;
    // Value is the number, text, formatted date, or entityId in case this is a reference.
    private String value;
    // If this field is a reference, referenceClassname is non-null classname (the short version).
    private String referenceClassname;
    // Instead of a single value or reference, display a list when valueList is non-null.
    private List<EntityField> valueList;

    public EntityField(@Nonnull String fieldName, String value, String referenceClassname,
                       List<EntityField> valueList) {
        this.fieldName = fieldName;
        this.value = value;
        this.referenceClassname = referenceClassname;
        this.valueList = valueList;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReferenceClassname() {
        return referenceClassname;
    }

    public void setReferenceClassname(String referenceClassname) {
        this.referenceClassname = referenceClassname;
    }

    public List<EntityField> getValueList() {
        return valueList;
    }

    public void setValueList(List<EntityField> valueList) {
        this.valueList = valueList;
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EntityField that = (EntityField) o;

        if (!fieldName.equals(that.fieldName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return fieldName.hashCode();
    }
}
