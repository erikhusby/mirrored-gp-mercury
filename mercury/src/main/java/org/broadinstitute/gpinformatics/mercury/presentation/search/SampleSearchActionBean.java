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
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.ArrayList;
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
            MercurySample foundSample = mercurySampleDao.findBySampleKey(searchKey);
            List<LabVessel> vessels = getLabVesselDao().findBySampleKey(searchKey);
            Set<LabVessel> allVessels = new LinkedHashSet<>(vessels);
            for (LabVessel vessel : vessels) {
                allVessels.addAll(vessel.getAncestorVessels());
                allVessels.addAll(vessel.getDescendantVessels());
            }
            if (foundSample != null) {
                List<String> sampleNames = new ArrayList<>();
                sampleNames.add(foundSample.getSampleKey());

                sampleDTOMap.putAll(sampleDataFetcher.fetchSampleData(sampleNames));

                mercurySampleToVessels.put(foundSample, allVessels);
                SampleData sampleData = sampleDTOMap.get(foundSample.getSampleKey());
                if (sampleData != null) {
                    foundSample.setSampleData(sampleData);
                }
                foundSampleNames.add(foundSample.getSampleKey());
            }
        }

        setResultSummaryString(SearchType.SAMPLES_BY_BARCODE.createSummaryString(searchList, foundSampleNames));
        setSearchDone(true);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public Set<SampleInstanceV2> getSampleInstancesForSample(LabVessel vessel, MercurySample sample) {
        Set<SampleInstanceV2> allSamples = vessel.getSampleInstancesV2();
        Set<SampleInstanceV2> filteredSamples = new HashSet<>();

        SampleData sampleDTO = sampleDTOMap.get(sample.getSampleKey());
        for (SampleInstanceV2 sampleInstance : allSamples) {
            SampleData sampleInstanceDTO = sampleDTOMap.get(sampleInstance.getNearestMercurySampleName());
            //check if samples have a common ancestor
            if (sampleDTO != null && sampleInstanceDTO != null
                && sampleInstance.getMercuryRootSampleName() != null
                && (sampleInstance.getMercuryRootSampleName().equals(sample.getSampleKey())
                    || sampleInstance.getMercuryRootSampleName().equals(sampleDTO.getStockSample())
                    || sampleInstanceDTO.getStockSample().equals(sample.getSampleKey())
                    || sampleInstanceDTO.getStockSample().equals(sampleDTO.getStockSample()))) {
                filteredSamples.add(sampleInstance);
            }
        }
        return filteredSamples;
    }
}
