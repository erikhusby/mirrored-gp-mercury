package org.broadinstitute.sequel;

import javax.persistence.Transient;
import java.util.Collection;

/**
 * What makes {@link StartingSample} different
 * from {@link Goop} is taht {@link StartingSample }
 * is read-only data,
 * at the finest granularity necessary
 * for de-multiplexing.
 * 
 * Primary implementation is a {@link BSPSample},
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

    public Project getRootProject();

    public Collection<ReadBucket> getRootReadBuckets();

    public SampleInstance.GSP_CONTROL_ROLE getRootControlRole();

    @Transient
    public SampleInstanceImpl createSampleInstance();
    
}
