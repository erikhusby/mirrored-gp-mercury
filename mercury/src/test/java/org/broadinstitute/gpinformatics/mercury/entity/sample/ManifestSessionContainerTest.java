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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

/**
 * Container tests for ManifestSessions.
 */
@Test(groups = TestGroups.STANDARD)
public class ManifestSessionContainerTest extends Arquillian {

    private static final String PATIENT_1 = "PATIENT 1";

    private static final String GENDER_MALE = "Male";
    private static final String GENDER_FEMALE = "Female";
    private static final String SAMPLE_ID_1 = "2482938";
    private static final String SAMPLE_ID_2 = "2342332";
    private static final String SAMPLE_ID_3 = "8482833";
    private static final String SAMPLE_ID_4 = "8284233";
    private static final String SAMPLE_ID_5 = "4772733";
    private static final String SAMPLE_ID_6 = "2244838";
    private static final String SAMPLE_ID_7 = "24829338";
    private static final String SAMPLE_ID_8 = "23423232";
    private static final String SAMPLE_ID_9 = "84821833";
    private static final String SAMPLE_ID_10 = "82844233";
    private static final String SAMPLE_ID_11 = "47732733";
    private static final String SAMPLE_ID_12 = "22424838";

    private ResearchProject researchProject;
    private ManifestSession manifestSessionI;
    private ManifestRecord manifestRecordI;
    private ManifestRecord manifestRecordIn2;
    private ManifestRecord manifestRecordIn3;
    private ManifestRecord manifestRecordIn4;
    private ManifestRecord manifestRecordIn5;
    private ManifestRecord manifestRecordIn6;
    private ManifestSession manifestSessionII;
    private ManifestRecord manifestRecordII1;
    private ManifestRecord manifestRecordII3;
    private ManifestRecord manifestRecordII4;
    private ManifestRecord manifestRecordII5;
    private ManifestRecord manifestRecordII6;
    private ManifestRecord manifestRecordII2;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject = ResearchProjectTestFactory.createTestResearchProject(ResearchProject.PREFIX + (new Date()).getTime());
        manifestSessionI = new ManifestSession(researchProject, "BUICK-TEST",
                new BSPUserList.QADudeUser("PM", 5176L));
        manifestRecordI = new ManifestRecord(new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE), new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_1));
        manifestRecordIn2 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_2),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "2"));
        manifestRecordIn3 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_3),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "3"));
        manifestRecordIn4 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_4),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "4"));
        manifestRecordIn5 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_5),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "5"));
        manifestRecordIn6 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_6),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "6"));
        manifestSessionII = new ManifestSession(researchProject, "BUICK-TEST2",
                new BSPUserList.QADudeUser("PM", 5176L));
        manifestRecordII1 = new ManifestRecord(new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "7"),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE), new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_7));
        manifestRecordII2 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_8),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "8"));
        manifestRecordII3 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_9),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "9"));
        manifestRecordII4 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_10),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "10"));
        manifestRecordII5 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_11),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "11"));
        manifestRecordII6 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_12),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "12"));


        manifestSessionI.addRecord(manifestRecordI);
        manifestSessionI.addRecord(manifestRecordIn2);
        manifestSessionI.addRecord(manifestRecordIn3);
        manifestSessionI.addRecord(manifestRecordIn4);
        manifestSessionI.addRecord(manifestRecordIn5);
        manifestSessionI.addRecord(manifestRecordIn6);

        manifestSessionI.addRecord(manifestRecordII1);
        manifestSessionI.addRecord(manifestRecordII2);
        manifestSessionI.addRecord(manifestRecordII3);
        manifestSessionI.addRecord(manifestRecordII4);
        manifestSessionI.addRecord(manifestRecordII5);
        manifestSessionI.addRecord(manifestRecordII6);

    }

    /**
     * Round trip test for ManifestSession.
     */
    public void roundTrip() {

        // Create and persist ManifestSession.

//        manifestSessionDao.persist(manifestSessionI);


//        manifestSessionI.setStatus(ManifestSession.SessionStatus.COMPLETED);
//        manifestRecordIn.setStatus(ManifestRecord.Status.SCANNED);
//        manifestRecordIn2.setStatus(ManifestRecord.Status.SCANNED);
//        manifestRecordIn3.setStatus(ManifestRecord.Status.SCANNED);
//        manifestRecordIn4.setStatus(ManifestRecord.Status.SCANNED);
//        manifestRecordIn5.setStatus(ManifestRecord.Status.SCANNED);
//        manifestRecordIn6.setStatus(ManifestRecord.Status.SCANNED);

        // Create ManifestSessionII.

        //Persit Everything
        manifestSessionDao.persist(manifestSessionI);


        assertThat(manifestSessionI.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionI.hasErrors(), is(equalTo(false)));
        assertThat(manifestSessionII.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionII.hasErrors(), is(equalTo(false)));
        assertThat(researchProject.getManifestSessions(), hasItems(manifestSessionI, manifestSessionII));

        // Clear the Session to force retrieval of a persistent instance 'manifestSessionOut' below that is distinct
        // from the detached 'manifestSessionI' instance.
        manifestSessionDao.clear();

        ManifestSession manifestSessionOut =
                manifestSessionDao.findById(ManifestSession.class, manifestSessionI.getManifestSessionId());

        assertThat(manifestSessionI.getRecords().size(), is(equalTo(manifestSessionOut.getRecords().size())));
        assertThat(manifestSessionOut.getRecords().size(), is(equalTo(1)));

        // Get the sole 'out' ManifestRecord for comparison with the sole 'in' ManifestRecord.
        //Just to make sure the annotations are correct
        ManifestRecord manifestRecordOut = manifestSessionOut.getRecords().get(0);
        assertThat(manifestRecordOut.getMetadata().size(), is(equalTo(manifestRecordI.getMetadata().size())));
        for (Metadata metadata : manifestRecordI.getMetadata()) {
            String inValue = metadata.getValue();
            String outValue = manifestRecordOut.getMetadataByKey(metadata.getKey()).getValue();
            assertThat(inValue, is(equalTo(outValue)));
        }

        assertThat(manifestRecordI.isQuarantined(), is(equalTo(false)));
        assertThat(manifestRecordOut.isQuarantined(), is(equalTo(manifestRecordI.isQuarantined())));

    }

    public void endToEnd() throws Exception {

        manifestSessionDao.persist(manifestSessionI);

        manifestSessionEjb.acceptManifestUpload(manifestSessionII.getManifestSessionId());

    }

}
