/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.limsquery;

import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Class is used to create objects generated from LimsQueries.xsd.
 * The maven jaxb2 xjc generator does not create constructors, so here we do it manually.
 */
public class SequencingTemplate extends SequencingTemplateType {
    public SequencingTemplate() {
    }

    /**
     * This structure defines run setup data for integrating mercury with the unified loader app.
     * It is roughly equivalent to a designation, in Squid.
     *
     * @param name           an optional name, which may or may not be an FCT ticket name
     * @param barcode        an optional flowcell barcode, depending on whether mercury knows the flowcell
     * @param pairedRun      whether or not the run is paired
     * @param onRigWorkflow  the on-rig workflow ("Resequencing")
     * @param onRigChemistry the on-rig chemistry ("Default")
     * @param readStructure  "read structure" object, which is a textual encoding of templates, barcodes, or skips.
     *                       For example:
     *                       a 76-base paired-end read with one molecular barcode index: "76T8B76T"
     *                       a 101-base paired-end read with dual indices: "101T8B8B101T"
     */
    public SequencingTemplate(@Nullable String name, @Nullable String barcode, @Nonnull Boolean pairedRun,
                              @Nonnull String onRigWorkflow, @Nonnull String onRigChemistry,
                              @Nonnull String readStructure) {
        this.name = name;
        this.barcode = barcode;
        this.pairedRun = pairedRun;
        this.onRigWorkflow = onRigWorkflow;
        this.onRigChemistry = onRigChemistry;
        this.readStructure = readStructure;
    }

    /**
     * This structure defines run setup data for integrating mercury with the unified loader app.
     * It is roughly equivalent to a designation, in Squid.
     *
     * @param name           an optional name, which may or may not be an FCT ticket name
     * @param barcode        an optional flowcell barcode, depending on whether mercury knows the flowcell
     * @param pairedRun      whether or not the run is paired
     * @param onRigWorkflow  the on-rig workflow ("Resequencing")
     * @param onRigChemistry the on-rig chemistry ("Default")
     * @param readStructure  "read structure" object, which is a textual encoding of templates, barcodes, or skips.
     *                       For example:
     *                       a 76-base paired-end read with one molecular barcode index: "76T8B76T"
     *                       a 101-base paired-end read with dual indices: "101T8B8B101T"
     * @param lanes          SequencingTemplateLanes
     */
    public SequencingTemplate(@Nullable String name, @Nullable String barcode, @Nonnull Boolean pairedRun,
                              @Nonnull String onRigWorkflow, @Nonnull String onRigChemistry,
                              @Nonnull String readStructure, @Nonnull List<SequencingTemplateLane> lanes) {
        this(name, barcode, pairedRun, onRigWorkflow, onRigChemistry, readStructure);
        getLanes().clear();
        for (SequencingTemplateLane lane : lanes) {
            SequencingTemplateLane newLane = new SequencingTemplateLane(lane.getLaneName(), lane.getLoadingConcentration(),
                    lane.getLoadingVesselLabel());
            getLanes().add(newLane);
        }
    }
}
