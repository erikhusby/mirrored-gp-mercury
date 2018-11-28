package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jvnet.inflector.Noun;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
@Test(groups = TestGroups.FIXUP)
public class ResearchProjectFixupTest extends Arquillian {
    @Inject
    private ResearchProjectEjb researchProjectEjb;

    @Inject
    private ResearchProjectDao rpDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Need to get default reference sequence - Homo_sapiens_assembly19|1.  This is the Reference sequence that will be
     * used for Exome projects.
     *
     * @throws Exception Any problems that might occur get thrown
     */
    @Test(enabled = false)
    public void fixupGpLim1927() throws Exception {
        List<ResearchProject> rpListToPersist = new ArrayList<>();
        List<ResearchProject> rpList = rpDao.findAllResearchProjects();

        final String DEFAULT_REFERENCE_SEQUENCE = "Homo_sapiens_assembly19|1";
        int count = 0;

        for (ResearchProject rp : rpList) {
            if (StringUtils.isBlank(rp.getReferenceSequenceKey())) {
                rp.setReferenceSequenceKey(DEFAULT_REFERENCE_SEQUENCE);
                rpListToPersist.add(rp);
                count++;
            }
        }

        if (count == 0) {
            Assert.fail("No Research Projects updated but there should have been some that needed updating.");
        }

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        rpDao.persistAll(rpListToPersist);
    }

    /**
     * Hacohen_cancer_exome_sequencing (RP-944) was created by accident. Can this please be deleted? Thanks!
     */
    @Test(enabled = false)
    public void fixupGPLIM3526_Delete_RP944() {
        userBean.loginOSUser();
        String RP944 = "RP-944";

        ResearchProject researchProject = rpDao.findByJiraTicketKey(RP944);
        if (researchProject == null) {
            Assert.fail(String.format("Research Project %s doesn't exist.", RP944));
        }

        if (CollectionUtils.isNotEmpty(researchProject.getProductOrders())) {
            int pdoCount = researchProject.getProductOrders().size();
            Assert.fail(String.format("Cannot delete %s: %d %s exist", RP944, pdoCount,
                    Noun.pluralOf("product", pdoCount)));
        }

        rpDao.remove(researchProject);
        rpDao.persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3526"));
    }

    @Test(enabled = false)
    public void fixupGPLIM_4880_Change_Title() {
        userBean.loginOSUser();

        final String RP1449 = "RP-1449";
        final String OLD_TITLE = "Nada Kalaany - Boston Children's Hospital";
        final String NEW_TITLE = "Nada Kalaany - Boston Childrens Hospital";

        ResearchProject researchProject = rpDao.findByJiraTicketKey(RP1449);
        if (researchProject == null) {
            Assert.fail(String.format("Research Project %s doesn't exist.", RP1449));
        }
        assertThat(researchProject.getTitle(), equalTo(OLD_TITLE));
        researchProject.setTitle(NEW_TITLE);

        rpDao.persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4880"));
    }

    /**
     * For each Project missing a JIRA ticket poke the service to create & associate one
     */
    @Test(enabled = false)
    public void fixupGpLim95() throws IOException {
        List<ResearchProject> rpList = rpDao.findAllResearchProjects();
        List<ResearchProject> rpListToPersist = new ArrayList<>();
        int count = 0;
        StringBuilder userTitleList = new StringBuilder();
        for (ResearchProject rp : rpList) {
            if (StringUtils.isBlank(rp.getJiraTicketKey())) {
                try {
                    researchProjectEjb.submitToJira(rp);
                    rpListToPersist.add(rp);
                } catch (Exception e) {
                    count++;
                    userTitleList.append(rp.getTitle()).append(":").append(rp.getCreatedBy()).append(" , ");
                }
            }
        }
        if (count > 0) {
            Assert.fail(count + " exceptions occurred. " + userTitleList);
        }

        // The entity is already persistent, this call to persist is solely to begin and end a transaction, so the
        // change gets flushed.  This is an artifact of the test environment.
        rpDao.persistAll(rpListToPersist);
    }

