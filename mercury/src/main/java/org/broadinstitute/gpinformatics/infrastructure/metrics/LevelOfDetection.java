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

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LevelOfDetection {

    private final Double min;
    private final Double max;

    public LevelOfDetection(@Nonnull Double min, @Nonnull Double max) {
        this.min = min;
        this.max = max;
        if (min > max) {
            throw new IllegalStateException(
                    String.format("value of min(%f) can not be larger than that of max(%f)", min, max));
        }
    }

    public static LevelOfDetection calculate(Collection<AggregationReadGroup> aggregationReadGroups) {
        if (CollectionUtils.isEmpty(aggregationReadGroups)) {
            return null;
        }
        Map<String, Double> laneLodMap = new HashMap<>();

        Set<String> lanes=new HashSet<>();
        for (AggregationReadGroup aggregationReadGroup : aggregationReadGroups) {
            for (PicardAnalysis picardAnalysis : aggregationReadGroup.getPicardAnalysis()) {
                PicardFingerprint fingerprint = picardAnalysis.getPicardFingerprint();
                lanes.add(picardAnalysis.getLane());
                if (fingerprint.getLodExpectedSample() != null) {
                    laneLodMap.put(picardAnalysis.getLane(), fingerprint.getLodExpectedSample());
                }
            }
        }
        if (lanes.size() == laneLodMap.size() || laneLodMap.values().isEmpty()) {
            return new LevelOfDetection(Collections.min(laneLodMap.values()), Collections.max(laneLodMap.values()));
        }
        return null;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LevelOfDetection)) {
            return false;
        }

        LevelOfDetection that = (LevelOfDetection) o;

        if (max != null ? !max.equals(that.max) : that.max != null) {
            return false;
        }
        return !(min != null ? !min.equals(that.min) : that.min != null);

    }

    @Override
    public int hashCode() {
        int result = min != null ? min.hashCode() : 0;
        result = 31 * result + (max != null ? max.hashCode() : 0);
        return result;
    }

    public String displayString() {
        return String.format("%2.2f/%2.2f", min, max);
    }
}
