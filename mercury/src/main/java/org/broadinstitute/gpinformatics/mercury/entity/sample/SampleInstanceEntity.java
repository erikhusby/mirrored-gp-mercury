package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a pre-processed sample and its vessel that is pushed into Mercury from an
 * outside source. The sample may or may not have been processed by BSP, and has not
 * been accessioned by Mercury. Enough info is provided so that this entity can be used
 * as a starting point of a chain of custody, so it functions as a SampleInstance.
 */
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

    @ManyToOne
    private ReferenceSequence referenceSequence;

    @Column(unique = true)
    private String sampleLibraryName;

    private Boolean pooled;
    private String libraryType;
    private String experiment;
    private Integer readLength;
    private Date uploadDate;
    private Boolean pairedEndRead;
    private String comments;
    private Integer numberLanes;
    private IlluminaFlowcell.FlowcellType sequencerModel;

    public void removeSubTasks() {
        sampleInstanceEntityTsks.clear();
    }

    public void addSubTasks(SampleInstanceEntityTsk subTasks) {
        sampleInstanceEntityTsks.add(subTasks);
        subTasks.setSampleInstanceEntity(this);
    }

    /** Returns the Jira dev sub tasks in the order they were created. */
    public List<String> getSubTasks() {
        List<SampleInstanceEntityTsk> list = new ArrayList<>(sampleInstanceEntityTsks);
        Collections.sort(list, new Comparator<SampleInstanceEntityTsk>() {
            @Override
            public int compare(SampleInstanceEntityTsk o1, SampleInstanceEntityTsk o2) {
                int order = o1.getOrderOfCreation() - o2.getOrderOfCreation();
                return (order == 0) ? o1.getSubTask().compareTo(o2.getSubTask()) : order;
            }
        });
        List<String> subTaskNames = new ArrayList<>();
        for (SampleInstanceEntityTsk subTask : list) {
            subTaskNames.add(subTask.getSubTask());
        }
        return subTaskNames;
    }

    public MercurySample getRootSample() {
        return rootSample;
    }

    public void setRootSample(MercurySample rootSample) {
        this.rootSample = rootSample;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }

    public void setMolecularIndexingScheme(MolecularIndexingScheme molecularIndexingScheme) {
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public ReagentDesign getReagentDesign() {
        return this.reagentDesign;
    }

    public void setReagentDesign(ReagentDesign reagentDesign) {
        this.reagentDesign = reagentDesign;
    }

    public void setMolecularIndexScheme(MolecularIndexingScheme molecularIndexingScheme) {
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public void setMercurySample(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }

    public MercurySample getMercurySample() {
        return this.mercurySample;
    }

    public void setSampleLibraryName(String sampleLibraryName) {
        this.sampleLibraryName = sampleLibraryName;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public ReferenceSequence getReferenceSequence() {
        return referenceSequence;
    }

    public void setReferenceSequence(ReferenceSequence referenceSequence) {
        this.referenceSequence = referenceSequence;
    }

    public Boolean getPooled() {
        return pooled;
    }

    public void setPooled(Boolean pooled) {
        this.pooled = pooled;
    }

    public String getLibraryType() {
        return libraryType;
    }

    public void setLibraryType(String libraryType) {
        this.libraryType = libraryType;
    }

    public void setSampleKitRequest(SampleKitRequest sampleKitRequest) {
        this.sampleKitRequest = sampleKitRequest;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        this.researchProject = researchProject;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public Integer getReadLength() {
        return readLength;
    }

    public void setReadLength(Integer readLength) {
        this.readLength = readLength;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        try {
            this.uploadDate = DateUtils.convertStringToDateTime(uploadDate);
        } catch (Exception e) {
            try {
                this.uploadDate = DateUtils.convertStringToDateTime(StringUtils.substringBefore(uploadDate, " "));
            } catch (Exception e1) {
                this.uploadDate = new Date();
            }
        }
    }
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Nonnull
    public LabVessel getLabVessel() {
        return labVessel;
    }

    public SampleKitRequest getSampleKitRequest() {
        return sampleKitRequest;
    }

    public String getSampleLibraryName() {
        return sampleLibraryName;
    }

    public Set<SampleInstanceEntityTsk> getSampleInstanceEntityTsks() {
        return sampleInstanceEntityTsks;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Boolean getPairedEndRead() {
        return pairedEndRead;
    }

    public void setPairedEndRead(Boolean pairedEndRead) {
        this.pairedEndRead = pairedEndRead;
    }

    public int getNumberLanes() {
        return numberLanes == null ? 1 : numberLanes;
    }

    public void setNumberLanes(Integer numberLanes) {
        this.numberLanes = numberLanes;
    }

    public IlluminaFlowcell.FlowcellType getSequencerModel() {
        return sequencerModel;
    }

    public void setSequencerModel(IlluminaFlowcell.FlowcellType sequencerModel) {
        this.sequencerModel = sequencerModel;
    }
}
