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
public class ManifestHeaderTest {
    public void testFromColumnNameReturnsEnum(){
        String goodHeaderName = "Patient_ID";
        ManifestHeader patientId = ManifestHeader.fromColumnName(goodHeaderName);
        assertThat(patientId, equalTo(ManifestHeader.PATIENT_ID));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFromColumnNameThrowsIllegalArgumentException() {
        String badHeaderName = "The_Patient_ID";
        ManifestHeader.fromColumnName(badHeaderName);
    }

    public void testFromColumnNameGeneratesErrors() {
        String goodHeaderName = "Patient_ID";
        String badHeaderName = "The_Patient_ID";
        List<String> errors = new ArrayList<>();
        ManifestHeader.fromColumnName(errors, badHeaderName, goodHeaderName);
        assertThat(errors, contains(badHeaderName));
        assertThat(errors, not(contains(goodHeaderName)));
    }
}
