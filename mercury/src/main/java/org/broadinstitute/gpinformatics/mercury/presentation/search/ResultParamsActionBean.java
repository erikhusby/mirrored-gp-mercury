package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.ErrorResolution;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.CustomTraversalEvaluator;
import org.broadinstitute.gpinformatics.infrastructure.search.ResultParamConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.search.ResultParamValues;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.Map;

@UrlBinding("/search/ResultParams.action")
public class ResultParamsActionBean extends CoreActionBean {

    public enum ParamType {
        SEARCH_TERM, CUSTOM_TRAVERSER
    }

    public static final String AJAX_PARAMS_FETCH = "paramsFetch";

    private String paramType;

    private String entityName;

    private String elementName;

    private ResultParamConfiguration resultParamConfig;

    private ResultParamValues resultParamValues;

    /**
     * Returns a dynamically generated result column parameter input UI snippet <br />
     * Required params:  <ul>
     *     <li>entityType (ColumnEntity.entityName: "LabVessel", "LabEvent", etc.)</li>
     *     <li>searchTermName</li></ul>
     * @return
     */
    @HandlesEvent(AJAX_PARAMS_FETCH)
    public Resolution fetchParams() {

        ParamType type = null;
        try {
            type = ParamType.valueOf(paramType);
        } catch (Exception e ) {
            return new ErrorResolution(500, "Unknown type of parameter configuration " + getParamType());
        }

        if( entityName == null || entityName.isEmpty() ) {
            return new ErrorResolution(500, "Entity to get params for was not supplied" );
        }

        if( type == ParamType.SEARCH_TERM ) {
            return getSearchTermParams();
        } else if( type == ParamType.CUSTOM_TRAVERSER ) {
            return getCustomTraverserParams();
        } else {
            return new ErrorResolution(500, "No handler for " + paramType);
        }

    }

    /**
     * Without any user saved settings, need to build out values with any defaults
     */
    private void setDefaultValues() {
        resultParamValues = new ResultParamValues(paramType, entityName, elementName );
        for(Map.Entry<String,ResultParamConfiguration.ParamInput> paramInputKeySet: resultParamConfig.getParamInputs().entrySet() ){
            if( paramInputKeySet.getValue().getDefaultValues() == null ) {
                continue;
            } else {
                for( String value : paramInputKeySet.getValue().getDefaultValues() ){
                    resultParamValues.addParamValue(paramInputKeySet.getKey(), value);
                }
            }
        }
    }

    private Resolution getSearchTermParams(){
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );

        String searchTermName = getElementName();

        SearchTerm searchTerm = configurableSearchDef.getSearchTerm(searchTermName);

        if( searchTerm == null ) {
            return new ErrorResolution(500, "Search term '" + searchTermName + "' is not available in search.");
        }

        if( searchTerm.getResultParamConfigurationExpression() == null ) {
            return new ErrorResolution(500, "Search term " + searchTermName + " has no parameters configured");
        }
        try {
            resultParamConfig = searchTerm.getResultParamConfigurationExpression().evaluate(null,null);
        } catch (Exception e) {
            return new ErrorResolution(500, e.getMessage() );
        }

        return new ForwardResolution("/search/result_params.jsp");
    }

    private Resolution getCustomTraverserParams(){
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );

        String traverserName = getElementName();

        Map<String,CustomTraversalEvaluator> customEvaluators = configurableSearchDef.getCustomTraversalOptions();
        if( customEvaluators == null || !customEvaluators.containsKey(traverserName) ) {
            return new ErrorResolution(500, entityName + " search has no custom traverser named " + traverserName + " configured");
        }

        CustomTraversalEvaluator traversalEvaluator = customEvaluators.get(traverserName);

        resultParamConfig = traversalEvaluator.getResultParamConfiguration();

        return new ForwardResolution("/search/result_params.jsp");
    }

    public String getEntityName() {
        return ( entityName );
    }

    public void setEntityName( String entityName ) {
        this.entityName = entityName;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public ResultParamConfiguration getResultParamConfig() {
        return resultParamConfig;
    }

    public void setResultParams( String resultParamValuesString ) {
        this.resultParamValues = ResultParamValues.fromString(resultParamValuesString );
    }

    public ResultParamValues getResultParamValues() {

        if( resultParamValues == null ) {
            setDefaultValues();
        }
        return resultParamValues;
    }

}
