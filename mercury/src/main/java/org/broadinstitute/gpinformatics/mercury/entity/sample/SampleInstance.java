package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury", name = "sample_instance")
@BatchSize(size = 50)
public class SampleInstance {


    @SequenceGenerator(name = "seq_sample_instance", schema = "mercury",  sequenceName = "seq_sample_instance")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sample_instance")
    @Id
    private Long sampleInstanceId;

    @Nonnull
    @ManyToOne
    private LabVessel labVessel;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private MercurySample mercurySample;

    @OneToMany(mappedBy = "sampleInstance", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SampleInstanceSubTasks> sampleInstanceSubTasks = new HashSet<>();

    @Nonnull
    @ManyToOne
    private ReagentDesign reagentDesign;

    @Nonnull
    @ManyToOne
    private MolecularIndexingScheme molecularIndexingScheme;

    private String sampleLibraryName;

    private Date uploadDate;

    private String rootSampleId;

    public void removeSubTasks() {
        sampleInstanceSubTasks.clear();
    }

    public void addSubTasks(SampleInstanceSubTasks subTasks) {
        sampleInstanceSubTasks.add(subTasks);
        subTasks.setSampleInstance(this);
    }

    /**
     *
     * Returns the Jira dev sub tasks in the order they were created. It concatenates them
     * into a single string for user-defined search.
     *
     */
    public String getSubTasks() {

       String subTask ="";
       for(SampleInstanceSubTasks task : sampleInstanceSubTasks)
       {
           subTask += (task.getSubTask() + " " );
       }

       return subTask;
    }

    public String getRootSampleId() {  return rootSampleId;  }

    public void setRootSampleId(String rootSampleId) { this.rootSampleId = rootSampleId;  }

    public void setLabVessel(LabVessel labVessel) { this.labVessel = labVessel; }

    public LabVessel getBarodedTube() { return labVessel;  }

    public String getSampleLibraryName() { return  sampleLibraryName; }

    public MolecularIndexingScheme getMolecularIndexingScheme() { return molecularIndexingScheme;  }

    public void setReagentDesign(ReagentDesign reagentDesign){ this.reagentDesign = reagentDesign; }

    public Long getSampleInstanceId() { return sampleInstanceId; }

    public void setMolecularIndexScheme(MolecularIndexingScheme molecularIndexingScheme) { this.molecularIndexingScheme = molecularIndexingScheme; }

    public void setMercurySampleId(MercurySample mercurySample){ this.mercurySample = mercurySample; }

    public void setSampleLibraryName(String sampleLibraryName) { this.sampleLibraryName = sampleLibraryName; }

    public void setUploadDate(){ this.uploadDate = new Date(); }

}
