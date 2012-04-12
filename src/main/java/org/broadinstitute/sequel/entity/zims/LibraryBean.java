package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A library DTO for Zamboni.  Copied from
 * LIMQuery.thrift from squidThriftService.
 */
@XmlRootElement
public class LibraryBean {
    
    private String library;
    
    private String project;
    
    private String initiative;
    
    private Long workRequest;
    
    private MolecularIndexingScheme indexingScheme;
    
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

    private DevExperimentData devExperimentData;
    
    public LibraryBean(String project,
                       String organism,
                       Long squidWorkRequestId) {
        this.project = project;
        this.organism = organism;
        this.workRequest = squidWorkRequestId;
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
    
    
    
    
    
    
    
    
    
}
