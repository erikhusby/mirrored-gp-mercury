package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(SampleSearchActionBean.ACTIONBEAN_URL_BINDING)
public class SampleSearchActionBean extends SearchActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/sample.action";
    private static final String SESSION_LIST_PAGE = "/search/sample_search.jsp";
    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    private Map<MercurySample, Set<LabVessel>> mercurySampleToVessels = new HashMap<MercurySample, Set<LabVessel>>();

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
        Map<String, BSPSampleDTO> sampleToDTO = bspSampleDataFetcher.fetchSamplesFromBSP(searchList);
        for (String searchKey : searchList) {
            List<MercurySample> samples = getMercurySampleDao().findBySampleKey(searchKey);
            List<LabVessel> vessels = getLabVesselDao().findBySampleKey(searchKey);
            Set<LabVessel> allVessels = new LinkedHashSet<LabVessel>(vessels);
            for (LabVessel vessel : vessels) {
                allVessels.addAll(vessel.getDescendantVessels());
            }
            if (!samples.isEmpty()) {
                MercurySample sample = samples.get(0);
                mercurySampleToVessels.put(sample, allVessels);
                sample.setBspSampleDTO(sampleToDTO.get(sample.getSampleKey()));
            }
        }
        setSearchDone(true);
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

}