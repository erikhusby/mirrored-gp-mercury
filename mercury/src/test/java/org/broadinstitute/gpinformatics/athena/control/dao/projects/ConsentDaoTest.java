package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.Consent;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class ConsentDaoTest extends ContainerTest {

    @Inject
    private ConsentDao consentDao;

    private Consent consent1;
    private Consent consent2;
    private Consent consent3;

    private String dupeIdentifier;
    private String nonDupeIdentifier;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void setUp() throws Exception {

        if(consentDao == null) {
            return;
        }

        Date today = new Date();
        dupeIdentifier = "testConsent" + today.getTime();

        consent1 = new Consent("Test first consent", Consent.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH,
                dupeIdentifier);
        consent2 = new Consent("Test first consent DupeName", Consent.Type.ORSP_NOT_ENGAGED,
                dupeIdentifier);
        nonDupeIdentifier = "testConsent no dupe" + today.getTime();
        consent3 = new Consent("Test third consent", Consent.Type.ORSP_NOT_ENGAGED,
                nonDupeIdentifier);

        consentDao.persist(consent1);
        consentDao.persist(consent2);
        consentDao.persist(consent3);
        consentDao.flush();
        consentDao.clear();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void tearDown() throws Exception {

        if(consentDao == null) {
            return;
        }

        consent1 = consentDao.findById(Consent.class, consent1.getConsentId());
        consent2 = consentDao.findById(Consent.class, consent2.getConsentId());
        consent3 = consentDao.findById(Consent.class, consent3.getConsentId());

        consentDao.remove(consent1);
        consentDao.remove(consent2);
        consentDao.remove(consent3);
        consentDao.flush();

        dupeIdentifier = "";
        nonDupeIdentifier = "";
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testFindByIdentifier() throws Exception {
        List<Consent> dupeIdentifiers = consentDao.findByIdentifier(dupeIdentifier);

        Assert.assertEquals(dupeIdentifiers.size(), 2);
        Assert.assertTrue(dupeIdentifiers.contains(consent1));
        Assert.assertTrue(dupeIdentifiers.contains(consent2));

        List<Consent> nonDupeIdentifiers = consentDao.findByIdentifier(nonDupeIdentifier);

        Assert.assertEquals(nonDupeIdentifiers.size(), 1);
        Assert.assertTrue(nonDupeIdentifiers.contains(consent3));
    }
}
