package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.STANDARD)
public class ManifestSessionContainerTest extends Arquillian {

    private static final String PATIENT_1 = "PATIENT 1";

    private static final String GENDER_MALE = "Male";

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @BeforeMethod
    public void setUp() throws Exception {
        if (researchProjectDao == null) {
            return;
        }
    }

    public void manifestEvent() {
        ManifestEvent manifestEvent = new ManifestEvent("Everything is OK.", ManifestEvent.Type.INFO);
        researchProjectDao.persist(manifestEvent);
        researchProjectDao.flush();
    }

    public void roundTrip() {

        ResearchProject researchProject =
                ResearchProjectTestFactory.createTestResearchProject("RP-" + (new Date()).getTime());

        researchProjectDao.persist(researchProject);
        researchProjectDao.flush();

        ManifestSession manifestSessionIn = new ManifestSession(researchProject, "BUICK-TEST",
                new BSPUserList.QADudeUser("PM", 5176L));

        ManifestRecord manifestRecordIn = new ManifestRecord(
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE));

        manifestSessionIn.addRecord(manifestRecordIn);
        manifestSessionEjb.save(manifestSessionIn);
        manifestSessionDao.clear();

        ManifestSession manifestSessionOut =
                manifestSessionDao.findById(ManifestSession.class, manifestSessionIn.getManifestSessionId());

        ManifestRecord manifestRecordOut = manifestSessionOut.getRecords().get(0);
        assertThat(manifestRecordOut.getMetadata().size(), is(equalTo(manifestRecordIn.getMetadata().size())));
        for (Metadata metadata : manifestRecordIn.getMetadata()) {
            String inValue = metadata.getValue();
            String outValue = manifestRecordOut.getMetadataByKey(metadata.getKey()).getValue();
            assertThat(inValue, is(equalTo(outValue)));
        }

        assertThat(manifestRecordIn.getErrorStatus(), is(nullValue()));
        assertThat(manifestRecordOut.getErrorStatus(), is(equalTo(manifestRecordIn.getErrorStatus())));
    }
}
