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
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

public class AggregationTestFactory {
    @SuppressWarnings("EmptyCatchBlock")
    public static Aggregation buildAggregation(String project, String sample, Double contamination,
                                               LevelOfDetection fingerprintLod, String dataType,
                                               Double pctTargetBases20X, Integer totalReadsAlignedInPairs,
                                               Double meanCoverageWgs) {
        Aggregation aggregation = new Aggregation();
        aggregation.setSample(sample);
        aggregation.setProject(project);
        aggregation.setDataType(dataType);
        AggregationContam aggregationContam = new AggregationContam();
        AggregationHybridSelection aggregationHybridSelection = new AggregationHybridSelection(pctTargetBases20X);
        aggregation.setAggregationHybridSelection(aggregationHybridSelection);
        AggregationAlignment aggregationAlignment = new AggregationAlignment(totalReadsAlignedInPairs);
        aggregation.setAggregationAlignments(Arrays.asList(aggregationAlignment));
        aggregationContam.setPctContamination(contamination);
        aggregation.setAggregationContam(aggregationContam);
        aggregation.setReadGroupCount(2);
        AggregationWgs aggregationWgs=new AggregationWgs(meanCoverageWgs);
        aggregation.setAggregationWgs(aggregationWgs);
        Date createdDate;

        try {
            createdDate = DateUtils.parseDate("01/01/2014");
            aggregation.setWorkflowEndDate(createdDate);
        } catch (ParseException e) {

        }
        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup();

        PicardFingerprint picardFingerprint1 = new PicardFingerprint();
        picardFingerprint1.setLodExpectedSample(fingerprintLod.getMax());
        PicardAnalysis picardAnalysis1 = new PicardAnalysis();
        picardAnalysis1.setPicardFingerprint(picardFingerprint1);
        picardAnalysis1.setLane("1");

        aggregationReadGroup.getPicardAnalysis().add(picardAnalysis1);


        PicardFingerprint picardFingerprint2 = new PicardFingerprint();
        picardFingerprint2.setLodExpectedSample(fingerprintLod.getMin());
        PicardAnalysis picardAnalysis2 = new PicardAnalysis();
        picardAnalysis2.setPicardFingerprint(picardFingerprint2);
        picardAnalysis2.setLane("2");
        aggregationReadGroup.getPicardAnalysis().add(picardAnalysis2);
        aggregationReadGroup.setLane(2);
        aggregation.setAggregationReadGroups(Arrays.asList(aggregationReadGroup));
        return aggregation;
    }

    public static Aggregation buildAggregation(String dataType, Double pctTargetBases20X,
                                               Integer totalReadsAlignedInPairs,
                                               Double meanCoverageWgs) {
        return buildAggregation(null, null, null, new LevelOfDetection(1.2, 2.2), dataType, pctTargetBases20X,
                totalReadsAlignedInPairs, meanCoverageWgs);
    }
}
