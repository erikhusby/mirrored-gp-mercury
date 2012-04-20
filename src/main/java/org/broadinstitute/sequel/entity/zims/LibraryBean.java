package org.broadinstitute.sequel.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;

import javax.xml.bind.annotation.*;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */
@XmlRootElement(name = "Library")
@XmlAccessorType(XmlAccessType.FIELD)
public class LibraryBean {
    
    private String library;
    
    private String project;
    
    private String initiative;
        
    private Long workRequest;

    @XmlElement(name = "MolecularIndexingScheme")
    private MolecularIndexingSchemeBean indexingScheme;
    
    private Boolean hasIndexingRead;
    
    private String expectedInsertSize;
    
    private String analysisType;
    
    private String referenceSequence;
    
    private String referenceSequenceVersion;
    
    /** the name the collaborator has given to the sample */
    private String collaboratorSampleId;
    
    /** the name of the collaborator */
    private String collaborator;
    
    private String organism;
    
    private String species;
    
    private String strain;
    
    private String sampleLSID;
    
    private String tissueType;
    
    private String expectedPlasmid;
    
    private String aligner;
    
    private String rrbsSizeRange;
    
    private String restrictionEnzyme;
    
    private String cellLine;
    
    private String bait;

    /** obfuscated name of the individual (person) from whence this sample was taken */
    private String individual;
    
    private Double labMeasuredInsertSize;
    
    private Boolean isPositiveControl;
    
    private Boolean isNegativeControl;
    
    private String weirdness;
    
    private Double preCircularizationDnaSize;
    
    private Boolean isPartOfDevExperiment;

    @XmlElement(name = "DevExperimentData")
    private DevExperimentDataBean devExperimentData;

    public LibraryBean() {}

    public LibraryBean(String library, String project, String initiative, Long workRequest, MolecularIndexingScheme indexingScheme, Boolean hasIndexingRead, String expectedInsertSize, String analysisType, String referenceSequence, String referenceSequenceVersion, String collaboratorSampleId, String collaborator, String organism, String species, String strain, String sampleLSID, String tissueType, String expectedPlasmid, String aligner, String rrbsSizeRange, String restrictionEnzyme, String cellLine, String bait, String individual, Double labMeasuredInsertSize, Boolean positiveControl, Boolean negativeControl, String weirdness, Double preCircularizationDnaSize, Boolean partOfDevExperiment, TZDevExperimentData devExperimentData) {
        this.library = library;
        this.project = project;
        this.initiative = initiative;
        this.workRequest = workRequest;
        this.indexingScheme = new MolecularIndexingSchemeBean(indexingScheme);
        this.hasIndexingRead = hasIndexingRead;
        this.expectedInsertSize = expectedInsertSize;
        this.analysisType = analysisType;
        this.referenceSequence = referenceSequence;
        this.referenceSequenceVersion = referenceSequenceVersion;
        this.collaboratorSampleId = collaboratorSampleId;
        this.collaborator = collaborator;
        this.organism = organism;
        this.species = species;
        this.strain = strain;
        this.sampleLSID = sampleLSID;
        this.tissueType = tissueType;
        this.expectedPlasmid = expectedPlasmid;
        this.aligner = aligner;
        this.rrbsSizeRange = rrbsSizeRange;
        this.restrictionEnzyme = restrictionEnzyme;
        this.cellLine = cellLine;
        this.bait = bait;
        this.individual = individual;
        this.labMeasuredInsertSize = labMeasuredInsertSize;
        isPositiveControl = positiveControl;
        isNegativeControl = negativeControl;
        this.weirdness = weirdness;
        this.preCircularizationDnaSize = preCircularizationDnaSize;
        isPartOfDevExperiment = partOfDevExperiment;
        this.devExperimentData = new DevExperimentDataBean(devExperimentData);
    }
    
    public Long getWorkRequest() {
        return workRequest;
    }
    
    public String getProject() {
        return project;
    }
    
    public String getOrganism() {
        return organism;
    }
    
    public String getCollaboratorSampleName() {
        return collaboratorSampleId;
    }
    
    public String getSampleLSID() {
        return sampleLSID;
    }
    
    public String getCellLine() {
        return cellLine;
    }
    
    public String getIndividual() {
        return individual;
    }
    
    
    
    
    
    
    
    
    
}
