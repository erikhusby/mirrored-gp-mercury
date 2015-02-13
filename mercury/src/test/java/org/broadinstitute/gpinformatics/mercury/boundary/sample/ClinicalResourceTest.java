/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.STANDARD)
public class ClinicalResourceTest extends RestServiceContainerTest {
    private static final String QA_DUDE_PM = "QaDudePM";
    @Inject
    private ClinicalResource clinicalResource;
    @Inject
    private ManifestSessionDao manifestSessionDao;
    @Inject
    private ManifestSessionEjb manifestSessionEjb;
    private static final long MANIFEST_ID = 5102l;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }


    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testWebService(@ArquillianResource URL baseUrl) throws Exception {
        Map<String,Object> postBody = new HashMap<String,Object>();
        postBody.put("username", QA_DUDE_PM);
        postBody.put("manifestId", MANIFEST_ID);

        postBody.put("samples", ClinicalSampleFactory.createCrspSample(ImmutableMap
                                .of(Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.SAMPLE_ID, "03101231193")));

        WebResource resource = makeWebResource(baseUrl, "addSamplesToManifest");
        resource = resource.queryParam("username", QA_DUDE_PM)
                .queryParam("manifestId", String.valueOf(MANIFEST_ID));
        resource.accept("application/json")
                        .type("application/json").post(ClientResponse.class, postBody);

        MultivaluedMap formData = new MultivaluedMapImpl();
          formData.add("name1", "val1");
          formData.add("name2", "val2");
          ClientResponse response = resource.type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);

        resource.entity(postBody).post(new GenericType<List<Sample>>() { });


    }


    public void testAddSamplesToManifest() throws Exception {
        ManifestSession manifestSession = manifestSessionDao.find(MANIFEST_ID);
        int numRecordsBefore = manifestSession.getRecords().size();
        List<Sample> samples = Arrays.asList(
                ClinicalSampleFactory.createCrspSample(ImmutableMap
                        .of(Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.SAMPLE_ID, "03101231193")),
                ClinicalSampleFactory.createCrspSample(ImmutableMap
                                .of(Metadata.Key.PATIENT_ID, "001-001", Metadata.Key.SAMPLE_ID, "03101067213")
                ));

        clinicalResource.addSamplesToManifest(QA_DUDE_PM, MANIFEST_ID, samples);

        manifestSession = manifestSessionDao.find(MANIFEST_ID);
        int numRecordsAfter = manifestSession.getRecords().size();

        assertThat(numRecordsAfter, equalTo(numRecordsBefore + samples.size()));
    }

    @Override
    protected String getResourcePath() {
        return "clinical";
    }
}
