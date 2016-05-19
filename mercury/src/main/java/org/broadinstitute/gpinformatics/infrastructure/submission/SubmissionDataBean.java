/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class SubmissionDataBean implements Serializable {
    private static final long serialVersionUID = 9052015315572483720L;

    public SubmissionDataBean() {
    }

    private List<SubmissionData> submissionData=new ArrayList<>();

    public SubmissionDataBean(List<SubmissionData> submissionData) {
        this.submissionData = submissionData;
    }

    public List<SubmissionData> getSubmissionData() {
        return submissionData;
    }

    public void setSubmissionData(List<SubmissionData> submissionData) {
        this.submissionData = submissionData;
    }
}
