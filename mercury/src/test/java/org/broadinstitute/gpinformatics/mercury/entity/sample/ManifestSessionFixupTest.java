package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Fixup test to assist in all deviations Manifest session related
 */
@Test(groups = TestGroups.FIXUP)
public class ManifestSessionFixupTest extends Arquillian {

    private static final Pattern TAB_PATTERN = Pattern.compile("\\t");

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

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

        String eventLocation = manifestSessionEjb.buildEventLocationName(oldSession);
        BspUser correctReceiptUser = manifestSessionEjb.getBspUser(correctSession, correctJiraIssue);

        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(Arrays.asList(samplesToFix));
        for (MercurySample mercurySample :mercurySamples) {

            // Update original receipt event with corrected created information;
            LabEvent labEvent = mercurySample.getReceiptEvent();
            assertThat(labEvent, notNullValue());
            if (labEvent!=null) {
                LabEventFixup.fixupLabEvent(labEvent, correctReceiptUser, correctJiraIssue.getCreated(), eventLocation);
            }

            // Update receipt metadata of sample to reflect the correct receipt record.
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

    /**
     * @deprecated use fixupCrsp616, it can handle more cases
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateManifestSession.txt, so
     * it can be used for other similar fixups, without writing a new test.
     * Line 1 is the fixup commentary.
     * Line 2 is the session prefix.
     * Line 3 and subsequent are SampleId\tMetadata.Key\value.
     * Example contents of the file are:
     * CRSP-538
     * MERCURY UPLOAD Myeloid Lymphoid CLIA VAL manifest_PDO-13048
     * 1125699613	TUMOR_NORMAL	Normal
     * 1125699614	TUMOR_NORMAL	Normal
     */
    @Deprecated
    @Test(enabled = false)
    public void fixupCrsp538() throws IOException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateManifestSession.txt"));
        String fixupComment = lines.get(0);
        String sessionPrefix = lines.get(1);

        boolean found = false;
        for (ManifestSession manifestSession : manifestSessionDao.findOpenSessions()) {
            if (manifestSession.getSessionName().startsWith(sessionPrefix)) {
                found = true;
                for (String line : lines.subList(2, lines.size())) {
                    String[] fields = TAB_PATTERN.split(line);
                    if (fields.length != 3) {
                        throw new RuntimeException("Expected three tab separated fields in " + line);
                    }
                    ManifestRecord manifestRecord = manifestSession.getRecordWithMatchingValueForKey(
                            Metadata.Key.SAMPLE_ID, fields[0]);
                    Assert.assertNotNull(manifestRecord, fields[0]);
                    manifestRecord.getMetadataByKey(Metadata.Key.valueOf(fields[1])).setStringValue(fields[2]);
                }
                break;
            }
        }
        Assert.assertTrue(found);

        manifestSessionDao.persist(new FixupCommentary(fixupComment));
        manifestSessionDao.flush();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateManifestSession.txt, so
     * it can be used for other similar fixups, without writing a new test.
     * Line 1 is the fixup commentary.
     * Line 2 is the session prefix.
     * Line 3 and subsequent are SAMPLE_ID or BROAD_SAMPLE_ID\tMetadata.Key\tvalue.
     * Example contents of the file are:
     * CRSP-616
     * ORDER-15598
     * SM-DPE8N	BROAD_SAMPLE_ID	SM-H24UB
     * SM-H24UB	BROAD_SAMPLE_ID	SM-DPE8N
     */
    @Test(enabled = false)
    public void fixupCrsp616() throws IOException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateManifestSession.txt"));
        String fixupComment = lines.get(0);
        String sessionPrefix = lines.get(1);

        boolean found = false;
        Map<String, ManifestRecord> mapSampleIdToRecord = new HashMap<>();
        for (ManifestSession manifestSession : manifestSessionDao.findOpenSessions()) {
            if (manifestSession.getSessionName().startsWith(sessionPrefix)) {
                found = true;
                for (String line : lines.subList(2, lines.size())) {
                    String[] fields = TAB_PATTERN.split(line);
                    if (fields.length != 3) {
                        throw new RuntimeException("Expected three tab separated fields in " + line);
                    }
                    String sampleId = fields[0];
                    ManifestRecord manifestRecord = manifestSession.getRecordWithMatchingValueForKey(
                            Metadata.Key.SAMPLE_ID, sampleId);
                    if (manifestRecord == null) {
                        manifestRecord = manifestSession.getRecordWithMatchingValueForKey(
                                Metadata.Key.BROAD_SAMPLE_ID, sampleId);
                    }
                    Assert.assertNotNull(manifestRecord, sampleId);
                    mapSampleIdToRecord.put(sampleId, manifestRecord);
                }
                break;
            }
        }
        Assert.assertTrue(found);
        for (String line : lines.subList(2, lines.size())) {
            String[] fields = TAB_PATTERN.split(line);
            ManifestRecord manifestRecord = mapSampleIdToRecord.get(fields[0]);
            manifestRecord.getMetadataByKey(Metadata.Key.valueOf(fields[1])).setStringValue(fields[2]);
        }

        manifestSessionDao.persist(new FixupCommentary(fixupComment));
        manifestSessionDao.flush();
    }

    @Test(enabled = false)
    public void fixupCovidStateMa7576() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        final ManifestSession manifestSession = manifestSessionDao.find(238256);
        manifestSession.getRecords().stream().filter(manifestRecord -> Arrays.asList("75", "76").contains(manifestRecord.getSampleId()))
                .forEach(manifestRecord -> {
                    if(manifestRecord.getStatus() == ManifestRecord.Status.SCANNED &&
                       StringUtils.equals(manifestRecord.getSampleId(), "76")) {
                        manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).setStringValue("75");
                    } else if(manifestRecord.getStatus() == ManifestRecord.Status.UPLOAD_ACCEPTED &&
                              StringUtils.equals(manifestRecord.getSampleId(), "75")) {
                        manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).setStringValue("76");
                    }
                });

        utx.commit();

    }

}
