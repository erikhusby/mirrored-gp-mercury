package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

/**
 * The basic plan here is to store only the
 * name of the sample, and then have
 * a service lookup the real data from bsp.
 */
@Entity
public class BSPSample extends StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    @Transient
    private BSPSampleDTO bspDTO;

    protected BSPSample() {}
    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     * @param  plan
     * @param  bspDTO The DTO fetched from {@link BSPSampleDataFetcher}.
     *                If you're creating lots of these objects, you should probably
     *                do a bulk fetch of all the samples.  Or maybe you don't
     *                want to fetch the BSP data at all unless the user
     *                drills into it.  Either way, you have some decisions to make
     *                about performance.
     */
    public BSPSample(String sampleName,
                     ProjectPlan plan,
                     BSPSampleDTO bspDTO) {
        this(sampleName,plan);
        this.bspDTO = bspDTO;
    }
    
    public BSPSample(String sampleName,
                     ProjectPlan plan) {
        super(sampleName, plan);
    }

    public void setBspDTO(BSPSampleDTO dto) {
        this.bspDTO = dto;
    }

    @Transient
    /**
     * Has the underlying BSP DTO been initialized?
     */
    public boolean hasBSPDTOBeenInitialized() {
        return bspDTO != null;
    }

    @Override
    @Transient
    /**
     * Gets the container id from the underlying
     * BSP DTO.
     */
    public String getContainerId() {
        return bspDTO.getContainerId();
    }

    @Override
    @Transient
    /**
     * Gets the patient id from the underlying
     * BSP DTO.
     */
    public String getPatientId() {
        return bspDTO.getPatientId();
    }

    @Override
    @Transient
    /**
     * Gets the organism name from the
     * underlying BSP DTO.
     */
    public String getOrganism() {
        return bspDTO.getOrganism();
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        final Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        sampleInstances.add(new SampleInstance(this,null,getRootProjectPlan(),null,getRootProjectPlan().getWorkflowDescription()));
        return sampleInstances;
    }
}
