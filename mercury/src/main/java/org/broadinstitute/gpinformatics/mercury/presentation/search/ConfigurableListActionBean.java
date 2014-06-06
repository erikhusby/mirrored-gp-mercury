package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationDao;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * For actions invoked by configurableList.jsp
 */
@SuppressWarnings("UnusedDeclaration")
@UrlBinding("/util/ConfigurableList.action")
public class ConfigurableListActionBean extends CoreActionBean {
    protected static final Log log = LogFactory.getLog(ConfigurableListActionBean.class);
    private static final String SPREADSHEET_FILENAME = "List.xls";

    private String downloadColumnSetName;

    private List<String> selectedIds;

    private String entityName;

    private String sessionKey;

    @Inject
    private PaginationDao paginationDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    /**
     * Stream an Excel spreadsheet, from a list of IDs
     *
     * @return streamed Excel spreadsheet
     */
    // TODO move to ConfigurableList ("Viewed Columns" needs access to SearchInstance?)
    public Resolution downloadFromIdList() {
        PaginationDao.Pagination pagination = (PaginationDao.Pagination) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.PAGINATION_PREFIX + sessionKey);

        List<?> entityList;
        try {
            // TODO jmt handle Longs
/*
            Barcoded barcoded = (Barcoded) Class.forName(pagination.getResultEntity()).newInstance();
            switch (barcoded.getBarcodeDataType()) {
            case NUMERIC:
                List<Long> ids = new ArrayList<>(selectedIds.size());
                for (String id : selectedIds) {
                    ids.add(Long.parseLong(id));
                }
                entityList = paginationDao.getByIds(pagination, ids);
                break;
            case ALPHANUMERIC:
*/
                entityList = paginationDao.getByIds(pagination, selectedIds);
/*
                break;
            }
*/
        } catch (Exception e) {
            log.error("Search failed: ", e);
            getContext().getValidationErrors().addGlobalError(new SimpleError(
                    "Search encountered an unexpected problem. Please email bsp-support."));
            return new ForwardResolution("/error.jsp");
        }

        if (downloadColumnSetName.equals("Viewed Columns")) {
            List<ColumnTabulation> columnTabulations = buildViewedColumnTabulations();

            ConfigurableList configurableList = new ConfigurableList(columnTabulations, 0, "ASC",
                    ColumnEntity.getByName(entityName));
            configurableList.addListener(new BspSampleSearchAddRowsListener(bspSampleSearchService));
            configurableList.addRows(entityList);
            ConfigurableList.ResultList resultList = configurableList.getResultList();
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
    // TODO move to PaginationDao or ConfigurableList?
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Resolution downloadAllPages() {
        // Determine which columns the user wants to download
        List<ColumnTabulation> columnTabulations;
        if (downloadColumnSetName.equals("Viewed Columns")) {
            columnTabulations = buildViewedColumnTabulations();
        } else {
            columnTabulations = configurableListFactory.buildColumnSetTabulations(downloadColumnSetName, entityName);
        }
        PaginationDao.Pagination pagination = (PaginationDao.Pagination) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.PAGINATION_PREFIX + sessionKey);
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 0, "ASC",
                ColumnEntity.getByName(entityName));
        configurableList.addListener(new BspSampleSearchAddRowsListener(bspSampleSearchService));

        // Get each page and add it to the configurable list
        for (int i = 0; i < pagination.getNumberPages(); i++) {
            List resultsPage = paginationDao.getPage(pagination, i);
            configurableList.addRows(resultsPage);
            paginationDao.clear();
        }
        return streamResultList(configurableList.getResultList());
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
        ConfigurableList configurableListUtils = configurableListFactory.create(entityList, downloadColumnSetName,
                entityName, ColumnEntity.getByName(entityName));

        Object[][] data = configurableListUtils.getResultList().getAsArray();
        return StreamCreatedSpreadsheetUtil.streamSpreadsheet(data, SPREADSHEET_FILENAME);
    }

    /**
     * For result columns, a configurable search can combine column sets, individual
     * columns, and search terms. This method builds a list of columns that match what the
     * user is viewing, to allow the same columns to be downloaded.
     *
     * @return list of columns
     */
    // TODO move to SearchInstance?
    private List<ColumnTabulation> buildViewedColumnTabulations() {
        // Get search instance
        SearchInstance searchInstance = (SearchInstance) getContext().getRequest().getSession()
                .getAttribute(ConfigurableSearchActionBean.SEARCH_INSTANCE_PREFIX + sessionKey);

        List<String> columnNameList;
        if (searchInstance.getPredefinedViewColumns() != null && !searchInstance.getPredefinedViewColumns().isEmpty()) {
            columnNameList = searchInstance.getPredefinedViewColumns();
        } else {
            columnNameList = searchInstance.getColumnSetColumnNameList();
        }
        ConfigurableSearchDefinition configurableSearchDef = new SearchDefinitionFactory().getForEntity(entityName);
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnNameList) {
            columnTabulations.add(configurableSearchDef.getSearchTerm(columnName));
        }
        columnTabulations.addAll(searchInstance.findTopLevelColumnTabulations());
        return columnTabulations;
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
