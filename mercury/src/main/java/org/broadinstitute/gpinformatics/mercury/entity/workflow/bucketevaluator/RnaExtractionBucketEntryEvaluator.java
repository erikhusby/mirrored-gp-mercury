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

package org.broadinstitute.gpinformatics.mercury.entity.workflow.bucketevaluator;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Collection;
import java.util.EnumSet;

public class RnaExtractionBucketEntryEvaluator extends ExtractionBucketEntryEvaluator {
    @Override
    protected Collection<Workflow> supportedWorkflows() {
        return EnumSet.of(Workflow.RNA_EXTRACTION);
    }

    @Override
    protected Collection<LabVessel.MaterialType> supportedMaterialTypes() {
        return EnumSet.of(LabVessel.MaterialType.CELL_SUSPENSION, LabVessel.MaterialType.FRESH_FROZEN_TISSUE,
                LabVessel.MaterialType.FFPE);

    }
}
