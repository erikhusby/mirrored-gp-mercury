package org.broadinstitute.gpinformatics.athena.entity.project;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ResearchProjectActionBeanTest {

    RegulatoryInfoDao regInfoDao = Mockito.mock(RegulatoryInfoDao.class);
    ResearchProjectDao rpDao = Mockito.mock(ResearchProjectDao.class);
    MockRoundtrip roundTrip;
    ResearchProjectActionBean actionBean;


    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        roundTrip = StripesMockTestUtils
                .createMockRoundtrip(ResearchProjectActionBean.class, regInfoDao, rpDao);
        roundTrip.execute();
        actionBean = roundTrip.getActionBean(ResearchProjectActionBean.class);
    }

    public void testGetSampleInfoFromBsp() throws Exception {

    }


}
