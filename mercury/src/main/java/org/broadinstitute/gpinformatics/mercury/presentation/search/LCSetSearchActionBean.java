package org.broadinstitute.gpinformatics.mercury.presentation.search;

import clover.org.apache.commons.lang.StringUtils;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
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
    private SampleDataFetcher sampleDataFetcher;

    private Map<String, SampleData> sampleToBspPicoValueMap = new HashMap<>();
    private Map<LabVessel, Set<SampleInstance>> vesselToSampleInstanceMap = new HashMap<>();

    public Map<String, SampleData> getSampleToBspPicoValueMap() {
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
        if (!getSearchKey().startsWith("LCSET")) {
            setSearchKey("LCSET-" + getSearchKey());
        }
        doSearch(SearchType.BATCH_BY_KEY);
        filterResults();
        generateBspPicoMap();
        return new ForwardResolution(LCSET_SEARCH_PAGE);
    }

    /**
     * Create the cache of sample names to BspSampleData objects.
     */
    private void generateBspPicoMap() {
        Set<String> sampleNames = new HashSet<>();
        for (LabBatch batch : getFoundBatches()) {
            Set<LabVessel> startingBatchLabVessels = batch.getStartingBatchLabVessels();
            for (LabVessel startingVessel : startingBatchLabVessels) {
                sampleNames.addAll(startingVessel.getSampleNames());
                Map<LabEvent, Set<LabVessel>> eventListMap =
                        startingVessel.findVesselsForLabEventType(LabEventType.SAMPLE_IMPORT, true);
                for (Map.Entry<LabEvent, Set<LabVessel>> entry : eventListMap.entrySet()) {
                    for (LabVessel sourceVessel : entry.getValue()) {
                        Set<SampleInstance> sampleInstances = sourceVessel.getSampleInstances();
                        for (SampleInstance targetSampleInstance : sampleInstances) {
                            sampleNames.add(targetSampleInstance.getStartingSample().getSampleKey());
                        }
                        vesselToSampleInstanceMap.put(startingVessel, sampleInstances);
                    }
                }
            }
        }
        sampleToBspPicoValueMap = sampleDataFetcher.fetchSampleData(sampleNames);
    }

    public Double getExportedSampleConcentration(LabVessel vessel) {
        Set<SampleInstance> sampleInstances = vesselToSampleInstanceMap.get(vessel);
        if (!CollectionUtils.isEmpty(sampleInstances)) {
            if (sampleInstances.size() == 1) {
                return sampleToBspPicoValueMap.get(sampleInstances.iterator().next().getStartingSample().getSampleKey())
                        .getConcentration();
            } else {
                throw new RuntimeException("No support for pooled sample imports.");
            }
        }
        return Double.NaN;
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
        LabEvent latestEvent = latestEventForVessel.get(vessel);
        if (latestEvent == null) {
            Collection<LabVessel> descendants = vessel.getDescendantVessels();
            LabVessel[] events = descendants.toArray(new LabVessel[descendants.size()]);
            LabVessel lastVessel = events[descendants.size() - 1];
            latestEvent = lastVessel.getLatestEvent();
            latestEventForVessel.put(vessel, latestEvent);
        }
        return latestEvent;
    }

    public String getPositionsForEvent(LabVessel vessel, LabEventType type) {
        Map<LabEvent, Set<LabVessel>> eventMap = vessel.findVesselsForLabEventType(type, true);

        Set<MercurySample> allMercurySamples = new HashSet<>();
        Collection<LabVessel> descendants = vessel.getDescendantVessels();
        for (LabVessel descendant : descendants) {
            allMercurySamples.addAll(descendant.getMercurySamples());
        }

        List<String> positions = new ArrayList<>();
        for (Map.Entry<LabEvent, Set<LabVessel>> entry : eventMap.entrySet()) {
            if (entry.getKey().getLabEventType().equals(type)) {
                Set<VesselPosition> allPositions = new HashSet<>();
                for (LabVessel sourceVessel : entry.getValue()) {
                    Set<SampleInstance> sampleInstances = sourceVessel.getSampleInstances();
                    for (SampleInstance targetSampleInstance : sampleInstances) {
                        if (allMercurySamples.contains(targetSampleInstance.getStartingSample())) {
                            if (sourceVessel.getContainers().isEmpty()) {
                                allPositions.addAll(sourceVessel.getContainerRole()
                                        .getPositionsOfSampleInstance(targetSampleInstance));
                            } else {
                                for (VesselContainer container : sourceVessel.getContainers()) {
                                    allPositions.addAll(container.getPositionsOfSampleInstance(targetSampleInstance));
                                }
                            }
                        }
                    }
                }
                for (VesselPosition position : allPositions) {
                    positions.add(position.name());
                }
            }
        }

        return StringUtils.join(positions, ',');
    }
}
