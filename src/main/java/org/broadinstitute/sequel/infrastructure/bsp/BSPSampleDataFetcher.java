package org.broadinstitute.sequel.infrastructure.bsp;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    // todo expose bulk fetch
    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        }
        else {
            Collection<String> sampleNames = new HashSet<String>();
            sampleNames.add(sampleName);
            // todo query multiple attributes at once for better efficiency.
            // don't just copy paste this!
            List<String[]> results = service.runSampleSearch(sampleNames, BSPSampleSearchColumn.PARTICIPANT_ID,
                    BSPSampleSearchColumn.ROOT_SAMPLE,
                    BSPSampleSearchColumn.STOCK_SAMPLE,
                    BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                    BSPSampleSearchColumn.COLLECTION,
                    BSPSampleSearchColumn.VOLUME,
                    BSPSampleSearchColumn.CONCENTRATION);

            if (results == null) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            if (results.isEmpty()) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            if (results.size() > 1) {
                throw new RuntimeException(results.size() + " sample results from BSP.  We were expecting just one.");
            }
            
            String[] columns = results.iterator().next();
            String patientId = columns[0];
            String rootSample = columns[1];
            String stockSample = columns[2];
            String collaboratorSampleId = columns[3];
            String collection = columns[4];
            String volume = columns[5];
            String concentration = columns[6];

            return new BSPSampleDTO(null,stockSample,rootSample,null,patientId,null,collaboratorSampleId,collection,volume,concentration);
        }
    }
    


}
