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

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;


public class LevelOfDetection implements Serializable {
    private static final long serialVersionUID = 3478940615499604L;
    public static LevelOfDetection nullSafeInstance = new LevelOfDetection(0d, 0d);
    private final Double min;
    private final Double max;

    public LevelOfDetection(Double min, Double max) {
        this.min = Optional.fromNullable(min).or(0d);
        this.max = Optional.fromNullable(max).or(0d);
        if (min.compareTo(max) > 0) {
            throw new IllegalStateException(
                    String.format("value of min(%f) can not be larger than that of max(%f)", min, max));
        }
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

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LevelOfDetection that = (LevelOfDetection) o;

        return new EqualsBuilder().append(getMin(), that.getMin()).append(getMax(), that.getMax()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getMin()).append(getMax()).toHashCode();
    }

    @Override
    public String toString() {
        return String.format("%2.2f/%2.2f", min, max);
    }
}
