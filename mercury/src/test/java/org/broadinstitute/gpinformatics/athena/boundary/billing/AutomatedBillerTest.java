/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.BillingRequirement;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.inject.spi.BeanManager;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class AutomatedBillerTest {

    public static final String TEST_PDO = "PDO-1234";
    public static final long USER_ID = 100L;
    private WorkCompleteMessageDao workCompleteMessageDao = Mockito.mock(WorkCompleteMessageDao.class);
    private AutomatedBiller automatedBiller;
    private BillingSessionDao billingSessionDao = Mockito.mock(BillingSessionDao.class);
    private ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
    ;
    private SessionContextUtility sessionContextUtility = new SessionContextUtility(Mockito.mock(
        BoundSessionContext.class), Mockito.mock(BeanManager.class));
    private LedgerEntryDao ledgerEntryDao = Mockito.mock(LedgerEntryDao.class);
    private BillingSessionAccessEjb billingSessionAccessEjb;
    private BillingAdaptor billingAdaptor;
    private SapIntegrationService sapService;

    @BeforeMethod
    public void setUp() {

        BillingEjb billingEjb = Mockito.spy(
            new BillingEjb(priceListCache, billingSessionDao, productOrderDao, ledgerEntryDao, null, AppConfig
                .produce(Deployment.DEV), SapConfig.produce(Deployment.DEV), null, null, bspUserList, null));
        billingSessionAccessEjb = Mockito.mock(BillingSessionAccessEjb.class);
        sapService = Mockito.mock(SapIntegrationService.class);
        billingAdaptor = new BillingAdaptor(billingEjb, null, null, billingSessionAccessEjb, sapService, null, null);
        ProductOrderEjb productOrderEjb = Mockito.mock(ProductOrderEjb.class);

        billingAdaptor.setProductOrderEjb(productOrderEjb);
        billingEjb.setBillingAdaptor(billingAdaptor);
        billingEjb.setBillingSessionAccessEjb(billingSessionAccessEjb);

        automatedBiller = new AutomatedBiller(workCompleteMessageDao, billingEjb, productOrderDao, sessionContextUtility);
    }

    private SapQuote buildSapQuote(ProductOrder productOrder) throws SAPIntegrationException {
        return TestUtils.buildTestSapQuote("1234", BigDecimal.ONE, BigDecimal.TEN, productOrder,
            TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS, "foo");
    }

    public void testProcessMessagesBillPrimary() throws Exception {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, TEST_PDO);
        Product product = productOrder.getProduct();
        BillingRequirement billingRequirement = new BillingRequirement("CAN_BILL", Operator.EQUALS, 1.0);
        product.addRequirement(billingRequirement);

        setupForBilling(productOrder);
        automatedBiller.processMessages();
        List<Set<LedgerEntry>> ledgers =
            productOrder.getSamples().stream().map(ProductOrderSample::getLedgerItems).collect(Collectors.toList());
        assertThat(ledgers, hasSize(1));
    }

    public void testProcessMessagesBillAddOn() throws SAPIntegrationException {
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, TEST_PDO);
        Product addOn = ProductTestFactory.createDummyProduct(Workflow.NONE, "ADD_ON");
        BillingRequirement billingRequirement = new BillingRequirement("CAN_BILL", Operator.EQUALS, 1.0);
        addOn.addRequirement(billingRequirement);
        assertThat(productOrder.getProduct().getRequirement().getAttribute(), emptyOrNullString());
        productOrder.getProduct().addAddOn(addOn);
        productOrder.setProductOrderAddOns(Collections.singletonList(new ProductOrderAddOn(addOn, productOrder)));

        setupForBilling(productOrder);
        automatedBiller.processMessages();
        List<LedgerEntry> ledgers = productOrder.getSamples().stream()
            .flatMap(productOrderSample -> productOrderSample.getLedgerItems().stream()).collect(Collectors.toList());
        assertThat(ledgers.stream().map(LedgerEntry::getBillingMessage).findFirst().orElse(null),
            not(emptyOrNullString()));

    }

    private void setupForBilling(ProductOrder productOrder) throws SAPIntegrationException {

        Map<String, Object> dataMap = new HashMap<String, Object>() {{
            put("USER_ID", USER_ID);
            put("AUTO_LEDGER", 1D);
            put("CAN_BILL", 1D);
        }};
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder.setQuoteId("01234");
        SapOrderDetail sapOrderDetail = new SapOrderDetail("812341234", 1, "01234",
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        productOrder.addSapOrderDetail(sapOrderDetail);

        Mockito.when(sapService.findSapQuote(Mockito.anyString())).thenReturn(buildSapQuote(productOrder));
        productOrder.getSamples()
            .forEach(productOrderSample -> productOrderSample.setAliquotId(productOrderSample.getSampleKey()));
        Product product = productOrder.getProduct();
        String billProduct;
        Optional<String> attribute = Optional.ofNullable(productOrder.getProduct().getRequirement().getAttribute());
        boolean canBill = false;
        if (attribute.isPresent()) {
            canBill = attribute.get().equals("CAN_BILL");
        }

        if (canBill) {
            product.setUseAutomatedBilling(true);
            billProduct = product.getPartNumber();
        } else {
            billProduct = productOrder.getAddOns().stream().map(ProductOrderAddOn::getAddOn).map(p -> {
                p.setUseAutomatedBilling(true);
                return p.getPartNumber();
            }).findFirst().orElse("");
        }
        WorkCompleteMessage workCompleteMessage =
            new WorkCompleteMessage(TEST_PDO, productOrder.getSamples().iterator().next().getAliquotId(),
                billProduct, USER_ID, new Date(), dataMap);

        Mockito.when(workCompleteMessageDao.getNewMessages())
            .thenReturn(Collections.singletonList(workCompleteMessage));
        Mockito.when(productOrderDao.findByBusinessKey(Mockito.anyString())).thenReturn(productOrder);
        Mockito.when(ledgerEntryDao.findUploadedUnbilledOrderList(Mockito.any()))
            .thenReturn(Collections.emptySet());
        Mockito.when(ledgerEntryDao
            .findWithoutBillingSessionByOrderList(Mockito.anyListOf(String.class), Mockito.anyListOf(String.class)))
            .thenCallRealMethod();

        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString()))
            .thenAnswer(invocation -> new BillingSession(USER_ID, productOrder.getSamples().stream()
                .flatMap(productOrderSample -> productOrderSample.getLedgerItems().stream())
                .collect(Collectors.toSet())));

        Mockito.when(ledgerEntryDao.findByOrderList(Mockito.anyListOf(String.class), Mockito.any(
            LedgerEntryDao.BillingSessionInclusion.class))).thenAnswer(invocation -> productOrder.getSamples().stream()
                .flatMap(productOrderSample -> productOrderSample.getLedgerItems().stream())
                .collect(Collectors.toSet()));
    }
}
