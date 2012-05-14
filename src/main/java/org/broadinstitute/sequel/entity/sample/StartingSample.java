package org.broadinstitute.sequel.entity.sample;

import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.MolecularState;

import javax.persistence.Transient;
import java.util.Collection;

/**
 * Read-only sample metadata at the finest granularity necessary
 * for de-multiplexing.
 * 
 * Primary implementation is a {@link org.broadinstitute.sequel.entity.bsp.BSPSample},
 * although an interesting alternative implementation
 * could be used for scenarios where we want to
 * declare a tube completely divorced from upstream
 * transfers, or accept a tube with a novel
 * sample sheet from some external source,
 * bypassing the usual BSP checkout.
 */
public interface StartingSample {

    // some patient/clinical centric data lookups

    public String getContainerId();
    
    public String getSampleName();
    
    public String getPatientId();
    
    public String getOrganism();
    
    public void logNote(StatusNote note);

    public MolecularState getRootMolecularState();

    public ProjectPlan getRootProjectPlan();
    
    public void setRootProjectPlan(ProjectPlan rootProjectPlan);

    public Collection<ReadBucket> getRootReadBuckets();

    public SampleInstance.GSP_CONTROL_ROLE getRootControlRole();

    @Transient
    public SampleInstance createSampleInstance();

}
