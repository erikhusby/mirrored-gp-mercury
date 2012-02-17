package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleInstanceImpl;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;

import java.util.Collection;

/**
 * The basic plan here is to store only the
 * name then
 */
public class BSPSample implements StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    private final String sampleName;

    private Project project;

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     */
    public BSPSample(String sampleName,Project p) {
        this.sampleName = sampleName;
        this.project = p;

    }

    @Override
    public String getContainerId() {
        return sampleName;
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getPatientId() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getOrganism() {
        throw new RuntimeException("I haven't been written yet.");
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
    public Project getRootProject() {
        return project;
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
        return new SampleInstanceImpl(this, SampleInstance.GSP_CONTROL_ROLE.NONE,project,null,null);
    }

    @Override
    public void setRootProject(Project rootProject) {
        this.project = rootProject;
    }
}
