/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.Serializable;

public interface ISubmissionTuple extends Serializable {

    String getProject();

    String getSampleName();

    @JsonIgnore
    String getVersionString();

    FileType getFileType();

    String getProcessingLocation();

    String getDataType();

    @JsonIgnore
    SubmissionTuple getSubmissionTuple();
}
