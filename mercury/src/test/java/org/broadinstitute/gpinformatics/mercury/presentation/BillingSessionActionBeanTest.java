package org.broadinstitute.gpinformatics.athena.presentation;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingSessionActionBeanTest {

    private static final String WORK_ITEM_ID = "1234";

    MockRoundtrip roundTrip;

    @BeforeMethod
    public void setUp() {
        roundTrip = StripesMockTestUtils.createMockRoundtrip(BillingSessionActionBean.class, Mockito.mock(BillingSessionDao.class));
    }

    @Test
    public void testLoadingNonNullWorkItemId() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(),equalTo(
                WORK_ITEM_ID));
    }

    @Test
    public void testLoadingNullWorkItemId() throws Exception  {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_URL_PARAMETER,null);
        roundTrip.execute();
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(), is(nullValue()));
    }

    public void testGetNullWorkItemId2() {
        BillingSessionActionBean bean = new BillingSessionActionBean();
        assertThat(bean.getWorkItemId(null), is(nullValue()));
    }

}
