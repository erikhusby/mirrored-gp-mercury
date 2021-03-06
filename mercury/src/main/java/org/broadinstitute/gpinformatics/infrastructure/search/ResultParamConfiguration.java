package org.broadinstitute.gpinformatics.infrastructure.search;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User selectable result column configurations for when a constrainedResultParamsExpression is attached <br />
 * Used to produce UI configuration via SearchTerm.getResultParamConfigurationExpression. (All input value included) <br />
 * Used to store value for use in SearchTerm.getDisplayValueExpression, input value not included. (ParamInput names tightly coupled to a specific displayValueExpression)
 */
public class ResultParamConfiguration {
    private Map<String,ParamInput> paramInputs = new LinkedHashMap<>();

    /**
     * A JavaScript block, last statement of which must return true or false/undefined
     * TODO JMS Is not implemented. Possibly do it at form level for interdependent fields and at field level also?
     */
    private String formValidationScript = "{ true; }";

    public ResultParamConfiguration() {}

    public enum InputType {
        TEXT,
        CHECKBOX,
        CHECKBOX_GROUP,
        RADIO,
        PICKLIST,
        MULTI_PICKLIST
    }

    // TODO:  JMS Implement editing by merging user selected values

    /**
     * Represents what's needed to construct inputs for a single result param. <br />
     * Names must be unique for a list of params, only alphanumeric and underscore characters allowed <br />
     * HTML element ID will be same as name for text, checkbox, picklist, and multi picklist types <br />
     * For radio and checkbox group, all options will have same name and unique HTML IDs generated by concatenating param name, underscore, and ConstrainedValue code.
     */
    public static class ParamInput {
        private String name;
        private InputType type;
        private String label;
        private List<ConstrainedValue> optionItems;
        private Set<String> defaultValues;

        public ParamInput(String name, InputType type, String label){
            this.name = name;
            this.type = type;
            this.label = label;
        }

        public String getName() {
            return name;
        }

        public InputType getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public List<ConstrainedValue> getOptionItems() {
            return optionItems;
        }

        public void setOptionItems(List<ConstrainedValue> optionItems) {
            this.optionItems = optionItems;
        }

        public Set<String> getDefaultValues() {
            return defaultValues;
        }

        public void addDefaultValue(String defaultValue) {
            if(this.defaultValues == null ) {
                this.defaultValues = new HashSet<>();
            }
            this.defaultValues.add(defaultValue);
        }

        public String getDefaultSingleValue() {
            return this.defaultValues == null || this.defaultValues.size() == 0?null:this.defaultValues.iterator().next();
        }

    }

    public Map<String,ParamInput> getParamInputs() {
        if( paramInputs == null ) {
            return Collections.emptyMap();
        }
        return paramInputs;
    }

    public ParamInput getParamInput( String name ) {
        return paramInputs.get(name);
    }

    /**
     * Add param input - name MUST be unique!
     */
    public void addParamInput(ParamInput paramInput) {
        if( paramInputs.get(paramInput.getName()) != null ) {
            throw new IllegalArgumentException( "Duplicate input element name (" + paramInput.getName() + ") not allowed");
        }
        this.paramInputs.put(paramInput.getName(), paramInput);
    }

    /**
     * For client side, not implemented in UI
     */
    public String getFormValidationScript() {
        return formValidationScript;
    }

    /**
     * For client side, not implemented in UI
     */
    public void setFormValidationScript(String formValidationScript) {
        this.formValidationScript = formValidationScript;
    }
}
