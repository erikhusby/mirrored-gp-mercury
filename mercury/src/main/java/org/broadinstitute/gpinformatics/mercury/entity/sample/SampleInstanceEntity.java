package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
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
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hornetq.core.remoting.impl.invm.InVMConnector.log;

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

    private String illumina454KitUsed;

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

    private Integer readLength;

    private Date submitDate;

    private String labName;

    private String readType;

    private String reference;

    private Integer referenceVersion;

    private String fragmentSize;

    private String isPhix;

    private BigDecimal phixPercentage;

    private Integer readLength2;

    private Integer indexLength;

    private Integer indexLength2;

    private String comments;

    private String enzyme;

    private String fragSizeRange;

    private String status;

    private String flowcellLaneDesignated;

    private String flowcellDesignation;

    private String libraryConstructionMethod;

    private String quantificationMethod;

    private String concentrationUnit;

    private Integer laneQuantity;

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
       for(SampleInstanceEntityTsk task : sampleInstanceEntityTsks) {
           subTask.add(task.getSubTask());
       }

       return subTask;
    }

    public void setReadLength(Integer readLength) { this.readLength = readLength;  }

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

    public void setRestrictionEnzyme(String restrictionEnzyme) {this.restrictionEnzyme = restrictionEnzyme;}

    public void setJumpSize(String jumpSize) {this.jumpSize = jumpSize; }

    public void setLibrarySizeRange(String librarySizeRange) { this.librarySizeRange = librarySizeRange;}

    public void setInsertSizeRange(String insertSizeRange) { this.insertSizeRange = insertSizeRange;}

    public String getPooled() { return this.pooled; }

    public void setPooled(String pooled) { this.pooled = pooled; }

    public String getLibraryType() { return  this.libraryType; }

    public void setLibraryType(String libraryType) { this.libraryType = libraryType; }

    public String getCollaboratorSampleId() { return this.collaboratorSampleId; }

    public void setCollaboratorSampleId(String collaboratorSampleId) { this.collaboratorSampleId = collaboratorSampleId;}

    public void setTissueType(String tissueType) { this.tissueType = tissueType;}

    public void setDesiredReadLength(Integer readLength) { this.readLength = readLength;}

    public void setSampleKitRequest(SampleKitRequest sampleKitRequest) { this.sampleKitRequest = sampleKitRequest; }

    public ResearchProject getResearchProject() { return researchProject;  }

    public void setResearchProject(ResearchProject researchProject) { this.researchProject = researchProject; }

    public ProductOrder getProductOrder() { return productOrder; }

    public void setProductOrder(ProductOrder productOrder) { this.productOrder = productOrder; }

    public void setMolecularIndexingScheme(MolecularIndexingScheme molecularIndexingScheme) {
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public String getRestrictionEnzyme() {
        return restrictionEnzyme;
    }

    public String getIllumina454KitUsed() {
        return illumina454KitUsed;
    }

    public void setIllumina454KitUsed(String illumina454KitUsed) {
        this.illumina454KitUsed = illumina454KitUsed;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getLibrarySizeRange() {
        return librarySizeRange;
    }

    public String getJumpSize() {
        return jumpSize;
    }

    public String getInsertSizeRange() {
        return insertSizeRange;
    }

    public String getTissueType() {
        return tissueType;
    }

    public String getSampleTubeBarcode() {
        return sampleTubeBarcode;
    }

    public void setSampleTubeBarcode(String sampleTubeBarcode) {
        this.sampleTubeBarcode = sampleTubeBarcode;
    }

    public String getSampleNumber() {
        return sampleNumber;
    }

    public void setSampleNumber(String sampleNumber) {
        this.sampleNumber = sampleNumber;
    }

    public Integer getReadLength() {
        return readLength;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        try {
            this.submitDate = DateUtils.convertStringToDateTime(submitDate);
        } catch (ParseException e) {
            try {
                this.submitDate = DateUtils.convertStringToDateTime(StringUtils.substringBefore(submitDate, " "));
            } catch (ParseException e1) {
                log.error("Cannot convert '" + submitDate + "' to date.");
                this.submitDate = new Date();
            }
        }
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }

    public String getReadType() {
        return readType;
    }

    public void setReadType(String readType) {
        this.readType = readType;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(Integer referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

    public String getFragmentSize() {
        return fragmentSize;
    }

    public void setFragmentSize(String fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    public String getIsPhix() {
        return isPhix;
    }

    public void setIsPhix(String isPhix) {
        this.isPhix = isPhix;
    }

    public BigDecimal getPhixPercentage() {
        return phixPercentage;
    }

    public void setPhixPercentage(BigDecimal phixPercentage) {
        this.phixPercentage = phixPercentage;
    }

    public Integer getReadLength2() {
        return readLength2;
    }

    public void setReadLength2(Integer readLength2) {
        this.readLength2 = readLength2;
    }

    public Integer getIndexLength() {
        return indexLength;
    }

    public void setIndexLength(Integer indexLength) {
        this.indexLength = indexLength;
    }

    public Integer getIndexLength2() {
        return indexLength2;
    }

    public void setIndexLength2(Integer indexLength2) {
        this.indexLength2 = indexLength2;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getEnzyme() {
        return enzyme;
    }

    public void setEnzyme(String enzyme) {
        this.enzyme = enzyme;
    }

    public String getFragSizeRange() {
        return fragSizeRange;
    }

    public void setFragSizeRange(String fragSizeRange) {
        this.fragSizeRange = fragSizeRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFlowcellLaneDesignated() {
        return flowcellLaneDesignated;
    }

    public void setFlowcellLaneDesignated(String flowcellLaneDesignated) {
        this.flowcellLaneDesignated = flowcellLaneDesignated;
    }

    public String getFlowcellDesignation() {
        return flowcellDesignation;
    }

    public void setFlowcellDesignation(String flowcellDesignation) {
        this.flowcellDesignation = flowcellDesignation;
    }

    public String getLibraryConstructionMethod() {
        return libraryConstructionMethod;
    }

    public void setLibraryConstructionMethod(String libraryConstructionMethod) {
        this.libraryConstructionMethod = libraryConstructionMethod;
    }

    public String getQuantificationMethod() {
        return this.quantificationMethod;
    }

    public void setQuantificationMethod(String quantificationMethod) {
        this.quantificationMethod = quantificationMethod;
    }

    public Integer getLaneQuantity() {
        return laneQuantity;
    }

    public void setLaneQuantity(Integer laneQuantity) {
        this.laneQuantity = laneQuantity;
    }

    public String getConcentrationUnit() {
        return concentrationUnit;
    }

    public void setConcentrationUnit(String concentrationUnit) {
        this.concentrationUnit = concentrationUnit;
    }
}
