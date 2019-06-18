package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel labVessel;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "MERCURY_SAMPLE")
    private MercurySample mercurySample;

    @OneToMany(mappedBy = "sampleInstanceEntity", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SampleInstanceEntityTsk> sampleInstanceEntityTsks = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "REAGENT_DESIGN")
    private ReagentDesign reagentDesign;

    @ManyToOne
    @JoinColumn(name = "MOLECULAR_INDEXING_SCHEME")
    private MolecularIndexingScheme molecularIndexingScheme;

    @ManyToOne
    @JoinColumn(name = "ANALYSIS_TYPE")
    private AnalysisType analysisType;

    @ManyToOne
    @JoinColumn(name = "REFERENCE_SEQUENCE")
    private ReferenceSequence referenceSequence;

    private String libraryName;
    private String experiment;

    @Column(name = "READ_LENGTH1")
    private Integer readLength1;

    @Column(name = "READ_LENGTH2")
    private Integer readLength2;

    @Column(name = "INDEX_LENGTH1")
    private Integer indexLength1;

    @Column(name = "INDEX_LENGTH2")
    private Integer indexLength2;

    private Date uploadDate;
    private Boolean pairedEndRead;
    private Integer numberLanes;
    private String aggregationParticle;
    private String insertSize;
    private Boolean umisPresent;
    private String aggregationDataType;

    /**
     * Implied sample name means there was no explicit sample name provided in the upload and that the library
     * name is used instead for Mercury internal purposes, but not sent to the pipeline.
     */
    private Boolean impliedSampleName;
    private String baitName;

    @Enumerated(EnumType.STRING)
    private FlowcellDesignation.IndexType indexType;

    @Enumerated(EnumType.STRING)
    private IlluminaFlowcell.FlowcellType sequencerModel;

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

    public void setMercurySample(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }

    public MercurySample getMercurySample() {
        return this.mercurySample;
    }

    public Boolean getImpliedSampleName() {
        return impliedSampleName;
    }

    public void setImpliedSampleName(Boolean impliedSampleName) {
        this.impliedSampleName = impliedSampleName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
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


    public Integer getReadLength1() {
        return readLength1;
    }

    public void setReadLength1(Integer readLength1) {
        this.readLength1 = readLength1;
    }

    public Integer getReadLength2() {
        return readLength2;
    }

    public void setReadLength2(Integer readLength2) {
        this.readLength2 = readLength2;
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

    @Nonnull
    public LabVessel getLabVessel() {
        return labVessel;
    }

    @Nonnull
    public String getLibraryName() {
        return libraryName;
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

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    public String getAggregationParticle() {
        return aggregationParticle;
    }

    public void setAggregationParticle(String aggregationParticle) {
        this.aggregationParticle = aggregationParticle;
    }

    public String getInsertSize() {
        return insertSize;
    }

    public void setInsertSize(String insertSize) {
        this.insertSize = insertSize;
    }

    public Boolean getUmisPresent() {
        return umisPresent;
    }

    public void setUmisPresent(Boolean umisPresent) {
        this.umisPresent = umisPresent;
    }

    public String getAggregationDataType() {
        return aggregationDataType;
    }

    public void setAggregationDataType(String aggregationDataType) {
        this.aggregationDataType = aggregationDataType;
    }

    public FlowcellDesignation.IndexType getIndexType() {
        return indexType;
    }

    public void setIndexType(FlowcellDesignation.IndexType indexType) {
        this.indexType = indexType;
    }

    public Integer getIndexLength1() {
        return indexLength1;
    }

    public void setIndexLength1(Integer indexLength1) {
        this.indexLength1 = indexLength1;
    }

    public Integer getIndexLength2() {
        return indexLength2;
    }

    public void setIndexLength2(Integer indexLength2) {
        this.indexLength2 = indexLength2;
    }

    public String getBaitName() {
        return baitName;
    }

    public void setBaitName(String baitName) {
        this.baitName = baitName;
    }
}
