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

import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SequencingTemplateLane extends SequencingTemplateLaneType {
    public SequencingTemplateLane() {
    }

    /**
     * @param laneName             the lane name.
     * @param loadingConcentration loading concentration.
     * @param loadingVesselLabel   barcode/label of the tube that should be used to load the lane.
     */
    public SequencingTemplateLane(@Nonnull String laneName, @Nullable Double loadingConcentration,
                                  @Nonnull String loadingVesselLabel) {
        this.laneName = laneName;
        this.loadingConcentration = loadingConcentration;
        this.loadingVesselLabel = loadingVesselLabel;
    }
}