    /**
     * Helper method to change the owner of a research project.
     *
     * @param newOwnerUsername new owner's username
     * @param projectKeys      list of RP keys
     */
    private void changeProjectOwner(String newOwnerUsername, String... projectKeys) {
        for (BspUser user : bspUserList.find(newOwnerUsername)) {
            if (user.getUsername().equals(newOwnerUsername)) {
                for (String key : projectKeys) {
                    ResearchProject researchProject = rpDao.findByBusinessKey(key);
                    researchProject.setCreatedBy(user.getUserId());
                    rpDao.persist(researchProject);
                }
                return;
            }
        }

        throw new RuntimeException("No " + newOwnerUsername + " Found!");
    }

    @Test(enabled = false)
    public void reassignRPUser() {
        changeProjectOwner("stevens", "RP-28", "RP-30", "RP-80", "RP-81");
    }

    @Test(enabled = false)
    public void reassignRPUserGPLIM_1156() {
        changeProjectOwner("namrata", "RP-57");
    }

    @Test(enabled = false)
    public void changeRegulatoryDesignationSUPPORT796() {
        userBean.loginOSUser();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-976");
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);
        rpDao.persist(new FixupCommentary("SUPPORT-796 updating incorrectly selected regulatory designation"));
    }

    @Test(enabled = false)
    public void support1192ChangeRegulatoryDesignationForRp1037() {
        userBean.loginOSUser();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1037");
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        rpDao.persist(new FixupCommentary("SUPPORT-1192 updating incorrectly selected regulatory designation"));
    }

    @Test(enabled = false)
    public void gplim3816ChangeRegulatoryDesignationForRp1040() {
        userBean.loginOSUser();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1040");
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        rpDao.persist(new FixupCommentary("GPLIM-3816 updating incorrectly selected regulatory designation"));
    }

    @Test(enabled = false)
    public void gplim4021addDefaultSubmissionRepository() {
        userBean.loginOSUser();
        List<ResearchProject> researchProjectList = rpDao.findList(ResearchProject.class, ResearchProject_.submissionRepositoryName, null);

        for (ResearchProject project : researchProjectList) {
            if (StringUtils.isNotBlank(project.getSubmissionRepositoryName())) {
                String message = String.format(
                        "Default SubmissionRepository not set for Research Project '%s' Current Value: '%s')",
                        project.getName(), project.getSubmissionRepositoryName());
                log.debug(message);
            } else {
                project.setSubmissionRepositoryName(SubmissionRepository.DEFAULT_REPOSITORY_NAME);
            }
        }
        rpDao.persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4021"));
        log.info(String.format("Updated %d rows", researchProjectList.size()));
    }

    @Test(enabled = false)
    public void fixupGplim4025() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1074");
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
        System.out.println("Changing regulatory designation for " + researchProject.getJiraTicketKey());
        rpDao.persist(new FixupCommentary("GPLIM-4025 changing regulatory designation for RP-1074"));
        rpDao.flush();

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1822() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1227");
        assertThat(researchProject, is(notNullValue()));
        researchProject.setTitle("Takeda Myeloid/Lymphoid v1 Panel Performance Assessment");

        researchProjectEjb.updateJiraIssue(researchProject);
        rpDao.persist(new FixupCommentary("SUPPORT 1822 changing title of RP"));

        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport2534() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1375");
        Assert.assertNotNull(researchProject);
        researchProject.setReferenceSequenceKey(null);
        researchProject.setSequenceAlignerKey(null);
        System.out.println("Setting " + researchProject.getBusinessKey() +
                " reference sequence to " + researchProject.getReferenceSequenceKey() +
                ", aligner to " + researchProject.getSequenceAlignerKey());
        rpDao.persist(new FixupCommentary("SUPPORT-2534 fix RP-1375 for pipeline query"));
        utx.commit();
    }

    @Test(enabled = false)
    public void changeRegulatoryDesignationGplim5031() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        ResearchProject researchProject = rpDao.findByBusinessKey("RP-1467");
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
        rpDao.persist(new FixupCommentary("GPLIM-5031 updating incorrectly selected regulatory designation."));
        utx.commit();
    }


}
