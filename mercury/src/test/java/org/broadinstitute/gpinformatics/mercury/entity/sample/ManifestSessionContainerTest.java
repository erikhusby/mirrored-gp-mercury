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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

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
        if(researchProjectDao == null) {
            return;
        }
    }

    public void roundTrip() {

        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject("RP-"+(new Date()).getTime());

        researchProjectDao.persist(researchProject);
        researchProjectDao.flush();

        ManifestSession manifestSession = new ManifestSession(researchProject, "BUICK-TEST",
                        new BSPUserList.QADudeUser("PM", 5176L));

        ManifestRecord manifestRecordIn = new ManifestRecord(Arrays.asList(
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE)
        ));

        manifestSession.addRecord(manifestRecordIn);
        manifestSessionEjb.save(manifestSession);

        ManifestRecord manifestRecordOut = manifestSession.getRecords().get(0);
        for (Map.Entry<Metadata.Key, Metadata> entry : manifestRecordIn.getMetadata().entrySet()) {
            String inValue = entry.getValue().getValue();
            String outValue = manifestRecordOut.getField(entry.getKey()).getValue();
            assertThat(inValue, is(equalTo(outValue)));
        }
    }
}
