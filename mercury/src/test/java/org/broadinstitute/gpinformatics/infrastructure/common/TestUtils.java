package org.broadinstitute.gpinformatics.infrastructure.common;

import functions.rfc.sap.document.sap_com.ZESDFUNDINGDET;
import functions.rfc.sap.document.sap_com.ZESDQUOTEHEADER;
import functions.rfc.sap.document.sap_com.ZESDQUOTEITEM;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.sap.entity.material.SAPChangeMaterial;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.FundingDetail;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteHeader;
import org.broadinstitute.sap.entity.quote.QuoteItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Methods useful when testing.
 */
public class TestUtils {
    /**
     * The location of where test data is stored.
     */
    public static final String TEST_DATA_LOCATION = "src/test/resources/testdata";

    /**
     * This method returns the full path to, and including the specified file name.
     *
     * @param fileName the name file which you seek
     *
     * @return the full path to fileName.
     */
    public static String getTestData(String fileName) {
        return TEST_DATA_LOCATION + "/" + fileName;
    }

    /**
     * Convenience method to return the first item in a collection. This method will return null
     * if the collection is empty.
     *
     * @return The first item in the collection or null if it is empty.
     */
    @Nullable
    public static <T> T getFirst(@Nonnull final Collection<T> collection) {
        if (!collection.isEmpty()) {
            return collection.iterator().next();
        }
        return null;
    }

    @NotNull
    public static SapQuote buildTestSapQuote(String testQuoteIdentifier, double totalOpenOrderValue,
                                             double quoteTotal, ProductOrder billingOrder,
                                             SapQuoteTestScenario quoteTestScenario, String salesorg)
            throws SAPIntegrationException {

        billingOrder.setQuoteId(testQuoteIdentifier);

        ZESDQUOTEHEADER sapQHeader = ZESDQUOTEHEADER.Factory.newInstance();
        sapQHeader.setPROJECTNAME("TestProject");
        sapQHeader.setQUOTENAME(testQuoteIdentifier);
        sapQHeader.setQUOTESTATUS(FundingStatus.APPROVED.name());
        sapQHeader.setSALESORG(salesorg);
        sapQHeader.setQUOTATION(testQuoteIdentifier);
        sapQHeader.setFUNDHEADERSTATUS(FundingStatus.APPROVED.name());
        sapQHeader.setCUSTOMER("");
        sapQHeader.setDISTCHANNEL("GE");
        sapQHeader.setFUNDTYPE(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.name());
        sapQHeader.setQUOTESTATUSTXT("");
        sapQHeader.setQUOTETOTAL(BigDecimal.valueOf(quoteTotal));
        sapQHeader.setSOTOTAL(BigDecimal.valueOf(totalOpenOrderValue));
        sapQHeader.setQUOTEOPENVAL(BigDecimal.valueOf(quoteTotal).subtract(BigDecimal.valueOf(totalOpenOrderValue)));

        QuoteHeader header = new QuoteHeader(sapQHeader);

        final Set<QuoteItem> quoteItems = new HashSet<>();

        final List<Product> allProductsOrdered = ProductOrder.getAllProductsOrdered(billingOrder);
        switch(quoteTestScenario) {
        case PRODUCTS_MATCH_QUOTE_ITEMS:

            allProductsOrdered.forEach(product -> {
                ZESDQUOTEITEM sapItem = ZESDQUOTEITEM.Factory.newInstance();
                sapItem.setMAKTX(product.getProductName());
                sapItem.setMATNR(product.getPartNumber());
                sapItem.setQUOTEITEM(String.valueOf((quoteItems.size() + 1) * 10));
                sapItem.setQUOTATION(testQuoteIdentifier);

                quoteItems.add(new QuoteItem(sapItem));
            });
            break;
        case DOLLAR_LIMITED:
            ZESDQUOTEITEM sapItem = ZESDQUOTEITEM.Factory.newInstance();
            sapItem.setMAKTX((salesorg.equals("GP01"))?"GP01 Generic Material-Dollar Limited":"GP02 Generic Material-Dollar Limited");
            sapItem.setMATNR("GP01-001");
            sapItem.setQUOTEITEM(String.valueOf((quoteItems.size() + 1) * 10));
            sapItem.setQUOTATION(testQuoteIdentifier);

            quoteItems.add(new QuoteItem(sapItem));
            break;
        case PRODUCTS_DIFFER:
            allProductsOrdered.forEach(product -> {
                ZESDQUOTEITEM sapItemDiffering = ZESDQUOTEITEM.Factory.newInstance();
                sapItemDiffering.setMAKTX(product.getProductName());
                sapItemDiffering.setMATNR(product.getPartNumber()+"mismatch");
                sapItemDiffering.setQUOTEITEM(String.valueOf((quoteItems.size() + 1) * 10));
                sapItemDiffering.setQUOTATION(testQuoteIdentifier);

                quoteItems.add(new QuoteItem(sapItemDiffering));
            });

            break;
        case MATCH_QUOTE_ITEMS_AND_DOLLAR_LIMITED:
            ZESDQUOTEITEM item = ZESDQUOTEITEM.Factory.newInstance();
            item.setMAKTX(QuoteItem.DOLLAR_LIMIT_MATERIAL_DESCRIPTOR);
            item.setMATNR("GP01-001");
            item.setQUOTEITEM(String.valueOf((quoteItems.size() + 1) * 10));
            item.setQUOTATION(testQuoteIdentifier);

            quoteItems.add(new QuoteItem(item));
            allProductsOrdered.forEach(product -> {
                ZESDQUOTEITEM lambdaItem = ZESDQUOTEITEM.Factory.newInstance();
                lambdaItem.setMAKTX(product.getProductName());
                lambdaItem.setMATNR(product.getPartNumber());
                lambdaItem.setQUOTEITEM(String.valueOf((quoteItems.size() + 1) * 10));
                lambdaItem.setQUOTATION(testQuoteIdentifier);

                quoteItems.add(new QuoteItem(lambdaItem));
            });
            break;
        }

        final Set<FundingDetail> fundingDetailsCollection = new HashSet<>();

        ZESDFUNDINGDET sapFundDetail = ZESDFUNDINGDET.Factory.newInstance();
        sapFundDetail.setFUNDTYPE(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.name());
        sapFundDetail.setSPLITPER(BigDecimal.valueOf(100));
        sapFundDetail.setAPPSTATUS(FundingStatus.APPROVED.name());
        sapFundDetail.setAUTHAMOUNT(BigDecimal.valueOf(100));
        sapFundDetail.setPONUMBER("1234");
        sapFundDetail.setITEMNO("1234");

        fundingDetailsCollection.add(new FundingDetail(sapFundDetail));

        final SapQuote sapQuote = new SapQuote(header, fundingDetailsCollection, Collections.emptySet(), quoteItems);
        try {
            billingOrder.updateQuoteItems(sapQuote);
        } catch (SAPInterfaceException e) {

        }
        billingOrder.setQuoteSource(ProductOrder.QuoteSourceType.SAP_SOURCE);
        return sapQuote;
    }

