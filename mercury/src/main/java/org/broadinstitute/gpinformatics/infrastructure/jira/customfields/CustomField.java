package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomField {

    @Nonnull
    private final CustomFieldDefinition definition;

    private final Object value;

    public static class ValueContainer {

        public ValueContainer() {
        }

        public ValueContainer(boolean value) {
            this.value = value ? "Yes" : "No";
        }

        public ValueContainer(String value) {
            this.value = value;
        }

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ValueContainer)) {
                return false;
            }

            ValueContainer that = (ValueContainer) o;

            return new EqualsBuilder().append(value, that.value).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(value).toHashCode();
        }
    }

    /**
     * Use this class to create a JSON stream for fields that are keyed off of 'name', such as a user field.
     */
    public static class NameContainer {
        private final String name;

        public NameContainer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class SelectOption extends ValueContainer {
        private String id;

        public SelectOption(String value) {
            if (value.equals("None")) {
                this.id = "-1";
            } else {
                this.setValue(value);
            }
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Should work with current Specs but does not:
     * https://developer.atlassian.com/display/JIRADEV/JIRA+REST+API+Example+-+Create+Issue#JIRARESTAPIExample-CreateIssue-CascadingSelectField
     */
    public static class CascadingSelectList extends ValueContainer {

        private final SelectOption child;

        public CascadingSelectList(String parentValue, String child) {
            this.setValue(parentValue);
            this.child = new SelectOption(child);
        }

        public SelectOption getChild() {
            return child;
        }
    }

    /**
     * Main constructor for the custom field.  Since there are different variations of the custom field, This
     * constructor is pretty generic to allow flexibility
     *
     * @param definition custom field definition returned from Jira.  Main values needed are the field name and the
     *                   Jira recognized field ID
     * @param value      value to be associated with the custom field.
     */
    public CustomField(@Nonnull CustomFieldDefinition definition, Object value) {
        if (definition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value for " + definition.getName() + " cannot be null");
        }

        this.definition = definition;
        this.value = value;
    }

    public interface SubmissionField {
        @Nonnull
        String getName();

        boolean isNullable();
    }

    /**
     * Secondary constructor for the custom field.  Allows just about any value to be set for the field.
     *
     * @param submissionFields A Map containing a reference to the jira definition of the custom field being
     *                         created indexed by the field name.  The main item needed from the field definition is
     *                         the Jira field ID
     * @param field            Represents the name of the field for which this custom field is to be generated.  The
     *                         implementation of SubmissionField is typically an Enum
     * @param value            value to assign to the custom field
     */
    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field, Object value) {
        CustomFieldDefinition definition = submissionFields.get(field.getName());
        if (!field.isNullable() && value==null){
            throw new NullPointerException("value for " + definition.getName() + " cannot be null");
        }
        this.definition = definition;
        this.value = value;
    }


    /**
     * similar to the other constructors, this is a convenience constructor specifically oriented to custom Fields that
     * are for booleans.  The representation in Jira is very specific for booleans so the this constructor will handle
     * the conversion for the user
     *
     * @param submissionFields A Map containing a reference to the jira definition of the custom field being
     *                         created indexed by the field name.  The main item needed from the field definition is
     *                         the Jira field ID
     * @param field            Represents the name of the field for which this custom field is to be generated.  The
     *                         implementation of SubmissionField is typically an Enum
     * @param value            boolean value to associate with the custom field.  This value is converted to
     *                         a {@link ValueContainer} and then that is passed to the main constructor
     */
    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       boolean value) {
        this(submissionFields.get(field.getName()), new ValueContainer(value));
    }

    /** Constructor for a single-select Jira dropdown field. */
    public CustomField(String value, @Nonnull CustomFieldDefinition customFieldDefinition) {
        this(customFieldDefinition, new ValueContainer(value));
    }

    /** Constructor for a multi-select Jira dropdown field. */
    public CustomField(String[] values, @Nonnull CustomFieldDefinition customFieldDefinition) {
        this(customFieldDefinition,
                Stream.of(values).map(value -> new ValueContainer(value)).collect(Collectors.toList()).toArray());
    }

    /**
     * Field Constructor associated with a cascading select field.  The structure of a cascading select is different
     * than a regular field
     *
     * @param submissionFields A Map containing a reference to the jira definition of the custom field being
     *                         created indexed by the field name.  The main item needed from the field definition is
     *                         the Jira field ID
     * @param field            Represents the name of the field for which this custom field is to be generated.  The
     *                         implementation of SubmissionField is typically an Enum
     * @param parentValue      Primary cascading field value
     * @param childValue       secondary cascading field value
     */
    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       String parentValue, String childValue) {
        this(submissionFields.get(field.getName()), new CascadingSelectList(parentValue, childValue));
    }

    @Nonnull
    public CustomFieldDefinition getFieldDefinition() {
        return definition;
    }

    @Nonnull
    public Object getValue() {
        return value;
    }
}
