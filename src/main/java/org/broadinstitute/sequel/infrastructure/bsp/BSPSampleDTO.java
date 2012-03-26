package org.broadinstitute.sequel.infrastructure.bsp;

/**
 * A simple DTO for fetching commonly used 
 * data from BSP.
 */
public class BSPSampleDTO {

    private final String patientId;
    
    private final String stockSample;
    
    private final String rootSample;
    
    private final String collaboratorsSampleName;
    
    private final String collection;
    
    public BSPSampleDTO(String containerId,
                        String stockSample,
                        String rootSample,
                        String aliquotSample,
                        String patientId,
                        String organism,
                        String collaboratorsSampleName,
                        String collection) {
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.patientId = patientId;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;
        
    }
    
    public String getRootSample() {
        return rootSample;
    }

    public String getStockSample() {
        return stockSample;
    }
    
    /**
     * Returns the name of the BSP collection
     * in which this sample resides
     * @return
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Gets the name that the collaborator
     * gave to this sample.
     * @return
     */
    public String getCollaboratorsSampleName() {
        return collaboratorsSampleName;
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
