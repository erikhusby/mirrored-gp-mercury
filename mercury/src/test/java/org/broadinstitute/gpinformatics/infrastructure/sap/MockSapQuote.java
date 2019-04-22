/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.sap;

import com.google.common.collect.ArrayListMultimap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.sap.entity.quote.FundingDetail;
import org.broadinstitute.sap.entity.quote.FundingPartner;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteHeader;
import org.broadinstitute.sap.entity.quote.QuoteItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MockSapQuote {
    public static SapQuote newInstance(String quoteId, ProductOrder productOrder, BigDecimal totalOpenOrderValue,
                                           BigDecimal quoteTotal) {
        QuoteItem  quoteItem = Mockito.mock(QuoteItem.class);
        Mockito.when(quoteItem.getQuoteItemNumber()).thenReturn(10);
        Mockito.when(quoteItem.getQuoteNumber()).thenReturn(quoteId);
        String partNumber = productOrder.getProduct().getPartNumber();
        Mockito.when(quoteItem.getMaterialNumber()).thenReturn(partNumber);
        SapQuote sapQuote = Mockito.mock(SapQuote.class);
        ArrayListMultimap<String, QuoteItem> quoteItemMap = ArrayListMultimap.create();
        ArrayListMultimap<String, QuoteItem> quoteItemByDescriptionMap = ArrayListMultimap.create();
        quoteItemMap.put(partNumber, quoteItem);
        quoteItemByDescriptionMap.put(quoteItem.getMaterialDescription(), quoteItem);

        List<String> productAddOns =
            productOrder.getAddOns().stream().map(ProductOrderAddOn::getAddOn).map(Product::getPartNumber).collect(
                Collectors.toList());
        IntStream.range(0, productAddOns.size()).forEach(index -> {
            String partName = productAddOns.get(index);

            QuoteItem  addOnItem = Mockito.mock(QuoteItem.class);
            Mockito.when(addOnItem.getQuoteNumber()).thenReturn(quoteId);
            Mockito.when(addOnItem.getMaterialNumber()).thenReturn(partName);
            Mockito.when(addOnItem.getQuoteItemNumber()).thenReturn(index + 1);
            quoteItemMap.put(partName, addOnItem);
            quoteItemByDescriptionMap.put(addOnItem.getMaterialDescription(), addOnItem);
        });
        FundingDetail mockFundingDetail = Mockito.mock(FundingDetail.class);
        Mockito.when(mockFundingDetail.getFundingStatus()).thenReturn(FundingStatus.APPROVED);
        Mockito.when(sapQuote.getFundingDetails()).thenReturn(Collections.singleton(mockFundingDetail));
        FundingPartner mockFundingPartner = Mockito.mock(FundingPartner.class);
        Mockito.when(sapQuote.getFundingPartners()).thenReturn(Collections.singleton(mockFundingPartner));
        Mockito.when(sapQuote.getQuoteItemMap()).thenReturn(quoteItemMap);
        Mockito.when(sapQuote.getQuoteItemByDescriptionMap()).thenReturn(quoteItemByDescriptionMap);
        QuoteHeader quoteHeader = Mockito.mock(QuoteHeader.class);
        Mockito.when(quoteHeader.getQuoteNumber()).thenReturn(quoteId);
        Mockito.when(sapQuote.getQuoteHeader()).thenReturn(quoteHeader);
        Mockito.when(quoteHeader.getQuoteOpenValue()).thenReturn(totalOpenOrderValue);
        Mockito.when(quoteHeader.getQuoteTotal()).thenReturn(quoteTotal);
        Mockito.when(quoteHeader.getFundingType()).thenReturn(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER);
        Mockito.when(quoteHeader.getFundingHeaderStatus()).thenReturn(FundingStatus.APPROVED);
        Mockito.when(sapQuote.isAllFundingApproved()).thenCallRealMethod();
        return sapQuote;
    }

    public static SapQuote newInstance(String quoteId, ProductOrder testOrder) {
        return newInstance(quoteId, testOrder, null, null);
    }
}
