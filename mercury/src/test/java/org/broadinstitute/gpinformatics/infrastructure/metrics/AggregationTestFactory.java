/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationAlignment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationContam;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationHybridSelection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationWgs;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;

import java.util.Arrays;

public class AggregationTestFactory {
    @SuppressWarnings("EmptyCatchBlock")
    public static Aggregation buildAggregation(String project, String sample, Double contamination,
                                               LevelOfDetection fingerprintLod, String dataType,
                                               Double pctTargetBases20X, Long totalReadsAlignedInPairs,
                                               Double meanCoverageWgs) {
        Aggregation aggregation = new Aggregation();
        aggregation.setLevelOfDetection(fingerprintLod);
        aggregation.setSample(sample);
        aggregation.setProject(project);
        aggregation.setDataType(dataType);
        AggregationContam aggregationContam = new AggregationContam();
        AggregationHybridSelection aggregationHybridSelection = new AggregationHybridSelection(pctTargetBases20X);
        aggregation.setAggregationHybridSelection(aggregationHybridSelection);
        AggregationAlignment aggregationAlignment = new AggregationAlignment(totalReadsAlignedInPairs, "PAIR");
        aggregation.setAggregationAlignments(Arrays.asList(aggregationAlignment));
        aggregationContam.setPctContamination(contamination);
        aggregation.setAggregationContam(aggregationContam);
        aggregation.setReadGroupCount(2);
        AggregationWgs aggregationWgs=new AggregationWgs(meanCoverageWgs);
        aggregation.setAggregationWgs(aggregationWgs);

        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup();
        aggregationReadGroup.setLane(2);
        aggregation.setAggregationReadGroups(Arrays.asList(aggregationReadGroup));
        return aggregation;
    }
}
