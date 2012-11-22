package org.broadinstitute.gpinformatics.infrastructure.experiments;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/11/12
 * Time: 5:00 PM
 */
public class ExperimentId {
    public final String value;

     public ExperimentId(String value) {
         if ((value == null) || StringUtils.isBlank(value)) {
             throw new IllegalArgumentException("ExperimentId is invalid. Must be non-null and non-empty.");
         }
         this.value = value;
     }

     public String getValue() {
         return value;
     }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ExperimentId)) return false;

        final ExperimentId that = (ExperimentId) o;

        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
