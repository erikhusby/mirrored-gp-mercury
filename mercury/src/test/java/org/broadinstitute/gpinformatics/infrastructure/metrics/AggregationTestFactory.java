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
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.ReadGroupIndex;

import java.util.Collections;

public class AggregationTestFactory {
    public static Aggregation buildAggregation(String project, String productOrder, String sample,
                                               int version, Double contamination,
                                               LevelOfDetection fingerprintLod, String dataType,
                                               Double pctTargetBases20X, Long totalReadsAlignedInPairs,
                                               Double meanCoverageWgs, String processingLocation) {

        AggregationContam aggregationContam = new AggregationContam(contamination);
        AggregationHybridSelection aggregationHybridSelection = new AggregationHybridSelection(pctTargetBases20X);
        AggregationAlignment aggregationAlignment = new AggregationAlignment(totalReadsAlignedInPairs, "PAIR");
        AggregationWgs aggregationWgs=new AggregationWgs(meanCoverageWgs);

        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup(null, 2, null,
            new ReadGroupIndex(null, null, 2, null, null, null, project, project, sample, productOrder));

        Integer readGroupCount = 2;

        return new Aggregation(project, sample, null, version, readGroupCount, dataType,
                Collections.singleton(aggregationAlignment), aggregationContam, aggregationHybridSelection,
                Collections.singleton(aggregationReadGroup), aggregationWgs, fingerprintLod, processingLocation);
    }
}
