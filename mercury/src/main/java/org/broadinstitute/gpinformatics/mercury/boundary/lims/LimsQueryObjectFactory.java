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

package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LaneReadStructure;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Class is used to create objects generated from LimsQueries.xsd.
 * The maven jaxb2 xjc generator does not create constructors, so here we do it manually.
 */
public class LimsQueryObjectFactory {
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
    public static SequencingTemplateType createSequencingTemplate(@Nullable String name,
                                                                  @Nullable String barcode,
                                                                  @Nonnull  Boolean pairedRun,
                                                                  @Nullable String onRigWorkflow,
                                                                  @Nullable String onRigChemistry,
                                                                  @Nonnull  String readStructure,
                                                                  SequencingTemplateLaneType... lanes) {
        SequencingTemplateType template = new SequencingTemplateType();
        template.setName(name);
        template.setBarcode(barcode);
        template.setPairedRun(pairedRun);
        template.setOnRigChemistry(onRigChemistry);
        template.setOnRigWorkflow(onRigWorkflow);
        template.setReadStructure(readStructure);
        template.getLanes().addAll(Arrays.asList(lanes));
        return template;
    }


    /**
     * @param laneName             the lane name.
     * @param loadingConcentration loading concentration.
     * @param loadingVesselLabel   barcode/label of the tube that should be used to load the lane.
     * @param startingVesselLabel
     */
    public static SequencingTemplateLaneType createSequencingTemplateLaneType(@Nonnull String laneName,
                                                                              @Nullable BigDecimal loadingConcentration,
                                                                              @Nonnull String loadingVesselLabel,
                                                                              String startingVesselLabel) {
        SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
        lane.setLaneName(laneName);
        lane.setLoadingConcentration(loadingConcentration);
        lane.setLoadingVesselLabel(loadingVesselLabel);
        lane.setDerivedVesselLabel(startingVesselLabel);
        return lane;
    }

    /**
     * Create a ReadStructureRequest object based on available parameters
     *
     * @return a newly created ReadStructureRequest.
     */
    public static ReadStructureRequest createReadStructureRequest(String runName, String runBarcode,
                                                                  String setupReadStructure, String actualReadStructure,
                                                                  Double imagedArea, String lanesSequenced,
                                                                  String error) {
        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunName(runName);
        readStructureRequest.setRunBarcode(runBarcode);
        readStructureRequest.setSetupReadStructure(setupReadStructure);
        readStructureRequest.setActualReadStructure(actualReadStructure);
        readStructureRequest.setImagedArea(imagedArea);
        readStructureRequest.setLanesSequenced(lanesSequenced);
        readStructureRequest.setError(error);
        return readStructureRequest;
    }

    /**
     * Create a ReadStructureRequest given a SequencingRun.
     *
     * @param sequencingRun a non-null SequencingRun.
     *
     * @return a newly created ReadStructureRequest.
     */
    public static ReadStructureRequest createReadStructureRequest(@Nonnull IlluminaSequencingRun sequencingRun) {
        ReadStructureRequest readStructureRequest = createReadStructureRequest(sequencingRun.getRunName(),
                sequencingRun.getRunBarcode(), sequencingRun.getSetupReadStructure(),
                sequencingRun.getActualReadStructure(), sequencingRun.getImagedAreaPerMM2(),
                sequencingRun.getLanesSequenced(), null);
        for (IlluminaSequencingRunChamber sequencingRunChamber : sequencingRun.getSequencingRunChambers()) {
            LaneReadStructure laneReadStructure = new LaneReadStructure();
            laneReadStructure.setLaneNumber(sequencingRunChamber.getLaneNumber());
            laneReadStructure.setActualReadStructure(sequencingRunChamber.getActualReadStructure());
            readStructureRequest.getLaneStructures().add(laneReadStructure);
        }

        return readStructureRequest;
    }
}
