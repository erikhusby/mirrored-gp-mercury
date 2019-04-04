/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum MaterialType implements Displayable {
    // These MaterialTypes already exist in the database.
    CELL_SUSPENSION("Cell Suspension", true),
    FRESH_BLOOD("Fresh Blood", true),
    BUFFY_COAT("Buffy Coat", true),
    DNA("DNA", true),
    RNA("RNA", true),
    FFPE("FFPE", true),
    DNA_GENOMIC("DNA:Genomic", true),
    FRESH_FROZEN_TISSUE("Fresh Frozen Tissue", true),
    FRESH_FROZEN_BLOOD("Fresh Frozen Blood", true),

    // These materialTypes match those in BSP
    BACTERIAL_CULTURE_GLYCEROL_FROZEN("Bacterial Culture:Glycerol Frozen"),
    BODILY_FLUID_ASCITES("Bodily Fluid:Ascites"),
    BODILY_FLUID_PLEURAL_EFFUSION("Bodily Fluid:Pleural Effusion"),
    BODILY_FLUID_SALIVA("Bodily Fluid:Saliva"),
    BODILY_FLUID_URINE("Bodily Fluid:Urine"),
    BODILY_FLUID_CEREBROSPINAL_FLUID("Bodily Fluid:Cerebrospinal Fluid"),
    CELLS_ALLPROTECT_REAGENT_PRESERVED("Cells:Allprotect Reagent Preserved"),
    CELLS_CELL_LINE_VIABLE("Cells:Cell Line, Viable"),
    CELLS_CELL_PELLET_FROZEN_LESIONAL_CELLS("Cells:Cell Pellet Frozen - Lesional Cells"),
    CELLS_GROWING("Cells:Growing"),
    CELLS_PELLET_FROZEN_IN_GLYCEROLYTE("Cells:Pellet frozen in glycerolyte"),
    CELLS_PELLET_FROZEN_IN_X_MEDIA("Cells: Pellet Frozen in X media"),
    CELLS_PELLET_FROZEN_LIPID_EXTRACTS("Cells:Pellet frozen, lipid extracts"),
    CELLS_PELLET_FROZEN_POLAR_EXTRACTS("Cells:Pellet frozen, polar extracts"),
    CELLS_PELLET_FROZEN_TOTAL_PROTEIN("Cells:Pellet frozen, total protein"),
    CELLS_PELLET_FROZEN("Cells:Pellet frozen"),
    CELLS_RED_BLOOD_CELLS("Cells:Red Blood Cells"),
    CELLS_RNALATER_REAGENT_PRESERVED("Cells:RNAlater Reagent Preserved"),
    DNA_CDNA_LIBRARY("DNA:cDNA Library"),
    DNA_CDNA("DNA:cDNA"),
    DNA_DNA_CELL_FREE("DNA:DNA Cell Free"),
    DNA_DNA_GENOMIC("DNA:DNA Genomic"),
    DNA_DNA_LIBRARY_EXTERNAL("DNA:DNA Library External"),
    DNA_DNA_POOLED("DNA:DNA Pooled"),
    DNA_DNA_POST_RNAI_INSERTION("DNA:DNA post RNAi insertion"),
    DNA_DNA_SOMATIC("DNA:DNA Somatic"),
    DNA_DNA_WGA_CLEANED("DNA:DNA WGA Cleaned"),
    DNA_DNA_WGA_FROM_WGA_CLEANED("DNA:DNA WGA from WGA Cleaned"),
    DNA_DNA_WGA_FROM_WGA("DNA:DNA WGA from WGA"),
    DNA_DNA_WGA_QIAGEN("DNA:DNA WGA Qiagen"),
    DNA_DNA_WGA_SIGMA("DNA:DNA WGA Sigma"),
    DNA_DS_VIRAL_CDNA("DNA:ds Viral cDNA"),
    DNA_DS_VIRAL_DNA_PCR_AMPLICON("DNA:ds Viral DNA PCR Amplicon"),
    DNA_DS_VIRAL_DNA("DNA:ds Viral DNA"),
    DNA_OLIGONUCLEOTIDE("DNA:Oligonucleotide"),
    DNA_PLASMID("DNA:Plasmid"),
    DNA_SS_VIRAL_CDNA("DNA:ss Viral cDNA"),
    DNA_SS_VIRAL_DNA("DNA:ss Viral DNA"),
    DNA_VIRAL_HYBRID("DNA:Viral Hybrid"),
    EMPTY_EMPTY("Empty:Empty"),
    FFPE_BLOCK("FFPE Block"),
    FFPE_SCROLL("FFPE Scroll"),
    OTHER_AGAROSE_PLUG("Other:Agarose Plug"),
    OTHER_BONE_MARROW("Other:Bone Marrow"),
    OTHER_BUCCAL_SWAB("Other:Buccal Swab"),
    OTHER_CELL_SUPERNATANT("Other:Cell Supernatant"),
    OTHER_LYSATE("Other:Lysate"),
    OTHER_MOUTHWASH("Other:Mouthwash"),
    OTHER_PLAQUE("Other:Plaque"),
    OTHER_POLYA_RNA("Other:PolyA+ RNA"),
    OTHER_TRIZOL_LYSATE("Other:Trizol Lysate"),
    OTHER_WATER_CONTROL("Other:Water Control"),
    PEPTIDES_PEPTIDES("Peptides:Peptides"),
    PLASMA_PLASMA("Plasma:Plasma"),
    PLASMODIUM_CULTURE_GLYCEROL_FROZEN("Plasmodium Culture:Glycerol Frozen"),
    REAGENT_REAGENT("Reagent: Reagent"),
    RNA_DS_VIRAL_RNA("RNA:ds Viral RNA"),
    RNA_MESSENGER_RNA("RNA:Messenger RNA"),
    RNA_SS_VIRAL_RNA("RNA:ss Viral RNA"),
    RNA_TOTAL_RNA("RNA:Total RNA"),
    SERUM_FROM_CLOT_TUBE("Serum:From Clot Tube"),
    SERUM_FROM_SST("Serum:From SST"),
    SLIDE_FROZEN("Slide:Frozen"),
    SLIDE_H_AND_E("Slide:H and E"),
    SLIDE_PARAFFIN("Slide:Paraffin"),
    STOOL_STOOL("Stool:Stool"),
    TISSUE_ALLPROTECT_REAGENT_PRESERVED("Tissue:Allprotect Reagent Preserved"),
    TISSUE_DMSO_CRYOPRESERVED_TISSUE("Tissue:DMSO Cryopreserved Tissue"),
    TISSUE_FFPE_EMBEDDED_BLOCK("Tissue:FFPE Embedded Block"),
    TISSUE_FFPE_TISSUE_SECTION("Tissue:FFPE Tissue Section"),
    TISSUE_FIXED_TISSUE("Tissue:Fixed Tissue"),
    TISSUE_FRESH_FROZEN_TISSUE("Tissue:Fresh Frozen Tissue"),
    TISSUE_FRESH_SKIN_PUNCH("Tissue:Fresh Skin Punch"),
    TISSUE_FRESH_TISSUE("Tissue:Fresh Tissue"),
    TISSUE_FROZEN_TISSUE_SECTION("Tissue:Frozen Tissue Section"),
    TISSUE_MOUSE_TAIL("Tissue:Mouse Tail"),
    TISSUE_OCT_EMBEDDED_TISSUE("Tissue:OCT Embedded Tissue"),
    TISSUE_PAXGENE_PRESERVED_PARAFFIN_EMBEDDED("Tissue:PAXgene Preserved, Paraffin-embedded"),
    TISSUE_PAXGENE_PRESERVED("Tissue:PAXgene Preserved"),
    TISSUE_RNALATER_REAGENT_PRESERVED("Tissue:RNAlater Reagent Preserved"),
    TISSUE_TISSUE_LYSATE_HOMOGENATE("Tissue:Tissue Lysate/Homogenate"),
    TNA_TOTAL_NUCLEIC_ACID("TNA:Total Nucleic Acid"),
    UNSPECIFIED_UNSPECIFIED("Unspecified:Unspecified"),
    WHOLE_BLOOD_BLOOD_CELLS("Whole Blood:Blood Cells"),
    WHOLE_BLOOD_BLOOD_SPOT("Whole Blood:Blood Spot"),
    WHOLE_BLOOD_BUFFY_COAT("Whole Blood:Buffy Coat"),
    WHOLE_BLOOD_FLOW_SORTED("Whole Blood:Flow-sorted"),
    WHOLE_BLOOD_LYMPHOCYTE("Whole Blood:Lymphocyte"),
    WHOLE_BLOOD_MONONUCLEAR_CELLS("Whole Blood:Mononuclear cells"),
    WHOLE_BLOOD_PAXGENE_PRESERVED("Whole Blood:PAXgene Preserved"),
    WHOLE_BLOOD_PBMC("Whole Blood: PBMC"),
    WHOLE_BLOOD_PLASMODIPURE_FILTERED("Whole Blood:Plasmodipure Filtered"),
    WHOLE_BLOOD_WHOLE_BLOOD_FRESH("Whole Blood:Whole Blood Fresh"),
    WHOLE_BLOOD_WHOLE_BLOOD_FROZEN("Whole Blood:Whole Blood Frozen"),
    WHOLE_BLOOD_WHOLE_BLOOD("Whole Blood:Whole Blood"),
    WHOLE_ORGANISM_MOSQUITO_SUPERNATANT("Whole Organism:Mosquito Supernatant"),
    WHOLE_ORGANISM_MULTIPLE_ETOH("Whole Organism:Multiple, ETOH"),
    WHOLE_ORGANISM_MULTIPLE_FROZEN("Whole Organism:Multiple, Frozen"),
    WHOLE_ORGANISM_OTHER("Whole Organism: Other"),
    WHOLE_ORGANISM_RNALATER_PRESERVED("Whole Organism:RNAlater Preserved"),
    WHOLE_ORGANISM_SINGLE_ETOH("Whole Organism:Single, ETOH"),
    WHOLE_ORGANISM_SINGLE_FROZEN("Whole Organism:Single, Frozen"),
    NONE("");

    private final String displayName;
    private final boolean mercuryMaterial;

    MaterialType(String displayName) {
        this(displayName, false);
    }

    MaterialType(String displayName, boolean mercuryMaterial) {
        this.displayName = displayName;
        this.mercuryMaterial = mercuryMaterial;
    }

    private static final List<MaterialType> BSP_MATERIAL_TYPES = Arrays.stream(MaterialType.values())
            .filter(m -> !m.mercuryMaterial)
            .collect(Collectors.toList());

    public static MaterialType fromDisplayName(String displayName) {
        if (StringUtils.isBlank(displayName)) {
            return NONE;
        }

        for (MaterialType materialType : values()) {
            if (materialType.displayName.equals(displayName)) {
                return materialType;
            }
        }
        throw new RuntimeException(String.format("Unknown MaterialType %s", displayName));
    }

    public static boolean isValid(String displayName) {
        boolean isValid=false;
        MaterialType materialType = null;
        try {
            materialType = fromDisplayName(displayName);
            if (materialType != NONE) {
                isValid=true;
            }
        } catch (Exception e) {
            isValid=false;
        }
        return isValid;
    }

    public static Stream<MaterialType> stream() {
        return Arrays.stream(MaterialType.values());
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public static Set<String> displayNamesOf(Collection<MaterialType> materialTypes) {
        Set<String> displayNames = new HashSet<>();
        for (MaterialType materialType : materialTypes) {
            displayNames.add(materialType.getDisplayName());
        }
        return displayNames;
    }

    public boolean containsIgnoringCase(String text) {
        return this.getDisplayName().toUpperCase().contains(text.toUpperCase());
    }

    public boolean isMercuryMaterial() {
        return mercuryMaterial;
    }

    public static List<MaterialType> getBspMaterialTypes() {
        return BSP_MATERIAL_TYPES;
    }
}