    public static SAPMaterial mockMaterialSearch(SapIntegrationClientImpl.SAPCompanyConfiguration copmanyConfig,
                                                 Product testProduct) {
        SAPMaterial otherPlatformMaterial =
                new SAPChangeMaterial(testProduct.getPartNumber(), copmanyConfig, copmanyConfig.getDefaultWbs(),
                        testProduct.getName(), "50", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                        "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                        testProduct.determineCompanyConfiguration().getSalesOrganization());
        return otherPlatformMaterial;
    }

    // This is a utility method and NOT a test method.  Will FAIL with arguments as it should.
    public static void billSampleOut(ProductOrder productOrder, ProductOrderSample sample, int expected) {

        billSamplesOut(productOrder, Collections.singleton(sample), expected);

    }

    public static void billSamplesOut(ProductOrder productOrder, Collection<ProductOrderSample> samples, int expected) {
        BillingSession billingSession = null;
        for (ProductOrderSample sample : samples) {
            LedgerEntry primaryItemSampleEntry = new LedgerEntry(sample,
                    productOrder.getProduct().getPrimaryPriceItem(), new Date(), /*productOrder.getProduct(),*/ 1);
            primaryItemSampleEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);

            LedgerEntry addonItemSampleEntry = new LedgerEntry(sample,
                    productOrder.getAddOns().iterator().next().getAddOn().getPrimaryPriceItem(),
                    new Date(), /*productOrder.getProduct(),*/ 1);
            addonItemSampleEntry.setPriceItemType(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
            sample.getLedgerItems().add(primaryItemSampleEntry);
            sample.getLedgerItems().add(addonItemSampleEntry);

            Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected);

            billingSession = new BillingSession(4L, sample.getLedgerItems());
        }

        Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected);

        for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
            ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        }

        Assert.assertEquals(productOrder.getUnbilledSampleCount(), expected-samples.size());
        billingSession.setBilledDate(new Date());
        if(productOrder.isSavedInSAP()) {
            productOrder.latestSapOrderDetail().addLedgerEntries(billingSession.getLedgerEntryItems());
        }
    }

    public enum SapQuoteTestScenario {
       PRODUCTS_MATCH_QUOTE_ITEMS, DOLLAR_LIMITED, MATCH_QUOTE_ITEMS_AND_DOLLAR_LIMITED, PRODUCTS_DIFFER;
    }
}
