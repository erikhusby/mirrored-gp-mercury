package org.broadinstitute.gpinformatics.athena.presentation;

import net.sourceforge.stripes.mock.MockRoundtrip;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingSessionActionBeanTest {

    private static final String WORK_ITEM_ID = "1234";
    private final String BILLING_SESSION_ID = "BILL-123";

    MockRoundtrip roundTrip;

    @BeforeMethod
    public void setUp() {
        roundTrip = StripesMockTestUtils.createMockRoundtrip(BillingSessionActionBean.class, Mockito.mock(BillingSessionDao.class));
    }

    @Test
    public void testLoadingNonNullWorkItemId() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(),equalTo(
                WORK_ITEM_ID));
    }

    @Test
    public void testInitDoesntLoseSession() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.SESSION_KEY_PARAMETER_NAME,BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getSessionKey(),equalTo(
                BILLING_SESSION_ID));
    }

    @Test
    public void testLoadingNullWorkItemId() throws Exception  {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER,null);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(), is(nullValue()));
    }

    @Test
    public void testRedirectFromQuoteServer() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute();
        // explicit checks on the redirect url
        assertThat(roundTrip.getRedirectUrl(), containsString(
                BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER + "=" + WORK_ITEM_ID));
        assertThat(roundTrip.getRedirectUrl(),containsString(
                BillingSessionActionBean.SESSION_KEY_PARAMETER_NAME + "=" + BILLING_SESSION_ID));
    }

    @Test
    public void testWorkItemGetter() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(),equalTo(
                WORK_ITEM_ID));
    }

}
