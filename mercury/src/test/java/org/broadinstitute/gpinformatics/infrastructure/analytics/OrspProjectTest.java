/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class OrspProjectTest {

    public void testGetNameParsesOutOrspNumber() {
        OrspProject project = new OrspProject("ORSP-123", "ORSP-123 (Test project)", null, null, null, null);
        assertThat(project.getName(), equalTo("Test project"));
    }

    public void testGetNameTrimsWhitespace() {
        OrspProject project = new OrspProject("ORSP-123", "ORSP-123 ( Test project )", null, null, null, null);
        assertThat(project.getName(), equalTo("Test project"));
    }

    public void testGetTypeTranslatesToEnumValue() {
        OrspProject project = new OrspProject(null, null, RegulatoryInfo.Type.IRB.getOrspServiceId(), null, null, null);
        assertThat(project.getType(), equalTo(RegulatoryInfo.Type.IRB));
    }
}
