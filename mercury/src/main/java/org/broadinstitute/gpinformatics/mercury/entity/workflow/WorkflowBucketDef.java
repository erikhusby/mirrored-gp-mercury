package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Where samples are placed for batching, typically the first step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
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

    /** For JAXB */
    WorkflowBucketDef() {
    }

    public WorkflowBucketDef(String name) {
        super(name);
    }

    public MaterialType getEntryMaterialType() {
        return entryMaterialType;
    }

    public void setEntryMaterialType(MaterialType entryMaterialType) {
        this.entryMaterialType = entryMaterialType;
    }

    public Double getEntryLowVolume() {
        return entryLowVolume;
    }

    public void setEntryLowVolume(Double entryLowVolume) {
        this.entryLowVolume = entryLowVolume;
    }

    public Double getEntryHighVolume() {
        return entryHighVolume;
    }

    public void setEntryHighVolume(Double entryHighVolume) {
        this.entryHighVolume = entryHighVolume;
    }

    public Double getEntryLowConcentration() {
        return entryLowConcentration;
    }

    public void setEntryLowConcentration(Double entryLowConcentration) {
        this.entryLowConcentration = entryLowConcentration;
    }

    public Double getEntryHighConcentration() {
        return entryHighConcentration;
    }

    public void setEntryHighConcentration(Double entryHighConcentration) {
        this.entryHighConcentration = entryHighConcentration;
    }

    public Double getAutoDrainDays() {
        return autoDrainDays;
    }

    public void setAutoDrainDays(Double autoDrainDays) {
        this.autoDrainDays = autoDrainDays;
    }
}
