package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * A web service front-end to Configurable Search.
 */
@Path("/search")
@Stateful
@RequestScoped
public class SearchResource {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    // runSearch(name, params)
    // SearchRequest/Param object?
    // alternative is List<Pair<String,List>>
    // Also need to think about operators?
    // List<String termName, Operator operator, List<String> values>
    // Return ResultList?

    public SearchResponse runSearch(SearchRequest searchRequest) {
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(
                searchRequest.getEntityName());
        SearchInstance searchInstance = null;
        PaginationUtil.Pagination pagination = configurableListFactory.getPagination(searchInstance,
                configurableSearchDef, null, null);
        configurableListFactory.fetchAllPages(pagination, searchInstance, "", "");
        return new SearchResponse(null, null);
    }
}
