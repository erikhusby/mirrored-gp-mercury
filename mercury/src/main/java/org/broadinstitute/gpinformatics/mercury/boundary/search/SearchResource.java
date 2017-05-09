package org.broadinstitute.gpinformatics.mercury.boundary.search;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A web service front-end to Configurable Search.
 */
@Path("/search")
@Stateful
@RequestScoped
public class SearchResource {

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @POST
    @Path("/run")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public SearchResponseBean runSearch(SearchRequestBean searchRequestBean) {
        try {
            ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(
                    searchRequestBean.getEntityName());

            // Get the search instance from a preference
            ColumnEntity entityType = ColumnEntity.getByName(searchRequestBean.getEntityName());
            PreferenceType preferenceType = entityType.getSearchInstancePrefs()[0];
            Preference preference =  preferenceDao.getGlobalPreference(preferenceType);
            Map<PreferenceType, Preference> preferenceMap = new HashMap<>();
            preferenceMap.put(preferenceType, preference);
            SearchInstance searchInstance = SearchInstance.findSearchInstance(preferenceType,
                    searchRequestBean.getSearchName(), configurableSearchDef, preferenceMap);

            // Set values in the search instance
            for (SearchValueBean requestSearchValueBean : searchRequestBean.getSearchValueBeanList()) {
                for (SearchInstance.SearchValue instanceSearchValue : searchInstance.getSearchValues()) {
                    if (instanceSearchValue.getTermName().equals(requestSearchValueBean.getTermName())) {
                        instanceSearchValue.setValues(requestSearchValueBean.getValues());
                    }
                }
            }

            // Get results from the database
            PaginationUtil.Pagination pagination = configurableListFactory.getPagination(searchInstance,
                    configurableSearchDef, null, null);
            ConfigurableList.ResultList resultList = configurableListFactory.fetchAllPages(pagination, searchInstance,
                    "Viewed Columns", searchRequestBean.getEntityName());

            // Load headers into DTO
            List<String> headers = new ArrayList<>();
            for (ConfigurableList.Header header : resultList.getHeaders()) {
                headers.add(header.getViewHeader());
            }

            // Load rows and cells into DTO
            List<ConfigurableList.ResultRow> resultRows = resultList.getResultRows();
            List<SearchRowBean> searchRowBeans = new ArrayList<>(resultRows.size());
            for (ConfigurableList.ResultRow resultRow : resultRows) {
                List<Comparable<?>> sortableCells = resultRow.getSortableCells();
                List<String> fields = new ArrayList<>(sortableCells.size());
                for (Comparable<?> comparable : sortableCells) {
                    fields.add(comparable == null ? null : comparable.toString());
                }
                SearchRowBean searchRowBean = new SearchRowBean(fields, null);
                searchRowBeans.add(searchRowBean);
            }

            return new SearchResponseBean(headers, searchRowBeans);
        } catch (Exception e) {
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }
}
