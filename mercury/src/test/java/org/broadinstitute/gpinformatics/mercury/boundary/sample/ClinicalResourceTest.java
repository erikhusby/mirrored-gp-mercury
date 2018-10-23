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
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.STANDARD)
public class ClinicalResourceTest extends RestServiceContainerTest {
    private static final String QA_DUDE_PM = "QaDudePM";
    @Inject
    private ClinicalResource clinicalResource;
    @Inject
    private ManifestSessionDao manifestSessionDao;
    @Inject
    private ManifestSessionEjb manifestSessionEjb;
    @Inject
    private MercurySampleDao mercurySampleDao;
    private static final long MANIFEST_ID = 5102l;
    private static final String MANIFEST_NAME = "fooManifest";

    private static final String EXISTING_RESEARCH_PROJECT_KEY = "RP-12";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    public void testCreateManifestFails(@ArquillianResource URL baseUrl) throws Exception {
        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, EXISTING_RESEARCH_PROJECT_KEY, true, 0);

        WebResource resource = makeWebResource(baseUrl, ClinicalResource.CREATE_MANIFEST);
        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE).entity(clinicalResourceBean)
                .post(ClientResponse.class);
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        String errorMessage = response.getEntity(String.class);
        assertThat(errorMessage, is(ClinicalResource.EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED));
    }

    public void testCreateManifestWithSamples() throws Exception {
        String sampleId = "SM-1";

        MercurySample sampleForTest = mercurySampleDao.findBySampleKey(sampleId);
        if (sampleForTest == null) {
            sampleForTest = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            mercurySampleDao.persist(sampleForTest);
        } else {
            assertThat(sampleForTest.getMetadataSource(), equalTo(MercurySample.MetadataSource.MERCURY));
        }

        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, EXISTING_RESEARCH_PROJECT_KEY, Boolean.TRUE,
                        ImmutableMap.of(
                                Metadata.Key.BROAD_SAMPLE_ID, sampleId, Metadata.Key.MATERIAL_TYPE,
                                MaterialType.DNA_DNA_GENOMIC.getDisplayName()));
        long manifestId = clinicalResource.createManifestWithSamples(clinicalResourceBean);

        ManifestSession manifestSession = manifestSessionDao.find(manifestId);

        assertThat(manifestSession.getSessionName(), startsWith(MANIFEST_NAME));
        assertThat(manifestSession.getResearchProject().getBusinessKey(), equalTo(EXISTING_RESEARCH_PROJECT_KEY));
        assertThat(manifestSession.getStatus(), equalTo(ManifestSession.SessionStatus.ACCESSIONING));
        assertThat(manifestSession.getRecords().size(), equalTo(1));
        ManifestRecord manifestRecord = manifestSession.getRecords().iterator().next();
        assertThat(manifestRecord.getValueByKey(Metadata.Key.BROAD_SAMPLE_ID), equalTo(sampleId));
        // After running through the method, we change the metadata source from mercury to the crsp portal.
        assertThat(sampleForTest.getMetadataSource(), equalTo(MercurySample.MetadataSource.CRSP_PORTAL));
    }

    public void testCreateManifestWithSamplesWithoutMaterialType() throws Exception {
        String sampleId = "SM-1";

        MercurySample sampleForTest = mercurySampleDao.findBySampleKey(sampleId);
        if (sampleForTest == null) {
            sampleForTest = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            mercurySampleDao.persist(sampleForTest);
        } else {
            assertThat(sampleForTest.getMetadataSource(), equalTo(MercurySample.MetadataSource.MERCURY));
        }

        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, EXISTING_RESEARCH_PROJECT_KEY, Boolean.TRUE,
                        ImmutableMap.of(Metadata.Key.BROAD_SAMPLE_ID, sampleId));

        try {
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getLocalizedMessage(), startsWith(ClinicalResource.REQUIRED_FIELD_MISSING));
            assertThat(e.getLocalizedMessage(), endsWith(
                    String.valueOf(ClinicalResource.REQUIRED_METADATA_KEYS)));
        }
    }

    public void testCreateManifestWithSamplesWithUnrecognizedMaterialType() throws Exception {
        String sampleId = "SM-1";

        MercurySample sampleForTest = mercurySampleDao.findBySampleKey(sampleId);
        if (sampleForTest == null) {
            sampleForTest = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
            mercurySampleDao.persist(sampleForTest);
        } else {
            assertThat(sampleForTest.getMetadataSource(), equalTo(MercurySample.MetadataSource.MERCURY));
        }

        ClinicalResourceBean clinicalResourceBean = ClinicalSampleTestFactory
                .createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, EXISTING_RESEARCH_PROJECT_KEY, Boolean.TRUE,
                        ImmutableMap.of(Metadata.Key.BROAD_SAMPLE_ID, sampleId, Metadata.Key.MATERIAL_TYPE, "Fresh DNA"));

        try {
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getLocalizedMessage(), startsWith(ClinicalResource.UNRECOGNIZED_MATERIAL_TYPE));
        }
    }

    public void testCreateManifestWithSamplesHavingEmptyMetadataValues() throws Exception {
        String sampleId = "";
        ClinicalResourceBean clinicalResourceBean =
                ClinicalSampleTestFactory
                        .createClinicalResourceBean(QA_DUDE_PM, MANIFEST_NAME, EXISTING_RESEARCH_PROJECT_KEY,
                                Boolean.TRUE, ImmutableMap.of(Metadata.Key.BROAD_SAMPLE_ID, sampleId));
        try {
            clinicalResource.createManifestWithSamples(clinicalResourceBean);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getLocalizedMessage(), is(ClinicalResource.SAMPLE_CONTAINS_NO_METADATA));
        }
    }

    @Override
    protected String getResourcePath() {
        return "clinical";
    }

}
