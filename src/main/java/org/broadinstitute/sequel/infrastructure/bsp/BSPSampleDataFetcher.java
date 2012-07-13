package org.broadinstitute.sequel.infrastructure.bsp;

import javax.inject.Inject;
import java.util.*;

/**
 * Wrapper around {@link BSPSampleSearchService} that
 * does a bit more object-ifying and type-safety.
 */
public class BSPSampleDataFetcher {
    
    @Inject BSPSampleSearchService service;

    public BSPSampleDataFetcher() {}

    /**
     * New one up using the given service.
     * @param service
     */
    public BSPSampleDataFetcher(BSPSampleSearchService service) {
        if (service == null) {
             throw new NullPointerException("service cannot be null.");
        }
        this.service = service;
    }

    /**
     * Fetch the data from BSP for the given sample.
     * @param sampleName
     * @return
     */
    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        }
        else {
            Collection<String> sampleNames = new HashSet<String>();
            sampleNames.add(sampleName);
            Map<String,BSPSampleDTO> sampleNameToDTO = fetchSamplesFromBSP(sampleNames);

            if (sampleNameToDTO.isEmpty()) {
                throw new RuntimeException("Could not find " + sampleName + " in bsp.");
            }
            else if (sampleNameToDTO.size() > 1) {
                throw new RuntimeException("Found " + sampleNameToDTO.size() + " possible matches in bsp.");
            }
            else {
                return sampleNameToDTO.entrySet().iterator().next().getValue();
            }
        }
    }

    /**
     * Fetch the data from bsp for multiple samples.
     * @param sampleNames
     * @return
     */
    public Map<String,BSPSampleDTO> fetchSamplesFromBSP(Collection<String> sampleNames) {
        if (sampleNames == null) {
            throw new NullPointerException("sampleNames cannot be null.");
        }
        if (sampleNames.isEmpty()) {
            throw new RuntimeException("sampleNames is empty.  No samples to lookup.");
        }
        final Map<String,BSPSampleDTO> sampleNameToDTO = new HashMap<String, BSPSampleDTO>();
        final List<String[]> results = getBSPResponse(sampleNames);

        for (String[] result : results) {
            BSPSampleDTO bspDTO = toDTO(result);
            String sampleName = bspDTO.getStockSample();

            if (sampleName.equals(bspDTO.getStockSample()) || sampleName.equals(bspDTO.getRootSample())) {
                sampleNames.remove(sampleName);
                sampleNameToDTO.put(sampleName,bspDTO);
            }
            else {
                throw new RuntimeException("Can't map bsp sample " + bspDTO.getStockSample() + " any of the " + sampleNames.size() + " queried samples");
            }
        }
        return sampleNameToDTO;
    }

    private BSPSampleDTO toDTO(String[] bspColumns) {
        String patientId = bspColumns[0];
        String rootSample = bspColumns[1];
        String stockSample = bspColumns[2];
        String collaboratorSampleId = bspColumns[3];
        String collection = bspColumns[4];
        String volume = bspColumns[5];
        String concentration = bspColumns[6];
        String organism = bspColumns[7];
        String sampleLsid = bspColumns[8];

        return new BSPSampleDTO(null,stockSample,rootSample,null,patientId,organism,collaboratorSampleId,collection,volume,concentration, sampleLsid);
    }

    private List<String[]> getBSPResponse(Collection<String> sampleNames) {
        return  service.runSampleSearch(sampleNames,
                BSPSampleSearchColumn.PARTICIPANT_ID,
                BSPSampleSearchColumn.ROOT_SAMPLE,
                BSPSampleSearchColumn.STOCK_SAMPLE,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                BSPSampleSearchColumn.COLLECTION,
                BSPSampleSearchColumn.VOLUME,
                BSPSampleSearchColumn.CONCENTRATION,
                BSPSampleSearchColumn.SPECIES,
                BSPSampleSearchColumn.LSID);
    }
}
