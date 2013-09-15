package org.broadinstitute.gpinformatics.athena.entity.billing;

import java.io.Serializable;
import java.util.Date;

/**
 * This interface provides a way to normalize a specified date into a particular date bucket. Implementations can
 * segregate by day, week, month, etc.
 */
public interface DateRollupCalculator extends Serializable {
    Date getBucketDate(Date workCompleteDate);
}
