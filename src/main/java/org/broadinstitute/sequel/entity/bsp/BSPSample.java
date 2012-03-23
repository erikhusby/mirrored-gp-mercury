package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.control.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleInstanceImpl;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.vessel.MolecularStateImpl;

import javax.persistence.PostLoad;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The basic plan here is to store only the
 * name of the sample, and then have
 * a service lookup the real data from bsp.
 */
public class BSPSample implements StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    private  String sampleName;

    private ProjectPlan projectPlan;
    
    private String patientId;

    /**
     * Conceptually we want this injected by the framework,
     * but injecting something with CDI into a hibernate
     * entity is a bit weird.  So we're going to try
     * using the @PostLoad annotation to instantiate
     * this service whenever we fetch one of these from
     * the database or new one up.
     */
    BSPSampleDataFetcher dataFetcher;

    private boolean hasFetched;

    public BSPSample() {
        // we're calling the @PostLoad method
        // so that when we new up one of these,
        // we pickup the right service for fetching
        // data from bsp.
        initializeDataFetcher();
    }

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     */
    public BSPSample(String sampleName,
                     ProjectPlan plan,
                     BSPSampleDataFetcher dataFetcher) {
        this(sampleName,plan);
        this.dataFetcher = dataFetcher;
    }
    
    public BSPSample(String sampleName,
                     ProjectPlan plan) {
        this();
        this.sampleName = sampleName;
        this.projectPlan = plan;
    }


    // todo make sure we hit this with an integration test
    @PostLoad
    void initializeDataFetcher() {
        if (dataFetcher == null) {
            dataFetcher = new BSPSampleDataFetcher();
        }
    }

    /**
     * Fetches all fields live from
     * {@link #dataFetcher}
     */
    private void fetchAllFieldsIfUnfetched() {
        if (!hasFetched) {
            if (dataFetcher != null) {
                dataFetcher.fetchFieldsFromBSP(sampleName);
                patientId = dataFetcher.getPatientId();
                hasFetched = true;
            }
        }
    }

    public void setSampleName(String sampleName) {
        if (sampleName == null) {
            throw new NullPointerException("sampleName cannot be null.");
        }
        this.sampleName = sampleName;
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
        fetchAllFieldsIfUnfetched();
        return patientId;
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

}
