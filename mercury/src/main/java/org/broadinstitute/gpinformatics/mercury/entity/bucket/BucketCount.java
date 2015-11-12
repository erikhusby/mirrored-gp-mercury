/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import java.util.Map;
import java.util.TreeMap;

public class BucketCount {
    String bucket;
    Long bucketEntryCount;
    Long reworkEntryCount;

    public BucketCount(String bucket, Long bucketEntryCount, Long reworkEntryCount) {
        this.bucket = bucket;
        this.bucketEntryCount = bucketEntryCount;
        this.reworkEntryCount = reworkEntryCount;
    }

    public BucketCount(String bucketName) {
        this(bucketName, 0l, 0l);
    }


    public String getBucket() {
        return bucket;
    }

    public Long getBucketEntryCount() {
        return bucketEntryCount;
    }

    public Long getReworkEntryCount() {
        return reworkEntryCount;
    }

}
