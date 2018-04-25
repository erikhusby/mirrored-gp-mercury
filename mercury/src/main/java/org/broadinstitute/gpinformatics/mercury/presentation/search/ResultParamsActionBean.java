package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.ErrorResolution;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.ResultParamConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

@UrlBinding("/search/ResultParams.action")
public class ResultParamsActionBean extends CoreActionBean {

    public static final String AJAX_PARAMS_FETCH = "paramsFetch";

    private ColumnEntity entityType;

    /**
     * The definition from which to get the search term's result parameter configuration
     */
    private ConfigurableSearchDefinition configurableSearchDef;

    private String searchTermName;

    private ResultParamConfiguration resultParams;

    /**
     * Returns a dynamically generated result column parameter input UI snippet <br />
     * Required params:  <ul>
     *     <li>entityType (ColumnEntity.entityName: "LabVessel", "LabEvent", etc.)</li>
     *     <li>searchTermName</li></ul>
     * @return
     */
    @HandlesEvent(AJAX_PARAMS_FETCH)
    public Resolution fetchParams() {
        configurableSearchDef = SearchDefinitionFactory.getForEntity( getEntityName() );

        SearchTerm searchTerm = configurableSearchDef.getSearchTerm(getSearchTermName());

        if( searchTerm == null ) {
            return new ErrorResolution(500, "No search term named " + getSearchTermName());
        }
        if( searchTerm.getResultParamConfigurationExpression() == null ) {
            return new ErrorResolution(500, "Search term " + getSearchTermName() + " has no parameters configured");
        }
        try {
            resultParams = searchTerm.getResultParamConfigurationExpression().evaluate(null,null);
        } catch (Exception e) {
            return new ErrorResolution(500, e.getMessage() );
        }

        return new ForwardResolution("/search/result_params.jsp");
    }

    public String getEntityName() {
        return ( entityType == null ? null : entityType.getEntityName() );
    }

    public void setEntityName( String entityName ) {
        entityType = null;
        for( ColumnEntity type : entityType.values() ) {
            if( type.getEntityName().equals(entityName) ) entityType = type;
        }
    }

    public String getSearchTermName() {
        return searchTermName;
    }

    public void setSearchTermName(String searchTermName) {
        this.searchTermName = searchTermName;
    }

    public ResultParamConfiguration getResultParams() {
        return resultParams;
    }

}
