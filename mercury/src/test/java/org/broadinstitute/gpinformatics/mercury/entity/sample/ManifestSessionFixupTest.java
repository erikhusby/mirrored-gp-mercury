package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.Sets;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata_;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventFixup;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Fixup test to assist in all deviations Manifest session related
 */
@Test(groups = TestGroups.FIXUP)
public class ManifestSessionFixupTest extends Arquillian {

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private UserTransaction utx;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void fixupGPLIM3031CleanLingeringSessions() {

        userBean.loginOSUser();

        List<ManifestSession> allOpenSessions = manifestSessionDao.findOpenSessions();

        for (ManifestSession openSession : allOpenSessions) {
            for (ManifestRecord nonQuarantinedManifestRecord : openSession.getNonQuarantinedRecords()) {
                nonQuarantinedManifestRecord.setStatus(ManifestRecord.Status.ACCESSIONED);
            }
            openSession.setStatus(ManifestSession.SessionStatus.COMPLETED);
        }
        manifestSessionDao.flush();

        List<ManifestSession> allClosedSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();

        for (ManifestSession closedSession : allClosedSessions) {
            for (ManifestRecord manifestRecord : closedSession.getNonQuarantinedRecords()) {
                manifestRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);
            }

        }
        manifestSessionDao.flush();
    }

    @Test(enabled = false)
    public void fix_GPLIM5058_samplesLinkedToWrongRCT() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        String[] samplesToFix = new String[]{"SM-CUZB2", "SM-CJGJX"};
        final String currentIncorrectRct = "RCT-208";
        String originalSessionPrefix = "ORDER-10243";
        String correctedRct = "RCT-217";
        String correctedSessionPrefix= "ORDER-10246";

        // Get current ManifestSession;
        List<ManifestSession> manifestRecordsByReceiptAndSample = getManifestRecords(currentIncorrectRct, originalSessionPrefix,
            samplesToFix);
        assertThat(manifestRecordsByReceiptAndSample, hasSize(1));
        ManifestSession oldSession= manifestRecordsByReceiptAndSample.iterator().next();
        List<ManifestRecord> recordsToMove = oldSession.getRecords();
        assertThat(recordsToMove, hasSize(samplesToFix.length));

        // Get correct ManifestSession to transfer samples to
        List<ManifestSession> correctSessions = getManifestRecords(correctedRct, correctedSessionPrefix);
        assertThat(correctSessions, hasSize(1));
        ManifestSession correctSession = correctSessions.iterator().next();

        // retrieve jira issues to update.
        JiraIssue originalJiraIssue = manifestSessionEjb.findJiraIssue(oldSession);
        assertThat(originalJiraIssue, notNullValue());
        JiraIssue correctJiraIssue = manifestSessionEjb.findJiraIssue(correctSession);
        assertThat(correctJiraIssue, notNullValue());

        // Find the receipt events for the original accessioning and update it.
        String eventLocation = manifestSessionEjb.buildEventLocationName(oldSession);
        BspUser correctReceiptUser = manifestSessionEjb.getBspUser(correctSession, correctJiraIssue);
        for (ManifestRecord manifestRecord : recordsToMove) {
            long disambiguator = manifestRecord.getSpreadsheetRowNumber();
            LabEvent labEvent = labEventDao.findByLocationDateDisambiguator(eventLocation, originalJiraIssue.getCreated(), disambiguator);
            assertThat(labEvent, notNullValue());
            if (labEvent!=null) {
                LabEventFixup.fixupLabEvent(labEvent, correctReceiptUser, correctJiraIssue.getCreated(), eventLocation);
            }
        }

        // Update receipt metadata of sample to reflect the correct receipt record.
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(Arrays.asList(samplesToFix));
        for (MercurySample mercurySample :mercurySamples) {
            for (Metadata metadata : mercurySample.getMetadata()) {
                if (metadata.getKey() == Metadata.Key.RECEIPT_RECORD) {
                    metadata.setStringValue(correctSession.getReceiptTicket());
                }
            }
        }

        // update the jira RCT ticket
        manifestSessionEjb.updateReceiptInfo(oldSession, correctJiraIssue.getKey());
        manifestSessionEjb.transitionReceiptTicket(correctJiraIssue, correctSession, Sets.newHashSet(samplesToFix));

        originalJiraIssue.addComment(String.format(
            "Some accessioned samples were mistakenly added to this ticket. The corrected receipt for samples %s is %s. For more information see GPLIM-5058",
            Arrays.asList(samplesToFix), correctJiraIssue.getKey()));

        manifestSessionDao.persist(new FixupCommentary("GPLIM-5058 update receipt ticket."));
        utx.commit();
    }

    private List<ManifestSession> getManifestRecords(final String receiptTicket, final String sessionPrefix,
                                                     final String... sampleIds) {
        return manifestSessionDao.findAll(ManifestSession.class, new GenericDao.GenericDaoCallback<ManifestSession>() {

            @Override
            public void callback(CriteriaQuery<ManifestSession> criteriaQuery, Root<ManifestSession> sessionRoot) {
                CriteriaBuilder criteriaBuilder = manifestSessionDao.getEntityManager().getCriteriaBuilder();
                ListJoin<ManifestSession, ManifestRecord> manifestRecordsJoin =
                    sessionRoot.join(ManifestSession_.records);
                Join<ManifestRecord, Metadata> metadataJoin = manifestRecordsJoin.join(ManifestRecord_.metadata);
                List<Predicate> sampleIdPredicates = new ArrayList<>(sampleIds.length);
                for (String sampleId : sampleIds) {
                    sampleIdPredicates.add(criteriaBuilder.equal(metadataJoin.get(Metadata_.stringValue), sampleId));
                }

                List<Predicate> queryPredicates = new ArrayList<>();
                queryPredicates
                    .add(criteriaBuilder.equal(sessionRoot.get(ManifestSession_.receiptTicket), receiptTicket));
                queryPredicates
                    .add(criteriaBuilder.equal(sessionRoot.get(ManifestSession_.sessionPrefix), sessionPrefix));

                if (!sampleIdPredicates.isEmpty()) {
                    queryPredicates.add(criteriaBuilder.and(
                        criteriaBuilder.equal(metadataJoin.get(Metadata_.key), Metadata.Key.BROAD_SAMPLE_ID),
                        criteriaBuilder.or(sampleIdPredicates.toArray(new Predicate[sampleIdPredicates.size()]))
                    ));
                }
                criteriaQuery.where(queryPredicates.toArray(new Predicate[queryPredicates.size()])).distinct(true);
            }
        });
    }


    @Test(enabled = false)
    public void fixupCrsp468() {
        userBean.loginOSUser();
        boolean found = false;
        for (ManifestSession manifestSession : manifestSessionDao.findOpenSessions()) {
            if (manifestSession.getSessionName().startsWith("ORDER-11159")) {
                for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
                    if (manifestRecord.getSampleId().equals("SM-DPE87")) {
                        System.out.println("Updating manifest record " + manifestRecord.getManifestRecordId());
                        manifestRecord.getMetadataByKey(Metadata.Key.BROAD_SAMPLE_ID).setStringValue("SM-DPE8Z");
                        found = true;
                        break;
                    }
                }
            }
        }
        Assert.assertTrue(found);

        manifestSessionDao.persist(new FixupCommentary("CRSP-468 change sample ID"));
        manifestSessionDao.flush();
    }
}
