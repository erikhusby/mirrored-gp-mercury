package org.broadinstitute.gpinformatics.infrastructure.search;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * User selectable result column configurations for when a constrainedResultParamsExpression is attached <br />
 * Used to produce UI configuration via SearchTerm.getResultParamConfigurationExpression. (All input value included) <br />
 * Used to store value for use in SearchTerm.getDisplayValueExpression, input value not included. (ParamInput names tightly coupled to a specific displayValueExpression)
 */
public class ResultParamValues {

    /**
     * Documentation indicates this is thread-safe so hold in static variable
     */
    @JsonIgnore
    private static ObjectMapper JSON_MAPPER = new ObjectMapper();

    private String searchTermName;
    private String userColumnName;
    private Set<Pair<String,String>> paramValues = new HashSet<>();

    public ResultParamValues(){}

    public ResultParamValues(String searchTermName, String userColumnName) {
        this.searchTermName = searchTermName;
        this.userColumnName = userColumnName;
    }

    public boolean addParamValue( String name, String value ) {
        return paramValues.add( Pair.of( name, value ));
    }

    public Set<Pair<String,String>> getParamValues(){
        return ImmutableSet.copyOf(paramValues);
    }

    /**
     * Gets all values for multi picklist and checkbox group
     */
    public Set<String> getMultiValues( String name ){
        Set<String> multiValues = new HashSet<>();
        for(  Pair<String,String> valuePair : paramValues ) {
            if( name.equals(valuePair.getLeft()) ) {
                multiValues.add(valuePair.getRight());
            }
        }
        return multiValues;
    }

    /**
     * Gets single value for text, single picklist, radio, and checkbox
     * TODO JMS Throw illegal state exception if more than one?
     */
    public String getSingleValue( String name ){
        for(  Pair<String,String> valuePair : paramValues ) {
            if( name.equals(valuePair.getLeft()) ) {
                return valuePair.getRight();
            }
        }
        return "";
    }

    public boolean containsValue( String name, String value ){
        for(  Pair<String,String> valuePair : paramValues ) {
            if( name.equals(valuePair.getLeft()) && value.equals( valuePair.getRight() )) {
                return true;
            }
        }
        return false;
    }
    public String getSearchTermName() {
        return searchTermName;
    }

//    public void setSearchTermName(String searchTermName) {
//        this.searchTermName = searchTermName;
//    }

    public String getUserColumnName() {
        return userColumnName;
    }

//    public void setUserColumnName(String userColumnName) {
//        this.userColumnName = userColumnName;
//    }

    /**
     * Pseudo JSON serializer
     * @return
     */
    @Override
    public String toString(){
        try {
            ObjectNode root = JSON_MAPPER.createObjectNode();
            root.put("searchTermName", searchTermName);
            root.put("userColumnName", userColumnName );
            ArrayNode paramValuesNode = JSON_MAPPER.createArrayNode();
            root.put("paramValues", paramValuesNode );
            for(Pair<String,String> paramValue : paramValues ) {
                ObjectNode input = JSON_MAPPER.createObjectNode();
                input.put("name", paramValue.getLeft() );
                input.put("value", paramValue.getRight());
                paramValuesNode.add(input);
            }
            return JSON_MAPPER.writeValueAsString(root);
        } catch (IOException ioe) {
            return ioe.getMessage();
        }
    }
}
