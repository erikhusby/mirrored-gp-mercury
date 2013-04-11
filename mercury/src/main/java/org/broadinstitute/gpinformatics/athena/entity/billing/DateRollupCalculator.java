package org.broadinstitute.gpinformatics.athena.entity.billing;

import java.util.Date;

/**
 * This interface provides a way to normalize a specified date into a particular date bucket. Implementations can
 * segregate by day, week, month, etc.
 *
 * @author hrafal
 */
public interface DateRollupCalculator {
    public Date getBucketDate(Date workCompleteDate);
}
