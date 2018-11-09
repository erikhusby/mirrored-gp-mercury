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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionSampleResultBeanTest {
    private static final String testJson="{\"accession\":\"PRJNA75723\",\"submittedSampleIds\":[\"4304714212_K\",\"4377315018_E\",\"4304714040_C\",\"4325507016_K\"]}";
    SubmissionSampleResultBean resultBean;
//    SubmissionSampleResultBean


    @BeforeMethod
    public void setUp() throws Exception {
        resultBean=new SubmissionSampleResultBean("PRJNA75723", "4304714212_K", "4377315018_E","4304714040_C","4325507016_K");
    }

    public void testSerialize() throws Exception {
           String serialized = MercuryStringUtils.serializeJsonBean(resultBean);
           assertThat(serialized.replaceAll("\\s+", ""), equalTo(testJson));
       }

       public void testDeSerialize() throws Exception {
           SubmissionSampleResultBean requestBean = MercuryStringUtils.deSerializeJsonBean(testJson, SubmissionSampleResultBean.class);

           assertThat(requestBean.getAccession(), equalTo(resultBean.getAccession()));
           assertThat(requestBean.getSubmittedSampleIds(), containsInAnyOrder(resultBean.getSubmittedSampleIds().toArray()));
           assertThat(requestBean, equalTo(resultBean));
       }
}
