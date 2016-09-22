package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class OrspProjectTest {

    public void testGetNameParsesOutOrspNumber() {
        OrspProject project = new OrspProject(null, "ORSP-123 (Test project)", null, null, null, null);
        assertThat(project.getName(), equalTo("Test project"));
    }

    public void testGetNameTrimsWhitespace() {
        OrspProject project = new OrspProject(null, "ORSP-123 ( Test project )", null, null, null, null);
        assertThat(project.getName(), equalTo("Test project"));
    }

    public void testGetTypeTranslatesToEnumValue() {
        OrspProject project = new OrspProject(null, null, RegulatoryInfo.Type.IRB.getOrspServiceId(), null, null, null);
        assertThat(project.getType(), equalTo(RegulatoryInfo.Type.IRB));
    }
}
