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

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.StringWriter;

@Test(groups = TestGroups.DATABASE_FREE)
public class BioProjectResultBeanTest {

    public void testBioProjectsJson() throws JAXBException {

        String testJson = "{\n"
                          + "\"bioprojects\": [{\"accession\":\"PRJNA185967\",\"alias\":\"phs000569\",\"projectName\":\"Primary submission\"},{\"accession\":\"PRJNA186400\",\"alias\":\"phs000584\",\"projectName\":\"Primary submission\"}\n"
                          + "]\n"
                          + "}";


        JSONJAXBContext context = new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                BioProjects.class);
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();
        BioProjects bioProjects = new BioProjects(
                new BioProject("PRJNA185967", "phs000569", "Primary submission"),
                new BioProject("PRJNA186400", "phs000584", "Primary submission")
        );

        marshaller.marshallToJSON(bioProjects, writer);
        Assert.assertEquals(writer.toString().replaceAll("\\s+", ""), testJson.replaceAll("\\s+", ""));
    }
}
