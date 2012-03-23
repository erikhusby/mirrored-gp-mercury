package org.broadinstitute.sequel.control.bsp;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BSPSampleDataFetcher {
    
    @Inject BSPSampleSearchService service;
    
    private String patientId;

    public BSPSampleDataFetcher() {}

    public BSPSampleDataFetcher(BSPSampleSearchService service) {
        if (service == null) {
             throw new NullPointerException("service cannot be null.");
        }
        this.service = service;
    }

    public void fetchFieldsFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        }
        else {
            Collection<String> sampleNames = new HashSet<String>();
            sampleNames.add(sampleName);
            // todo query multiple attributes at once for better efficiency.
            // don't just copy paste this!
            List<String[]> results = service.runSampleSearch(sampleNames, BSPSampleSearchColumn.PARTICIPANT_ID);

            if (results == null) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            if (results.isEmpty()) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            Set<String> patientIds = new HashSet<String>();
            for (String[] result : results) {
                if (result == null) {
                    throw new RuntimeException("No patient id for sample " + sampleName);
                }
                if (result.length < 1) {
                    throw new RuntimeException("No patient id for sample " + sampleName);
                }
                patientIds.add(result[0]);
            }

            if (patientIds.size() > 1) {
                throw new RuntimeException("Multiple patient ids found for sample " + sampleName);
            }
            patientId = patientIds.iterator().next();
        }
    }
    
    public String getPatientId() {
        return patientId;
    }
    

}
