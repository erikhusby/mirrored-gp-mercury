package org.broadinstitute.sequel.infrastructure.bsp;

import java.io.Serializable;

/**
 * A simple DTO for fetching commonly used 
 * data from BSP.
 */
public class BSPSampleDTO implements Serializable {

    private final String patientId;
    
    private final String stockSample;
    
    private final String rootSample;
    
    private final String collaboratorsSampleName;
    
    private final String collection;
    
    private final String volume;
    
    private final String concentration;
    
    public BSPSampleDTO(String containerId,
                        String stockSample,
                        String rootSample,
                        String aliquotSample,
                        String patientId,
                        String organism,
                        String collaboratorsSampleName,
                        String collection,
                        String volume,
                        String concentration) {
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.patientId = patientId;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;
        this.volume = volume;
        this.concentration = concentration;
        
    }
   
    public String getVolume() {
        // todo strongly type, figure out units
        return volume;
    }
    
    public String getConcentration() {
        // todo strongly type, figure out units
        return concentration;
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
