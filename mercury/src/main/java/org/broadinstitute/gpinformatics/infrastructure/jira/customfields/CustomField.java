package org.broadinstitute.gpinformatics.infrastructure.jira.customfields;

import javax.annotation.Nonnull;
import java.util.Map;

public class CustomField {

    @Nonnull
    private final CustomFieldDefinition definition;

    @Nonnull
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
    }

    public static class SelectOption extends ValueContainer {
        private String id;

        public SelectOption(String value) {
            if(value.equals("None")) {
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
    public static class CascadingSelectList extends ValueContainer{

        private final SelectOption child;

        public CascadingSelectList(String parentValue, String child) {
            this.setValue(parentValue);
            this.child = new SelectOption(child);
        }

        public SelectOption getChild() {
            return child;
        }
    }

    public CustomField(@Nonnull CustomFieldDefinition definition, @Nonnull Object value) {
        if (definition == null) {
            throw new NullPointerException("fieldDefinition cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }

        this.definition = definition;
        this.value = value;
    }

    public interface SubmissionField {
        @Nonnull String getFieldName();
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       @Nonnull Object value) {
        this(submissionFields.get(field.getFieldName()), value);
    }

    public CustomField(@Nonnull Map<String, CustomFieldDefinition> submissionFields,
                       @Nonnull SubmissionField field,
                       boolean value) {
        this(submissionFields.get(field.getFieldName()), new ValueContainer(value));
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
