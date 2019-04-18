package org.broadinstitute.gpinformatics.infrastructure.common;

import functions.rfc.sap.document.sap_com.ZESDFUNDINGDET;
import functions.rfc.sap.document.sap_com.ZESDQUOTEHEADER;
import functions.rfc.sap.document.sap_com.ZESDQUOTEITEM;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.sap.entity.quote.FundingDetail;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteHeader;
import org.broadinstitute.sap.entity.quote.QuoteItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
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
    public static SapQuote buildTestSapQuote(String testQuoteIdentifier, BigDecimal totalOpenOrderValue,
                                             BigDecimal quoteTotal, ProductOrder billingOrder,
                                             SapQuoteTestScenario quoteTestScenario)
            throws SAPIntegrationException {

        ZESDQUOTEHEADER sapQHeader = ZESDQUOTEHEADER.Factory.newInstance();
        sapQHeader.setPROJECTNAME("TestProject");
        sapQHeader.setQUOTENAME(testQuoteIdentifier);
        sapQHeader.setQUOTESTATUS(FundingStatus.APPROVED.name());
        sapQHeader.setSALESORG("GP01");
        sapQHeader.setFUNDHEADERSTATUS(FundingStatus.APPROVED.name());
        sapQHeader.setCUSTOMER("");
        sapQHeader.setDISTCHANNEL("GE");
        sapQHeader.setFUNDTYPE(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.name());
        sapQHeader.setQUOTESTATUSTXT("");
        sapQHeader.setQUOTETOTAL(quoteTotal);
        sapQHeader.setSOTOTAL(totalOpenOrderValue);
        sapQHeader.setQUOTEOPENVAL(quoteTotal.subtract(totalOpenOrderValue));

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
            sapItem.setMAKTX(QuoteItem.DOLLAR_LIMIT_MATERIAL_DESCRIPTOR);
            sapItem.setMATNR("GP-001");
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

        return new SapQuote(header, fundingDetailsCollection, Collections.emptySet(), quoteItems);
    }

    public enum SapQuoteTestScenario {
       PRODUCTS_MATCH_QUOTE_ITEMS, DOLLAR_LIMITED, PRODUCTS_DIFFER;
    }
}
