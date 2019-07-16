package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.FIXUP)
public class RegulatoryInfoFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Inject
    private UserTransaction utx;

    /**
     * Change the identifier for IRB 1107004579 to ORSP-783, which is a reference to IRB 1107004579.
     */
    @Test(enabled = false)
    public void gplim3765FixRegulatoryInfoForOrsp783() {
        userBean.loginOSUser();

        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("1107004579");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo regulatoryInfo = regulatoryInfos.get(0);
        assertThat(regulatoryInfo.getType(), equalTo(RegulatoryInfo.Type.IRB));

        regulatoryInfo.setIdentifier("ORSP-783");

        regulatoryInfoDao.persist(new FixupCommentary(
                "GPLIM-3765 Renaming RegulatoryInfo for IRB 1107004579 to ORSP-783 (which is a reference to IRB 1107004579)"));
    }

    @Test(enabled = false)
    public void support1823ChangeRegulatoryInfoForOrsp3569() {
        userBean.loginOSUser();

        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-3569");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo regulatoryInfo = regulatoryInfos.get(0);
        assertThat(regulatoryInfo.getType(), equalTo(RegulatoryInfo.Type.IRB));

        regulatoryInfo.setType(RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH);

        regulatoryInfoDao.persist(new FixupCommentary("Support-1823 change type to Not Human Subjects Research"));

    }

    @Test(enabled = false)
    public void fixupSUPPORT_5356_SUPPORT_5357() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        String incorrectOrspIdentifier = "2169";
        String correctOrspIdentifier = "ORSP-2169";

        RegulatoryInfo orspToReplace =
            regulatoryInfoDao.findByIdentifierAndType(incorrectOrspIdentifier, RegulatoryInfo.Type.IRB);

        RegulatoryInfo orsp2169 =
            regulatoryInfoDao.findByIdentifierAndType(correctOrspIdentifier, RegulatoryInfo.Type.IRB);

        orspToReplace.getResearchProjects().forEach(researchProject -> researchProject.addRegulatoryInfo(orsp2169));

        regulatoryInfoDao.persist(new FixupCommentary("SUPPORT-5356 SUPPORT-5357 add ORSP-2169 to projects."));
        utx.commit();
    }


    @Test(enabled = false)
    public void gplim6444UpdateRegulatoryInfoType() throws Exception {
        userBean.loginOSUser();

        List<RegulatoryInfo> regulatoryInfos = regulatoryInfoDao.findByIdentifier("ORSP-3763");
        assertThat(regulatoryInfos, hasSize(1));
        RegulatoryInfo regulatoryInfo = regulatoryInfos.get(0);
        assertThat(regulatoryInfo.getType(), equalTo(RegulatoryInfo.Type.IRB));

        regulatoryInfo.setType(RegulatoryInfo.Type.ORSP_NOT_ENGAGED);
        System.out.println("Updated the type for ORSP-3763 to ORSP Not Engaged");

        regulatoryInfoDao.persist(new FixupCommentary("GPLIM-6444 change type for ORSP-3763 to Not Engaged"));

    }
}
