package org.broadinstitute.gpinformatics.mercury.presentation.storage;


import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.PickerVesselPlugin;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.NotInStorageException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This Action Bean takes Lab Batch or Lab Vessel Barcodes and generates XL20 pick lists by searching for the
 * tubes in the storage system.
 */
@UrlBinding(value = PickerActionBean.ACTION_BEAN_URL)
public class PickerActionBean extends CoreActionBean {

    public static final String ENTITY_NAME = "labVessel";

    public enum SearchType {
        LAB_BATCH("Lab Batch"),
        LAB_VESSEL_BARCODE("Lab Vessel Barcodes");
        private String displayName;

        SearchType(String displayName) {

            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Logger logger = Logger.getLogger(PickerActionBean.class.getName());

    public static final String ACTION_BEAN_URL = "/storage/picker.action";
    private static final String VIEW_PAGE = "/storage/create_picker_csv.jsp";
    private static final String SEARCH_ACTION = "search";

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;
    @Inject
    private JiraConfig jiraConfig;
    @Inject
    private PriceListCache priceListCache;
    @Inject
    private QuoteLink quoteLink;
    @Inject
    private SapQuoteLink sapQuoteLink;
    @Validate(required = true, on = {SEARCH_ACTION})
    private String barcodes;

    private ConfigurableList.ResultList resultList;

    private SearchType searchType;

    private Set<String> storageLocations;

    private Set<String> unpickableBarcodes;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {
        MessageCollection messageCollection = new MessageCollection();
        List<String> barcodeList = Arrays.asList(this.barcodes.trim().split("\\s+"));
        Set<String> foundBarcodes = new HashSet<>();
        if (barcodeList.isEmpty()) {
            addMessage("Search values are required.");
        } else {
            switch (searchType) {
            case LAB_BATCH:
                List<LabBatch> labBatches = labBatchDao.findByListIdentifier(barcodeList);
                for (String barcode: barcodeList) {
                    boolean foundBatch = false;
                    for (LabBatch labBatch: labBatches) {
                        if (labBatch.getBatchName().equals(barcode)) {
                            foundBatch = true;
                            foundBarcodes.add(labBatch.getBatchName());
                        }
                    }
                    if (!foundBatch) {
                        messageCollection.addError("Failed to find Lab Batch " + barcode);
                    }
                }

                Set<LabVessel> startingLabVessels = new HashSet<>();
                for (LabBatch labBatch: labBatches) {
                    startingLabVessels.addAll(labBatch.getNonReworkStartingLabVessels());
                }
                if (!messageCollection.hasErrors()) {
                    pickLabVessels(new ArrayList<>(startingLabVessels), messageCollection);
                } else {
                    this.barcodes = StringUtils.join(foundBarcodes, '\n');
                }
                break;
            case LAB_VESSEL_BARCODE:
                List<LabVessel> labVessels = labVesselDao.findByListIdentifiers(barcodeList);
                for (String barcode: barcodeList) {
                    boolean foundVessel = false;
                    for (LabVessel labVessel: labVessels) {
                        if (labVessel.getLabel().equals(barcode)) {
                            foundVessel = true;
                            foundBarcodes.add(labVessel.getLabel());
                        }
                    }
                    if (!foundVessel) {
                        messageCollection.addError("Failed to find Lab Vessel " + barcode);
                    }
                }
                Set<String> notInStorage = pickLabVessels(labVessels, messageCollection);
                foundBarcodes.removeAll(notInStorage);
                this.barcodes = StringUtils.join(foundBarcodes, '\n');
                break;
            }
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    private Set<String> pickLabVessels(List<LabVessel> labVessels, MessageCollection messageCollection ) {
        this.storageLocations = new HashSet<>();
        this.unpickableBarcodes = new HashSet<>();
        Set<String> tubesNotInStorage = new HashSet<>();
        SearchContext searchContext = new SearchContext();
        searchContext.setBspUserList(bspUserList);
        searchContext.setUserBean(userBean);
        searchContext.setJiraConfig(jiraConfig);
        searchContext.setPriceListCache(priceListCache);
        searchContext.setQuoteLink(quoteLink);
        searchContext.setSapQuoteLink(sapQuoteLink);
        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("XL20 Picker");
        searchTerm.setPluginClass(PickerVesselPlugin.class);
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(searchTerm);
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.emptyMap(), 0, "ASC",
                ColumnEntity.LAB_VESSEL);
        List<LabVessel> searchTerms = new ArrayList<>();
        for (LabVessel labVessel : labVessels) {
            if (labVessel.getLabel().startsWith("AB")) {
                unpickableBarcodes.add(labVessel.getLabel());
                if (labVessel.getStorageLocation() != null) {
                    storageLocations.add(labVessel.getStorageLocation().buildLocationTrail());
                } else {
                    messageCollection.addError("Failed to find lab vessel in storage " + labVessel.getLabel());
                    tubesNotInStorage.add(labVessel.getLabel());
                }
                continue;
            }
            searchTerms.add(labVessel);
        }
        try {
            configurableList.addRows(searchTerms, searchContext);
            this.resultList = configurableList.getResultList();
            for(Iterator<ConfigurableList.ResultRow> iter = resultList.getResultRows().iterator(); iter.hasNext(); ) {
                ConfigurableList.ResultRow resultRow = iter.next();
                List<String> values = resultRow.getRenderableCells();
                if( values.get(1).isEmpty() ) {
                    // No container - not pickable
                    unpickableBarcodes.add(resultRow.getResultId());
                    iter.remove();
                } else {
                    this.storageLocations.add(values.get(0));
                }
            }
        } catch (NotInStorageException e) {
            for (String missing: e.getMissingVessels()) {
                messageCollection.addError("Failed to find lab vessel in storage " + missing);
                tubesNotInStorage.add(missing);
            }
        }

        return tubesNotInStorage;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(
            SearchType searchType) {
        this.searchType = searchType;
    }

    public String getBarcodes() {
        return barcodes;
    }

    public void setBarcodes(String barcodes) {
        this.barcodes = barcodes;
    }

    public Set<String> getStorageLocations() {
        return storageLocations;
    }

    public void setStorageLocations(Set<String> storageLocations) {
        this.storageLocations = storageLocations;
    }

    public Set<String> getUnpickableBarcodes() {
        return unpickableBarcodes;
    }

    public void setUnpickableBarcodes(Set<String> unpickableBarcodes) {
        this.unpickableBarcodes = unpickableBarcodes;
    }

    public String getEntityName() {
        return ENTITY_NAME;
    }

    public ConfigurableList.ResultList getResultList() {
        return resultList;
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

    // For Testing
    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public void setLabBatchDao(LabBatchDao labBatchDao) {
        this.labBatchDao = labBatchDao;
    }

}
