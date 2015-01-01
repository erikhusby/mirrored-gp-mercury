/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.mock.MockRoundtrip;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ResearchProjectRegulatoryInfoActionBeanTest {
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

    public void testValidateProtocolTileBlank() throws Exception {
        actionBean.setRegulatoryInfoAlias("");
        Resolution resolution = actionBean.validateTitle();

        resolution.execute(roundTrip.getRequest(), roundTrip.getResponse());
        Assert.assertEquals(roundTrip.getOutputString(), "Protocol Title is required.");
    }

    public void testValidateProtocolTileTooLong() throws Exception {
        String tooLongString = StringUtils.repeat("x", RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH + 1);
        actionBean.setRegulatoryInfoAlias(tooLongString);
        Resolution resolution = actionBean.validateTitle();

        resolution.execute(roundTrip.getRequest(), roundTrip.getResponse());
        String expectedError =
                String.format("Protocol title exceeds maximum length of 255 with %d.", tooLongString.length());
        Assert.assertEquals(roundTrip.getOutputString(), expectedError);
    }

    public void testValidateProtocolTile() throws Exception {
        String okString = StringUtils.repeat("x", RegulatoryInfo.PROTOCOL_TITLE_MAX_LENGTH);
        actionBean.setRegulatoryInfoAlias(okString);
        Resolution resolution = actionBean.validateTitle();

        resolution.execute(roundTrip.getRequest(), roundTrip.getResponse());
        Assert.assertTrue(StringUtils.isBlank(roundTrip.getOutputString()));
    }
}
