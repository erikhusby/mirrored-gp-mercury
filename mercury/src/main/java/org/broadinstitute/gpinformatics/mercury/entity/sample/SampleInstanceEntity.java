package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;


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
    private SampleKitRequest sampleKitRequest;

    @ManyToOne
    private ReagentDesign reagentDesign;

    @ManyToOne
    private MolecularIndexingScheme molecularIndexingScheme;

    @ManyToOne
    private ResearchProject researchProject;

    @ManyToOne
    private ProductOrder productOrder;

    private String sampleLibraryName;

    private String referenceSequence;

    private String coverage;

    private String restrictionEnzyme;

    private String illumina_454_KitUsed;

    private Date uploadDate;

    private String librarySizeRange;

    private String jumpSize;

    private String insertSizeRange;

    private String pooled;

    private String libraryType;

    private String experiment;

    private String collaboratorSampleId;

    private String tissueType;

    private String sampleTubeBarcode;

    private String sampleNumber;

    private String readLength;

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


    public MercurySample getRootSample() {  return rootSample;  }

    public void setRootSample(MercurySample rootSample) { this.rootSample = rootSample;  }

    public void setLabVessel(LabVessel labVessel) { this.labVessel = labVessel; }

    public LabVessel getBarodedTube() { return labVessel;  }

    public String getSampleLibraryName() { return  sampleLibraryName; }

    public MolecularIndexingScheme getMolecularIndexingScheme() { return molecularIndexingScheme;  }

    public void setReagentDesign(ReagentDesign reagentDesign){ this.reagentDesign = reagentDesign; }

    public ReagentDesign getReagentDesign() { return this.reagentDesign; }

    public void setSampleInstanceEntityId(Long sampleInstanceEntityId) {this.sampleInstanceEntityId = sampleInstanceEntityId; }

    public Long getSampleInstanceEntityId() { return sampleInstanceEntityId; }

    public void setMolecularIndexScheme(MolecularIndexingScheme molecularIndexingScheme) { this.molecularIndexingScheme = molecularIndexingScheme; }

    public void setMercurySampleId(MercurySample mercurySample){ this.mercurySample = mercurySample; }

    public MercurySample getMercurySample() { return this.mercurySample;    }

    public void setSampleLibraryName(String sampleLibraryName) { this.sampleLibraryName = sampleLibraryName; }

    public void setUploadDate(){ this.uploadDate = new Date(); }

    public void setExperiment(String experiment) { this.experiment = experiment;  }

    public String getExperiment() { return this.experiment; }

    public String getReferenceSequence() { return this.referenceSequence; }

    public void setReferenceSequence(String referenceSequence) {this.referenceSequence = referenceSequence;}

    public String getCoverage() { return this.coverage; }

    public void setCoverage(String coverage) { this.coverage = coverage;}

    public String getRestrictionEnzyme(){ return this.restrictionEnzyme ;}

    public void setRestrictionEnzyme(String restrictionEnzyme) {this.restrictionEnzyme = restrictionEnzyme;}

    public String getIllumina454KitUsed() { return this.illumina_454_KitUsed; }

    public void setIllumina454KitUsed(String illumina454KitUsed) {this.illumina_454_KitUsed = illumina454KitUsed;}

    public String getJumpSize() { return this.jumpSize; }

    public void setJumpSize(String jumpSize) {this.jumpSize = jumpSize; }

    public String getLibrarySizeRange() { return this.librarySizeRange; }

    public void setLibrarySizeRange(String librarySizeRange) { this.librarySizeRange = librarySizeRange;}

    public String getInsertSizeRange() { return  this.insertSizeRange; }

    public void setInsertSizeRange(String insertSizeRange) { this.insertSizeRange = insertSizeRange;}

    public String getPooled() { return this.pooled; }

    public void setPooled(String pooled) { this.pooled = pooled; }

    public String getLibraryType() { return  this.libraryType; }

    public void setLibraryType(String libraryType) { this.libraryType = libraryType; }

    public String getCollaboratorSampleId() { return this.collaboratorSampleId; }

    public void setCollaboratorSampleId(String collaboratorSampleId) { this.collaboratorSampleId = collaboratorSampleId;}

    public void setTissueType(String tissueType) { this.tissueType = tissueType;}

    public String getTissueType() { return this.tissueType;}

    public void setSampleTubeBarcode(String sampleTubeBarcode) {this.sampleTubeBarcode = sampleTubeBarcode;}

    public String getSampleTubeBarcode() { return this.sampleTubeBarcode;}

    private String getSampleNumber() { return this.sampleNumber;}

    private void setSampleNumber(String sampleNumber) { this.sampleNumber = sampleNumber;}

    public void setDesiredReadLength(String readLength) { this.readLength = readLength;}

    public String getDesiredReadLength() {return this.readLength;}

    public SampleKitRequest getSampleKitRequest() { return sampleKitRequest; }

    public void setSampleKitRequest(SampleKitRequest sampleKitRequest) { this.sampleKitRequest = sampleKitRequest; }

    public ResearchProject getResearchProject() { return researchProject;  }

    public void setResearchProject(ResearchProject researchProject) { this.researchProject = researchProject; }

    public ProductOrder getProductOrder() { return productOrder; }

    public void setProductOrder(ProductOrder productOrder) { this.productOrder = productOrder; }
}
