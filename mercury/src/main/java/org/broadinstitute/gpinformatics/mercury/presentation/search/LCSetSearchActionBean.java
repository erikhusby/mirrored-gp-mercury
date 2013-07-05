package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(LCSetSearchActionBean.ACTIONBEAN_URL_BINDING)
public class LCSetSearchActionBean extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/lcset.action";
    public static final String LCSET_SEARCH = "lcsetSearch";
    private static final String SESSION_LIST_PAGE = "/search/lcset_search.jsp";

    @Inject
    private BSPSampleDataFetcher sampleDataFetcher;

    private Map<String, BSPSampleDTO> sampleToBspPicoValueMap = new HashMap<>();

    public Map<String, BSPSampleDTO> getSampleToBspPicoValueMap() {
        return sampleToBspPicoValueMap;
    }

    public void setSampleToBspPicoValueMap(Map<String, BSPSampleDTO> sampleToBspPicoValueMap) {
        this.sampleToBspPicoValueMap = sampleToBspPicoValueMap;
    }

    @Override
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(LCSET_SEARCH)
    public Resolution lcsetSearch() throws Exception {
        doSearch(SearchType.BATCH_BY_KEY);
        filterResults();
        generateBspPicoMap();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    private void generateBspPicoMap() {
        Set<String> sampleNames = new HashSet<>();

        for (LabBatch batch : getFoundBatches()) {
            for (LabVessel startingVessel : batch.getStartingBatchLabVessels()) {
                sampleNames.addAll(startingVessel.getSampleNames());
            }
        }

        sampleToBspPicoValueMap = sampleDataFetcher.fetchSamplesFromBSP(sampleNames);
    }

    private void filterResults() {
        List<LabBatch> filteredBatches = new ArrayList<>();
        for (LabBatch batch : getFoundBatches()) {
            if (batch.getLabBatchType().equals(LabBatch.LabBatchType.WORKFLOW)) {
                filteredBatches.add(batch);
            }
        }
        setFoundBatches(filteredBatches);
    }


}
