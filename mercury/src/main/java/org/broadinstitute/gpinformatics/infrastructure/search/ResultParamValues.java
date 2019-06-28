package org.broadinstitute.gpinformatics.infrastructure.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User selectable result column configurations for when a constrainedResultParamsExpression is attached <br />
 * Used to produce UI configuration via SearchTerm.getResultParamConfigurationExpression. (All input value included) <br />
 * Used to store value for use in SearchTerm.getDisplayValueExpression, input value not included. (ParamInput names tightly coupled to a specific displayValueExpression)
 */
public class ResultParamValues {

    private static final Log LOG = LogFactory.getLog(ResultParamValues.class);

    /**
     * Documentation indicates this is thread-safe so hold in static variable
     */
    @JsonIgnore
    private static ObjectMapper JSON_MAPPER = new ObjectMapper();

    private String paramType;
    private String entityName;
    private String elementName;

    private List<ParamValue> paramValues = new ArrayList<>();

    // JSON requires
    public ResultParamValues(){}

    public ResultParamValues(String paramType, String entityName, String elementName ) {
        this.paramType = paramType;
        this.entityName = entityName;
        this.elementName = elementName;
    }

    public String getParamType() {
        return paramType;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getElementName() {
        return elementName;
    }

    public boolean addParamValue( String name, String value ) {
        return paramValues.add( new ParamValue( name, value ));
    }

    public Set<ParamValue> getParamValues(){
        return ImmutableSet.copyOf(paramValues);
    }

    /**
     * Case only valid for when values apply to a custom search term
     */
    public String getUserColumnName(){
        return getSingleValue( "userColumnName" );
    }

    /**
     * Gets all values for multi picklist and checkbox group
     */
    public Set<String> getMultiValues( String name ){
        Set<String> multiValues = new HashSet<>();
        for(  ParamValue paramValue : paramValues ) {
            if( name.equals(paramValue.getName()) ) {
                multiValues.add(paramValue.getValue());
            }
        }
        return multiValues;
    }

    /**
     * Gets single value for text, single picklist, radio, and checkbox
     * TODO JMS Throw illegal state exception if more than one?
     */
    public String getSingleValue( String name ){
        for(  ParamValue paramValue : paramValues ) {
            if( name.equals(paramValue.getName()) ) {
                return paramValue.getValue();
            }
        }
        return "";
    }

    public boolean containsValue( String name, String value ){
        for(  ParamValue paramValue : paramValues ) {
            if( name.equals(paramValue.getName() ) && value.equals( paramValue.getValue() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * JSON serializer
     */
    @Override
    public String toString(){
        try {
            ObjectNode root = JSON_MAPPER.createObjectNode();
            root.put("paramType", paramType);
            root.put("entityName", entityName);
            root.put("elementName", elementName);
            ArrayNode paramValuesNode = JSON_MAPPER.createArrayNode();
            root.put("paramValues", paramValuesNode );
            for(ParamValue paramValue : paramValues ) {
                ObjectNode input = JSON_MAPPER.createObjectNode();
                input.put("name", paramValue.getName() );
                input.put("value", paramValue.getValue());
                paramValuesNode.add(input);
            }
            return JSON_MAPPER.writeValueAsString(root);
        } catch (IOException ioe) {
            LOG.error("Fail marshalling result params", ioe);
            return "Fail marshalling result params: " + ioe.getMessage();
        }
    }

    /**
     * JSON Deserializer with the option to simply return null if just a search term or traverser name
     */
    public static ResultParamValues fromString( String json ){
        // Quick JSON test, although index of { should always be 0
        if (json.indexOf("{") < 0) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, ResultParamValues.class);
        } catch (IOException ioe) {
            LOG.error("Fail unmarshalling result params", ioe);
            return null;
        }
    }

    public static class ParamValue {

        // JSON requires
        public ParamValue(){}

        public ParamValue( String name, String value){
            this.name = name;
            this.value = value;
        }

        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
