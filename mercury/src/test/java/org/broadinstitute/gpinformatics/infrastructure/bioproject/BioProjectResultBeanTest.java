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

package org.broadinstitute.gpinformatics.infrastructure.bioproject;

import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

@Test(groups = TestGroups.DATABASE_FREE)
public class BioProjectResultBeanTest {

    public void testBioProjectsJson() throws IOException {

        String testJson = "{\n"
                          + "\"bioprojects\": [{\"accession\":\"PRJNA185967\",\"alias\":\"phs000569\",\"projectName\":\"Primary submission\"},{\"accession\":\"PRJNA186400\",\"alias\":\"phs000584\",\"projectName\":\"Primary submission\"}\n"
                          + "]\n"
                          + "}";

        BioProjects bioProjecrsDeSerialized = MercuryStringUtils.deSerializeJsonBean(testJson, BioProjects.class);
        BioProjects bioProjects = new BioProjects(
                new BioProject("PRJNA185967", "phs000569", "Primary submission"),
                new BioProject("PRJNA186400", "phs000584", "Primary submission")
        );

        Assert.assertEquals(bioProjects, bioProjecrsDeSerialized);
    }
    public void testBioProjectsUnmarshallNullFields() throws IOException {

        String testJson = "{\"bioprojects\": [{\"accession\":\"PRJNA185967\"},{\"accession\":\"PRJNA186400\"}]}";

        BioProjects bioProjects = new BioProjects(new BioProject("PRJNA185967"), new BioProject("PRJNA186400"));

        String serialized = MercuryStringUtils.serializeJsonBean(bioProjects);

        Assert.assertEquals(serialized.replaceAll("\\s+", ""), testJson.replaceAll("\\s+", ""));
    }
}
