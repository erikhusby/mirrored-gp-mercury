package org.broadinstitute.sequel.infrastructure.bsp;

/**
 * A simple DTO for fetching commonly used 
 * data from BSP.
 */
public class BSPSampleDTO {

    private final String patientId;
    
    public BSPSampleDTO(String containerId,
                        String stockSample,
                        String rootSample,
                        String aliquotSample,
                        String patientId,
                        String organism) {
        this.patientId = patientId;
    }

    public String getContainerId() {
        throw new RuntimeException("not implemented yet.");
    }

    public String getPatientId() {
        return patientId;
    }

    public String getOrganism() {
        throw new RuntimeException("not implemented yet.");
    }
}
