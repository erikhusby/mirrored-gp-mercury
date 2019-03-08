package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(SampleLibrariesActionBean.ACTIONBEAN_URL_BINDING)
public class SampleLibrariesActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/sample/sampleLibraries.action";
    private static final String SHOW_LIBRARIES = "showLibraries";
    private static final String SAMPLE_LIBRARIES_PAGE = "/sample/sample_libraries.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;


    private String searchKey;
    private Map<String, SampleData> sampleToBspPicoValueMap = new HashMap<>();
    private Map<LabVessel, Map<LabEvent, Set<LabVessel>>> vesselToEventVesselsMap = new HashMap<>();
    private List<String> selectedSamples = new ArrayList<>();
    private Map<String, List<LabVessel>> sampleNameToVesselsMap = new HashMap<>();

    public Map<String, SampleData> getSampleToBspPicoValueMap() {
        return sampleToBspPicoValueMap;
    }

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    @DefaultHandler
    @HandlesEvent(SHOW_LIBRARIES)
    public Resolution showLibraries() {
        generateBspPicoMap();
        return new ForwardResolution(SAMPLE_LIBRARIES_PAGE);
    }

    /**
     * Create the cache of sample names to BspSampleData objects.
     */
    private void generateBspPicoMap() {
        Set<String> sampleNames = new HashSet<>();
        List<LabEventType> eventTypes = new ArrayList<>();
        eventTypes.add(LabEventType.SAMPLE_IMPORT);
        eventTypes.add(LabEventType.POND_REGISTRATION);
        eventTypes.add(LabEventType.NORMALIZED_CATCH_REGISTRATION);
        eventTypes.add(LabEventType.POOLING_TRANSFER);
        eventTypes.add(LabEventType.NORMALIZATION_TRANSFER);
        eventTypes.add(LabEventType.DENATURE_TRANSFER);
        eventTypes.add(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER);

        List<LabVessel> vessels = labVesselDao.findByUnknownBarcodeTypeList(selectedSamples);
        for (LabVessel startingVessel : vessels) {
            Collection<String> vesselSampleNames = startingVessel.getSampleNames();
            sampleNames.addAll(vesselSampleNames);
            for (String sampleName : vesselSampleNames) {
                List<LabVessel> vesselList = sampleNameToVesselsMap.get(sampleName);
                if (vesselList == null) {
                    vesselList = new ArrayList<>();
                    sampleNameToVesselsMap.put(sampleName, vesselList);
                }
                vesselList.add(startingVessel);
            }
            Map<LabEvent, Set<LabVessel>> eventListMap =
                    startingVessel.findVesselsForLabEventTypes(eventTypes, true);
            vesselToEventVesselsMap.put(startingVessel, eventListMap);
        }
        sampleToBspPicoValueMap = sampleDataFetcher.fetchSampleData(sampleNames);
    }

    public List<MolecularIndexReagent> getIndexesForSample(String sampleName) {
        List<MolecularIndexReagent> allIndexes = new ArrayList<>();
        List<LabVessel> vessels = sampleNameToVesselsMap.get(sampleName);
        for (LabVessel vessel : vessels) {
            Set<MercurySample> mercurySamples = new HashSet<>();
            Collection<LabVessel> descendants = vessel.getDescendantVessels();
            for (LabVessel descendant : descendants) {
                mercurySamples.addAll(descendant.getMercurySamples());
            }
            Map<LabEvent, Set<LabVessel>> eventToVessels = vesselToEventVesselsMap.get(vessel);
            for (Map.Entry<LabEvent, Set<LabVessel>> entry : eventToVessels.entrySet()) {
                if (entry.getKey().getLabEventType().equals(LabEventType.POND_REGISTRATION)) {
                    for (LabVessel curVessel : entry.getValue()) {
                        for (MercurySample sample : mercurySamples) {
                            allIndexes.addAll(curVessel.getIndexesForSample(sample));
                        }
                    }
                }
            }
        }
        return allIndexes;
    }

    public List<LabVessel> getVesselStringBySampleAndType(String sampleName, LabEventType type) {
        List<LabVessel> allVessels = new ArrayList<>();
        List<LabVessel> vessels = sampleNameToVesselsMap.get(sampleName);
        for (LabVessel vessel : vessels) {
            Map<LabEvent, Set<LabVessel>> eventToVessels = vesselToEventVesselsMap.get(vessel);
            for (Map.Entry<LabEvent, Set<LabVessel>> entry : eventToVessels.entrySet()) {
                if (entry.getKey().getLabEventType().equals(type)) {
                    for (LabVessel curVessel : entry.getValue()) {
                        allVessels.add(curVessel);
                    }
                }
            }
        }
        return allVessels;
    }

    @ValidationMethod(on = SHOW_LIBRARIES)
    public void validateSelection() {
        if (selectedSamples.isEmpty()) {
            addGlobalValidationError("You must select one or more samples to show libraries.");
        }
    }
}
