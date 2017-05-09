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
import java.util.ArrayList;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury", name = "sample_instance_entity")
@BatchSize(size = 50)
public class SampleInstanceEntity {


    @SequenceGenerator(name = "seq_sample_instance_entity", schema = "mercury",  sequenceName = "seq_sample_instance_entity")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_sample_instance_entity")
    @Id
    private Long sampleInstanceEntityId;

    @Nonnull
    @ManyToOne
    private LabVessel labVessel;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private MercurySample mercurySample;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private MercurySample rootSample;


    @OneToMany(mappedBy = "sampleInstanceEntity", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SampleInstanceEntityTsk> sampleInstanceEntityTsks = new HashSet<>();

    @ManyToOne
    private ReagentDesign reagentDesign;

    @ManyToOne
    private MolecularIndexingScheme molecularIndexingScheme;

    private String sampleLibraryName;

    private Date uploadDate;

    private String experiment;

    private Integer readLength;

    public void removeSubTasks() {
        sampleInstanceEntityTsks.clear();
    }

    public void addSubTasks(SampleInstanceEntityTsk subTasks) {
        sampleInstanceEntityTsks.add(subTasks);
        subTasks.setSampleInstanceEntity(this);
    }

    /**
     *
     * Returns the Jira dev sub tasks in the order they were created. It concatenates them
     * into a single string for user-defined search.
     *
     */
    public List<String> getSubTasks() {
       List<String> subTask = new ArrayList<>();
       for(SampleInstanceEntityTsk task : sampleInstanceEntityTsks)
       {
           subTask.add(task.getSubTask());
       }

       return subTask;
    }


    public Integer getReadLength() { return readLength; }

    public void setReadLength(Integer readLength) { this.readLength = readLength; }

    public MercurySample getRootSample() {  return rootSample;  }

    public void setRootSample(MercurySample rootSample) { this.rootSample = rootSample;  }

    public void setLabVessel(LabVessel labVessel) { this.labVessel = labVessel; }

    public MolecularIndexingScheme getMolecularIndexingScheme() { return molecularIndexingScheme;  }

    public void setReagentDesign(ReagentDesign reagentDesign){ this.reagentDesign = reagentDesign; }

    public ReagentDesign getReagentDesign() { return this.reagentDesign; }

    public void setMolecularIndexScheme(MolecularIndexingScheme molecularIndexingScheme) { this.molecularIndexingScheme = molecularIndexingScheme; }

    public void setMercurySampleId(MercurySample mercurySample){ this.mercurySample = mercurySample; }

    public MercurySample getMercurySample() { return this.mercurySample;    }

    public void setSampleLibraryName(String sampleLibraryName) { this.sampleLibraryName = sampleLibraryName; }

    public String getSampleLibraryName() { return sampleLibraryName;  }

    public void setUploadDate(){ this.uploadDate = new Date(); }

    public void setExperiment(String experiment) { this.experiment = experiment;  }

    public String getExperiment() { return experiment; }
}
