package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReadBucket;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularState;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Read-only sample metadata at the finest granularity necessary
 * for de-multiplexing.
 * 
 * Primary implementation is a {@link org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample},
 * although an interesting alternative implementation
 * could be used for scenarios where we want to
 * declare a tube completely divorced from upstream
 * transfers, or accept a tube with a novel
 * sample sheet from some external source,
 * bypassing the usual BSP checkout.
 */
@Entity
@Audited
@Table(schema = "mercury")
public abstract class StartingSample implements Starter {

    @SequenceGenerator(name = "SEQ_STARTING_SAMPLE", schema = "mercury", sequenceName = "SEQ_STARTING_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STARTING_SAMPLE")
    @Id
    private Long sampleId;

    private  String sampleName;

//    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
//    private ProjectPlan projectPlan;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    BSPSampleAuthorityTwoDTube bspSampleAuthorityTwoDTube;

    // todo arz hibernate-ify
    @Transient
    private Set<LabBatch> labBatches = new HashSet<LabBatch>();

    protected StartingSample(String sampleName/*, ProjectPlan projectPlan*/) {
        this.sampleName = sampleName;
        // todo jmt add to projectPlan?
//        this.projectPlan = projectPlan;
    }

    protected StartingSample() {
    }

    // some patient/clinical centric data lookups

    public abstract String getContainerId();

    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getLabel() {
        return sampleName;
    }

    public abstract String getPatientId();
    
    public abstract String getOrganism();

    public void logNote(StatusNote note) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public MolecularState getRootMolecularState() {
        throw new RuntimeException("not implemented");
    }

//    public ProjectPlan getRootProjectPlan() {
//        return projectPlan;
//    }
//
//    public void setRootProjectPlan(ProjectPlan rootProjectPlan) {
//        this.projectPlan = rootProjectPlan;
//    }

    public Collection<ReadBucket> getRootReadBuckets() {
        throw new RuntimeException("not implemented");
    }

    public SampleInstance.GSP_CONTROL_ROLE getRootControlRole() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Set<LabBatch> getLabBatches() {
        return labBatches;
    }

    @Override
    public void addLabBatch(LabBatch labBatch) {
        labBatches.add(labBatch);
    }

    public BSPSampleAuthorityTwoDTube getBspSampleAuthorityTwoDTube() {
        return bspSampleAuthorityTwoDTube;
    }

    public void setBspSampleAuthorityTwoDTube(BSPSampleAuthorityTwoDTube bspSampleAuthorityTwoDTube) {
        this.bspSampleAuthorityTwoDTube = bspSampleAuthorityTwoDTube;
    }
}
