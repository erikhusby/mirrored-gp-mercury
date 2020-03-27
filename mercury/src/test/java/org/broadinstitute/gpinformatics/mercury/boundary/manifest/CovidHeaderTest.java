package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class CovidHeaderTest {

    public void testFromColumnNameReturnsEnum() {
        String goodHeaderName = CovidHeader.PATIENT_ID.getColumnName();
        CovidHeader patientId = CovidHeader.fromColumnName(goodHeaderName);
        assertThat(patientId, equalTo(CovidHeader.PATIENT_ID));
    }

    @Test()
    public void testFromColumnNameThrowsIllegalArgumentException() {
        try {
            String badHeaderName = "The_Patient_ID";
            final CovidHeader testCovidHeader = CovidHeader.fromColumnName(badHeaderName);
            assertThat(testCovidHeader.getColumnName(), is(equalTo(badHeaderName)));
            assertThat(testCovidHeader.getMetadataKey(), is(equalTo(Metadata.Key.NA)));
            assertThat(testCovidHeader.isIgnoreColumn(), is(true));
            assertThat(testCovidHeader.isRequiredHeader(),is(false));
        } catch (Exception e) {
            Assert.fail("CovidHeader should not throw exception for unknown fields");
        }
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
        assertThat(errors, not(contains(badHeaderName)));
        assertThat(errors, not(contains(goodHeaderName)));
    }

    public void testToeMetadat() {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(CovidHeader.PATIENT_ID.getColumnName(), "sara jane");
        dataRow.put(CovidHeader.REQUESTING_PHYSICIAN.getColumnName(), "jane, sam");
        dataRow.put(CovidHeader.DATE_COLLECTED.getColumnName(), "3/19/2000");
        dataRow.put(CovidHeader.INSTITUTION_ID.getColumnName(), "Broad");
        dataRow.put(CovidHeader.SAMPLE_ID.getColumnName(), "2894384297423723");
        dataRow.put("Test Column", "Nothing much");
        dataRow.put("nothing to find", "empty");
        dataRow.put("", "empty");

        try {
            final Metadata[] metadataResult = CovidHeader.toMetadata(dataRow);
            assertThat(metadataResult.length, is(equalTo(4)));
        } catch (Exception e) {
            Assert.fail();
        }
    }

}
