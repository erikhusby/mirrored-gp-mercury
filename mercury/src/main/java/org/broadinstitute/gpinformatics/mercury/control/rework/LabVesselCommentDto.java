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

package org.broadinstitute.gpinformatics.mercury.control.rework;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.LabVesselComment;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.LabVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.RapSheetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LabVesselCommentDto {

    private LabVesselComment labVesselComment;

    public LabVesselCommentDto(LabEvent labEvent, LabVessel labVessel,
                               RapSheetEntry rapSheetEntry,
                               Map<MercurySample, VesselPosition> sampleVesselPositionMap,
                               String comment) {

        List<RapSheet> rapSheets = new ArrayList<RapSheet>(sampleVesselPositionMap.size());
        for (MercurySample sample : sampleVesselPositionMap.keySet()) {
            final RapSheet rapSheet = new RapSheet(rapSheetEntry);
            final LabVesselPosition lpv = new LabVesselPosition(sampleVesselPositionMap.get(sample), sample);
            rapSheet.setLabVesselPosition(lpv);
            rapSheet.setSample(sample);
            rapSheet.addEntry(rapSheetEntry);
            rapSheets.add(rapSheet);
        }

        labVesselComment =
                new LabVesselComment(labEvent, labVessel, comment, rapSheets);
    }

    @DaoFree
    public LabVesselComment getLabVesselCommentDbFree() {
        return labVesselComment;
    }
}
