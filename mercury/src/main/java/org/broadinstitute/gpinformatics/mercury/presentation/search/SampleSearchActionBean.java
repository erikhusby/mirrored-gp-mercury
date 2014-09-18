package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(SampleSearchActionBean.ACTIONBEAN_URL_BINDING)
public class SampleSearchActionBean extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/sample.action";
    private static final String SESSION_LIST_PAGE = "/search/sample_search.jsp";

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private MercurySampleDao mercurySampleDao;

    private Map<String, SampleData> sampleDTOMap = new HashMap<>();

    // order of samples in result list should match input order from text area; hence LinkedHashMap
    private Map<MercurySample, Set<LabVessel>> mercurySampleToVessels = new LinkedHashMap<>();

    public Map<MercurySample, Set<LabVessel>> getMercurySampleToVessels() {
        return mercurySampleToVessels;
    }

    public void setMercurySampleToVessels(Map<MercurySample, Set<LabVessel>> mercurySampleToVessels) {
        this.mercurySampleToVessels = mercurySampleToVessels;
    }

    @Override
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("sampleSearch")
    public Resolution sampleSearch() throws Exception {
        List<String> searchList = cleanInputString(getSearchKey());
        setNumSearchTerms(searchList.size());
        Set<String> foundSampleNames=new HashSet<>(searchList.size());

        for (String searchKey : searchList) {
            Set<MercurySample> samples = new HashSet<>();
            samples.addAll(mercurySampleDao.findBySampleKey(searchKey));
            List<LabVessel> vessels = getLabVesselDao().findBySampleKey(searchKey);
            Set<LabVessel> allVessels = new LinkedHashSet<>(vessels);
            for (LabVessel vessel : vessels) {
                allVessels.addAll(vessel.getAncestorVessels());
                allVessels.addAll(vessel.getDescendantVessels());
            }
            if (!samples.isEmpty()) {
                List<String> sampleNames = new ArrayList<>();
                for (MercurySample sample : samples) {
                    sampleNames.add(sample.getSampleKey());
                }
                sampleDTOMap.putAll(sampleDataFetcher.fetchSampleData((Collection<String>) sampleNames));
                for (MercurySample sample : samples) {
                    mercurySampleToVessels.put(sample, allVessels);
                    SampleData sampleData = sampleDTOMap.get(sample.getSampleKey());
                    if (sampleData != null) {
                        sample.setSampleData(sampleData);
                    }
                    foundSampleNames.add(sample.getSampleKey());
                }
            }
        }

        setResultSummaryString(SearchType.SAMPLES_BY_BARCODE.createSummaryString(searchList, foundSampleNames));
        setSearchDone(true);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public Set<SampleInstance> getSampleInstancesForSample(LabVessel vessel, MercurySample sample,
                                                           LabVessel.SampleType type) {
        Set<SampleInstance> allSamples = vessel.getSampleInstances(type, null);
        Set<SampleInstance> filteredSamples = new HashSet<>();

        SampleData sampleDTO = sampleDTOMap.get(sample.getSampleKey());
        for (SampleInstance sampleInstance : allSamples) {
            SampleData sampleInstanceDTO = sampleDTOMap.get(sampleInstance.getStartingSample().getSampleKey());
            //check if samples have a common ancestor
            if (sampleDTO != null && sampleInstanceDTO != null
                && sampleInstance.getStartingSample() != null
                && (sampleInstance.getStartingSample().equals(sample)
                    || sampleInstance.getStartingSample().getSampleKey().equals(sampleDTO.getStockSample())
                    || sampleInstanceDTO.getStockSample().equals(sample.getSampleKey())
                    || sampleInstanceDTO.getStockSample().equals(sampleDTO.getStockSample()))) {
                filteredSamples.add(sampleInstance);
            }
        }
        return filteredSamples;
    }
}
