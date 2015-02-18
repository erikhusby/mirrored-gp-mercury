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
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
    private static final String MANIFEST_NAME = "fooManifest";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testWebService(@ArquillianResource URL baseUrl) throws Exception {
        ClinicalResourceBean clinicalResourceBean =
                ClinicalSampleFactory.createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, "RP-12", true,
                        ImmutableMap.of(Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.SAMPLE_ID, "03101231193"),
                        ImmutableMap.of(Metadata.Key.PATIENT_ID, "994-002", Metadata.Key.SAMPLE_ID, "93101231193")
                );

        WebResource resource = makeWebResource(baseUrl, ClinicalResource.CREATE_MANIFEST);

        ClientResponse response = resource.type(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML).entity(clinicalResourceBean)
                .post(ClientResponse.class);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }


    public void testAddSamplesToManifest() throws Exception {
        ManifestSession manifestSession = manifestSessionDao.find(MANIFEST_ID);
        int numRecordsBefore = manifestSession.getRecords().size();
        List<Sample> samples = Arrays.asList(
                ClinicalSampleFactory.createCrspSample(ImmutableMap
                        .of(Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.SAMPLE_ID, "03101231193")),
                ClinicalSampleFactory.createCrspSample(ImmutableMap
                        .of(Metadata.Key.PATIENT_ID, "001-001", Metadata.Key.SAMPLE_ID, "03101067213")));

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
