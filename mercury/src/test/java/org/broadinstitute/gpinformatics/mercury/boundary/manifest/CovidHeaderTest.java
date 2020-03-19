package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class CovidHeaderTest {

    public void testFromColumnNameReturnsEnum() {
        String goodHeaderName = CovidHeader.PATIENT_ID.getColumnName();
        CovidHeader patientId = CovidHeader.fromColumnName(goodHeaderName);
        assertThat(patientId, equalTo(CovidHeader.PATIENT_ID));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromColumnNameThrowsIllegalArgumentException() {
        String badHeaderName = "The_Patient_ID";
        CovidHeader.fromColumnName(badHeaderName);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromColumnNameWithNullThrowsIllegalArgumentException() {
        CovidHeader.fromColumnName(null);
    }

    public void testFromColumnNameGeneratesErrors() {
        String goodHeaderName = CovidHeader.PATIENT_ID.getColumnName();
        String badHeaderName = "The_Patient_ID";
        List<String> errors = new ArrayList<>();
        CovidHeader.fromColumnName(errors, badHeaderName, goodHeaderName);
        assertThat(errors, contains(badHeaderName));
        assertThat(errors, not(contains(goodHeaderName)));
    }

}
