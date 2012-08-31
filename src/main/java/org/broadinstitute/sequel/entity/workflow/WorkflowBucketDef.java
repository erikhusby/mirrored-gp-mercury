package org.broadinstitute.sequel.entity.workflow;

/**
 * Where samples are placed for batching, typically the first step in a process
 */
public class WorkflowBucketDef extends WorkflowStepDef {

    public enum MaterialType {
//        BAC_FOSMID_LIBRARY("BAC/Fosmid Library"),
        GENOMIC_DNA("Genomic DNA"),
//        GLYCEROL_CLONE("Glycerol clone"),
//        PCR_PRODUCT("PCR Product"),
//        PRIMER("Primer"),
        TISSUE("Tissue"),
        VIRAL_C_DNA("Viral cDNA"),
//        VOUCHER("Voucher"),
        C_DNA_LIBRARY("cDNA Library"),
        SST_DNA_LIBRARY("sstDNA library"),
        NEW_TECH_LIBRARY("New Tech Library"),
        RNA("RNA");

        private final String name;

        MaterialType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // rules for entry - material type, vol / conc ranges
    private MaterialType entryMaterialType;
    private Double entryLowVolume;
    private Double entryHighVolume;
    private Double entryLowConcentration;
    private Double entryHighConcentration;

    // auto-drain rules - time / date based
    private Double autoDrainDays;

    public WorkflowBucketDef(String name) {
        super(name);
    }
}
