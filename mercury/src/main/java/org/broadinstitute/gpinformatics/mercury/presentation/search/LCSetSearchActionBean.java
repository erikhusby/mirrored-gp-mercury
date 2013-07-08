package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(LCSetSearchActionBean.ACTIONBEAN_URL_BINDING)
public class LCSetSearchActionBean extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/lcset.action";
    public static final String LCSET_SEARCH = "lcsetSearch";
    private static final String LCSET_SEARCH_PAGE = "/search/lcset_search.jsp";
    private Map<LabVessel, LabEvent> latestEventForVessel = new HashMap<>();

    @Inject
    private BSPSampleDataFetcher sampleDataFetcher;

    private Map<String, BSPSampleDTO> sampleToBspPicoValueMap = new HashMap<>();

    public Map<String, BSPSampleDTO> getSampleToBspPicoValueMap() {
        return sampleToBspPicoValueMap;
    }

    @Override
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(LCSET_SEARCH_PAGE);
    }

    @HandlesEvent(LCSET_SEARCH)
    public Resolution lcsetSearch() throws Exception {
        doSearch(SearchType.BATCH_BY_KEY);
        filterResults();
        generateBspPicoMap();
        return new ForwardResolution(LCSET_SEARCH_PAGE);
    }

    /**
     * Create the cache of sample names to BSPSampleDTO objects.
     */
    private void generateBspPicoMap() {
        Set<String> sampleNames = new HashSet<>();
        for (LabBatch batch : getFoundBatches()) {
            for (LabVessel startingVessel : batch.getStartingBatchLabVessels()) {
                sampleNames.addAll(startingVessel.getSampleNames());
            }
        }
        sampleToBspPicoValueMap = sampleDataFetcher.fetchSamplesFromBSP(sampleNames);
    }

    /**
     * Filter out the non LCSET batches.
     */
    private void filterResults() {
        List<LabBatch> filteredBatches = new ArrayList<>();
        for (LabBatch batch : getFoundBatches()) {
            if (batch.getLabBatchType().equals(LabBatch.LabBatchType.WORKFLOW)) {
                filteredBatches.add(batch);
            }
        }
        setFoundBatches(filteredBatches);
    }

    /**
     * This method gets the latest event for the last lab vessel that this sample was in.
     *
     * @param vessel The starting lab vessel to search for the last event from.
     *
     * @return The last event we've seen this sample or its descendants go through.
     */
    public LabEvent getLatestEventForVessel(LabVessel vessel) {
        Collection<LabVessel> descendants = vessel.getDescendantVessels();
        LabVessel[] events = descendants.toArray(new LabVessel[descendants.size()]);
        LabVessel lastVessel = events[descendants.size() - 1];
        LabEvent latestEvent = latestEventForVessel.get(lastVessel);
        if (latestEvent == null) {
            latestEvent = lastVessel.getLatestEvent();
            latestEventForVessel.put(lastVessel, latestEvent);
        }
        return latestEvent;
    }
}
