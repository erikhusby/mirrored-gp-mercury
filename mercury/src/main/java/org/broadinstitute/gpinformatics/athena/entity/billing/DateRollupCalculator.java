package org.broadinstitute.gpinformatics.athena.entity.billing;

import java.util.Date;

/**
 * This class is...
 *
 * @author hrafal
 */
public interface DateRollupCalculator {
    public Date getBucketDate(Date workCompleteDate);
}
