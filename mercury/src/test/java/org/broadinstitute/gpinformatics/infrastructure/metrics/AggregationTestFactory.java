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
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationContam;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint;

import java.util.Arrays;

public class AggregationTestFactory {
    public static Aggregation buildAggregation(String project, String sample, double contamination,
                                               LevelOfDetection fingerprintLod) {
        Aggregation aggregation = new Aggregation();
        aggregation.setSample(sample);
        aggregation.setProject(project);
        AggregationContam aggregationContam = new AggregationContam();
        aggregationContam.setPctContamination(contamination);
        aggregation.setAggregationContam(aggregationContam);

        AggregationReadGroup aggregationReadGroup = new AggregationReadGroup();

        PicardFingerprint picardFingerprint1=new PicardFingerprint();
        picardFingerprint1.setLodExpectedSample(fingerprintLod.getMax());
        PicardAnalysis picardAnalysis1 = new PicardAnalysis();
        picardAnalysis1.setPicardFingerprint(picardFingerprint1);
        picardAnalysis1.setLane("1");

        aggregationReadGroup.getPicardAnalysis().add(picardAnalysis1);


        PicardFingerprint picardFingerprint2=new PicardFingerprint();
        picardFingerprint2.setLodExpectedSample(fingerprintLod.getMin());
        PicardAnalysis picardAnalysis2 = new PicardAnalysis();
        picardAnalysis2.setPicardFingerprint(picardFingerprint2);
        picardAnalysis2.setLane("2");
        aggregationReadGroup.getPicardAnalysis().add(picardAnalysis2);
        aggregationReadGroup.setLane(2);
        aggregation.setAggregationReadGroups(Arrays.asList(aggregationReadGroup));
        return aggregation;
    }
}
