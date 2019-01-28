package org.broadinstitute.gpinformatics.mercury.entity.workflow;

/**
 * Workflow name constants for use in testing.  WorkflowConfig.xml is the definitive source for non-test code.
 */
public class Workflow {
    public static final String AGILENT_EXOME_EXPRESS = "Agilent Exome Express";
    public static final String ICE_EXOME_EXPRESS = "ICE Exome Express";
    public static final String HYBRID_SELECTION = "Hybrid Selection";
    public static final String WHOLE_GENOME = "Whole Genome";
    public static final String PCR_FREE = "Whole Genome PCR Free";
    public static final String PCR_PLUS = "Whole Genome PCR Plus";
    public static final String PCR_FREE_HYPER_PREP = "Whole Genome PCR Free HyperPrep";
    public static final String PCR_PLUS_HYPER_PREP = "Whole Genome PCR Plus HyperPrep";
    public static final String CELL_FREE_HYPER_PREP = "Cell Free HyperPrep";
    public static final String CELL_FREE_HYPER_PREP_UMIS = "Cell Free HyperPrep With UMIs";
    public static final String ICE_EXOME_EXPRESS_HYPER_PREP = "Hyper Prep ICE Exome Express";
    public static final String ICE_EXOME_EXPRESS_HYPER_PREP_UMIS = "Hyper Prep ICE Exome Express With UMIs";
    public static final String CUSTOM_SELECTION = "Custom Selection";
    public static final String ICE = "ICE";
    public static final String ICE_CRSP = "ICE CRSP";
    public static final String CLINICAL_WHOLE_BLOOD_EXTRACTION = "Clinical Whole Blood Extraction";
    public static final String DNA_RNA_EXTRACTION_CELL_PELLETS = "DNA and RNA from Cell Pellets";
//    public static final String DNA_RNA_EXTRACTION_CELL_PELLETS_BSP = "DNA and RNA from Cell Pellets BSP";
    public static final String TRU_SEQ_STRAND_SPECIFIC_CRSP = "TruSeq Strand Specific CRSP";
    public static final String TEN_X = "10X";
    public static final String INFINIUM = "Infinium";
    public static final String INFINIUM_METHYLATION = "Infinium Methylation";
//    public static final String MALARIA = "Malaria";
//    public static final String LASSO = "Lasso";
    public static final String DNA_RNA_EXTRACTION_STOOL = "DNA and RNA from Stool";
    public static final String SINGLE_CELL_SMART_SEQ = "Single Cell SmartSeq";
    public static final String SINGLE_CELL_10X = "Single Cell 10X";
    /** Use this to indicate that no workflow is associated. */
    public static final String NONE = null;
}
