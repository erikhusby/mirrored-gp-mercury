package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.ThreadEntityManager;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.ConstrainedValueDao;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * For actions invoked by configurableList.jsp
 */
@SuppressWarnings("UnusedDeclaration")
@UrlBinding("/util/ConfigurableList.action")
public class ConfigurableListActionBean extends CoreActionBean {
    protected static final Log log = LogFactory.getLog(ConfigurableListActionBean.class);
    private static final String SPREADSHEET_FILENAME = "List.xlsx";

    private String downloadColumnSetName;

    private List<String> selectedIds;

    private String entityName;

    private String sessionKey;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ThreadEntityManager threadEntityManager;

    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private SapQuoteLink sapQuoteLink;

    @Inject
    private ConstrainedValueDao constrainedValueDao;

    /**
     * Stream an Excel spreadsheet, from a list of IDs
     *
     * @return streamed Excel spreadsheet
     */
    // TODO move to ConfigurableList ("Viewed Columns" needs access to SearchInstance?)
    public Resolution downloadFromIdList() {
        PaginationUtil.Pagination pagination = (PaginationUtil.Pagination) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.PAGINATION_PREFIX + sessionKey);
        SearchInstance searchInstance = (SearchInstance) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.SEARCH_INSTANCE_PREFIX + sessionKey);

        if( selectedIds == null || selectedIds.isEmpty()) {
            addGlobalValidationError("You must select one or more rows to download.");
            return new ForwardResolution("/error.jsp");
        }

        List<?> entityList;
        try {
            List<?> typeSafeIds = PaginationUtil.convertStringIdsToEntityType(pagination, selectedIds);
            entityList = PaginationUtil.getByIds(threadEntityManager.getEntityManager(), pagination, typeSafeIds);
        } catch (Exception e) {
            log.error("Search failed: ", e);
            getContext().getRequest().setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
            getContext().getValidationErrors().addGlobalError(new SimpleError(
                    "Search encountered an unexpected problem."));
            return new ForwardResolution("/error.jsp");
        }

        if (downloadColumnSetName.equals("Viewed Columns")) {
            List<ColumnTabulation> columnTabulations = searchInstance.buildViewedColumnTabulations(entityName);

            ConfigurableList configurableList = new ConfigurableList(columnTabulations, searchInstance.getViewColumnParamMap(), 0, "ASC",
                    ColumnEntity.getByName(entityName));

            // Add any row listeners
            configurableList.addAddRowsListeners(SearchDefinitionFactory.getForEntity(entityName));

            SearchContext searchContext = buildSearchContext(searchInstance);
            searchContext.setPagination(pagination);
            configurableList.addRows(entityList, searchContext);
            ConfigurableList.ResultList resultList = configurableList.getResultList(false);
            return streamResultList(resultList);
        }

        return createConfigurableDownload(entityList, downloadColumnSetName, getContext(), entityName,
                pagination.getResultEntityId());
    }

    /**
     * For a search that returned multiple pages of results, downloads all pages to a
     * spreadsheet.
     *
     * @return streamed Excel spreadsheet
     */
    public Resolution downloadAllPages() {
        PaginationUtil.Pagination pagination = (PaginationUtil.Pagination) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.PAGINATION_PREFIX + sessionKey);
        SearchInstance searchInstance = (SearchInstance) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.SEARCH_INSTANCE_PREFIX + sessionKey);
        ConfigurableList.ResultList resultList = configurableListFactory.fetchAllPages(pagination, searchInstance,
                downloadColumnSetName, entityName);
        return streamResultList(resultList);
    }

    /**
     *  BSP user lookup required in column eval expression
     *  Use context to avoid need to test in container
     */
    private SearchContext buildSearchContext(SearchInstance searchInstance){
        SearchContext evalContext = new SearchContext();
        evalContext.setBspUserList( bspUserList );
        evalContext.setSearchInstance(searchInstance);
        evalContext.setColumnEntityType(ColumnEntity.getByName(entityName));
        evalContext.setUserBean(userBean);
        evalContext.setJiraConfig(jiraConfig);
        evalContext.setPriceListCache(priceListCache);
        evalContext.setQuoteLink(quoteLink);
        evalContext.setSapQuoteLink(sapQuoteLink);
        evalContext.setOptionValueDao(constrainedValueDao);
        return evalContext;
    }

    /**
     * Creates a spreadsheet and an associated StreamingResolution from a list of entities and a user-selected
     * list of columns.
     *
     * @param entityList            sample list
     * @param downloadColumnSetName name of the column set chosen by the user, with
     *                              preference level prefix
     * @param context               web app context
     * @param entityName            e.g. "Sample"
     * @param entityId              e.g. "sampleId"
     * @return streamed spreadsheet
     */
    public Resolution createConfigurableDownload(List<?> entityList, String downloadColumnSetName,
            CoreActionBeanContext context, String entityName, String entityId) {

        // Get search instance
        SearchInstance searchInstance = (SearchInstance) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.SEARCH_INSTANCE_PREFIX + sessionKey);

        SearchContext searchContext = buildSearchContext(searchInstance);
        searchContext.setResultCellTargetPlatform(SearchContext.ResultCellTargetPlatform.TEXT);

        ConfigurableList configurableListUtils = configurableListFactory.create(entityList, downloadColumnSetName,
                ColumnEntity.getByName(entityName), searchContext,
                SearchDefinitionFactory.getForEntity(entityName));

        Object[][] data = configurableListUtils.getResultList(false).getAsArray();
        return StreamCreatedSpreadsheetUtil.streamSpreadsheet(data, SPREADSHEET_FILENAME);
    }


    /**
     * Convert a resultList to a spreadsheet, and stream it to the browser
     *
     * @param resultList result columns the user requested
     * @return streamed Excel spreadsheet
     */
    private static Resolution streamResultList(ConfigurableList.ResultList resultList) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            SpreadsheetCreator.createSpreadsheet("Sample Info", resultList.getAsArray(), out);
        } catch (IOException ioEx) {
            log.error("Failed to create spreadsheet");
            throw new RuntimeException(ioEx);
        }

        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        stream.setFilename(SPREADSHEET_FILENAME);
        return stream;
    }

    public String getDownloadColumnSetName() {
        return downloadColumnSetName;
    }

    public void setDownloadColumnSetName(String downloadColumnSetName) {
        this.downloadColumnSetName = downloadColumnSetName;
    }

    public List<String> getSelectedIds() {
        return selectedIds;
    }

    public void setSelectedIds(List<String> selectedIds) {
        this.selectedIds = selectedIds;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
}
