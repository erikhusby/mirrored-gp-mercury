package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleInstanceImpl;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.vessel.MolecularStateImpl;

import javax.persistence.PostLoad;
import java.util.Collection;

/**
 * The basic plan here is to store only the
 * name of the sample, and then have
 * a service lookup the real data from bsp.
 */
public class BSPSample implements StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    private  String sampleName;

    private ProjectPlan projectPlan;

    private BSPSampleDTO bspDTO;

    public BSPSample() {

    }

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     */
    public BSPSample(String sampleName,
                     ProjectPlan plan,
                     BSPSampleDTO bspDTO) {
        this(sampleName,plan);
        this.bspDTO = bspDTO;
    }
    
    public BSPSample(String sampleName,
                     ProjectPlan plan) {
        this.sampleName = sampleName;
        this.projectPlan = plan;
    }


    public void setSampleName(String sampleName) {
        if (sampleName == null) {
            throw new NullPointerException("sampleName cannot be null.");
        }
        this.sampleName = sampleName;
    }

    @Override
    public void logNote(StatusNote note) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public MolecularState getRootMolecularState() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ProjectPlan getRootProjectPlan() {
        return projectPlan;
    }

    @Override
    public void setRootProjectPlan(ProjectPlan rootProjectPlan) {
        this.projectPlan = rootProjectPlan;
    }

    @Override
    public Collection<ReadBucket> getRootReadBuckets() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public SampleInstance.GSP_CONTROL_ROLE getRootControlRole() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public SampleInstanceImpl createSampleInstance() {
        return new SampleInstanceImpl(this, SampleInstance.GSP_CONTROL_ROLE.NONE, projectPlan, new MolecularStateImpl(), null);
    }

    @Override
    public String getContainerId() {
        return bspDTO.getContainerId();
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getPatientId() {
        return bspDTO.getPatientId();
    }

    @Override
    public String getOrganism() {
        return bspDTO.getOrganism();
    }
}
