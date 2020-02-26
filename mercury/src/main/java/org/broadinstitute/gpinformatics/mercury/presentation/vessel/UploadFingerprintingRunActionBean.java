package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.LabVesselFingerprintingMetricPlugin;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FluidigmRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.storage.PickerActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@UrlBinding(value = "/view/uploadFingerprintingRun.action")
public class UploadFingerprintingRunActionBean extends CoreActionBean {

    public static final String ENTITY_NAME = "labMetric";

    public static final String UPLOAD_ACTION = "upload";

    public static final String FINGERPRINTING_RUN_UPLOAD_PAGE = "/vessel/fingerprinting_run_upload.jsp";

    @Inject
    private FluidigmRunFactory fluidigmRunFactory;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private JiraConfig jiraConfig;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private LabMetricRunDao labMetricRunDao;

    @Validate(required = true, on = UPLOAD_ACTION)
    private FileBean runFile;

    private Long labMetricRunId;

    private LabMetricRun labMetricRun;

    private ConfigurableList.ResultList resultList;

    private PickerActionBean.SearchType searchType;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labMetricRunId != null) {
            labMetricRun = labMetricRunDao.findById(LabMetricRun.class, labMetricRunId);
            buildColumns();
        }
        return new ForwardResolution(FINGERPRINTING_RUN_UPLOAD_PAGE);
    }

    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        MessageCollection messageCollection = new MessageCollection();
        InputStream fileStream = null;
        try {
            fileStream = runFile.getInputStream();
            Pair<StaticPlate, LabMetricRun> pair = fluidigmRunFactory.createFluidigmChipRunAndDequeue(
                    runFile.getInputStream(), userBean.getBspUser().getUserId(), messageCollection);

            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
            } else {
                labMetricRun = pair.getRight();
                messageCollection.addInfo("Successfully uploaded run " + pair.getRight().getRunName());
            }
        } catch (IOException io) {
            messageCollection.addError("IO exception while parsing upload."  + io.getMessage());
        } finally {
            IOUtils.closeQuietly(fileStream);
            try {
                runFile.delete();
            } catch (IOException ignored) {
                // If cannot delete, oh well.
            }
        }

        buildColumns();
        return new ForwardResolution(FINGERPRINTING_RUN_UPLOAD_PAGE);
    }

    public void buildColumns() {
        if (labMetricRun == null) {
            return;
        }

        SearchContext searchContext = new SearchContext();
        searchContext.setBspUserList(bspUserList);
        searchContext.setUserBean(userBean);
        searchContext.setJiraConfig(jiraConfig);
        searchContext.setPriceListCache(priceListCache);
        searchContext.setQuoteLink(quoteLink);
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Fluidigm Metrics");
        searchTerm.setPluginClass(LabVesselFingerprintingMetricPlugin.class);

        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(searchTerm);

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.emptyMap(), 0, "ASC",
                ColumnEntity.LAB_VESSEL);

        List<LabVessel> labVessels = labMetricRun.getLabMetrics().stream()
                .map(LabMetric::getLabVessel)
                .distinct()
                .collect(Collectors.toList());

        configurableList.addRows(labVessels, searchContext);

        resultList = configurableList.getResultList();
    }

    public void setRunFile(FileBean runFile) {
        this.runFile = runFile;
    }

    public ConfigurableList.ResultList getResultList() {
        return resultList;
    }

    public void setResultList(ConfigurableList.ResultList resultList) {
        this.resultList = resultList;
    }

    public PickerActionBean.SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(
            PickerActionBean.SearchType searchType) {
        this.searchType = searchType;
    }

    public LabMetricRun getLabMetricRun() {
        return labMetricRun;
    }

    public void setLabMetricRun(LabMetricRun labMetricRun) {
        this.labMetricRun = labMetricRun;
    }

    public Long getLabMetricRunId() {
        return labMetricRunId;
    }

    public void setLabMetricRunId(Long labMetricRunId) {
        this.labMetricRunId = labMetricRunId;
    }

    public String getEntityName() {
        return ENTITY_NAME;
    }

    public String getSessionKey() {
        return null;
    }

    public String getColumnSetName() {
        return null;
    }

    public String getDownloadColumnSets() {
        return null;
    }
}
