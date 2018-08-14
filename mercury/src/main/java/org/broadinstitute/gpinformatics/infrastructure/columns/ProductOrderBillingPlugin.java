package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportInfo;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin defined to assist in the display of billing session related data
 */
public class ProductOrderBillingPlugin implements ListPlugin  {
    private static final String BILLING_SESSION_HEADER_KEY = "billingSession";
    private static final String QUOTE_WORK_IDENTIFIER_HEADER = "workId";
    private static final String SAP_DELIVERY_DOCUMENT_HEADER = "sapDeliveryDocument";
    private static final String QUANTITY_HEADER = "quantity";
    private static final String PRODUCT_HEADER = "product";

    private static final Map<String, ConfigurableList.Header> mapTypeToHeader = new HashMap<>();

    private static final String COMPLETE_DATE_HEADER = "completeDate";

    private static final String COMPLETED_HEADER = "completed";

    private static final String BILLED_QUOTE = "billedQuote";

    private static final String BILLED_DATE_HEADER = "billedDate";

    static {
        mapTypeToHeader.put(BILLED_QUOTE,
                new ConfigurableList.Header("Billed Quote",
                        "Billed Quote", ""));
        mapTypeToHeader.put(BILLING_SESSION_HEADER_KEY,
                new ConfigurableList.Header("Billing Session",
                        "Billing Session",
                        ""));
        mapTypeToHeader.put(QUOTE_WORK_IDENTIFIER_HEADER,
                new ConfigurableList.Header("Quote Work ID",
                        "Quote Work ID",
                        ""));
        mapTypeToHeader.put(SAP_DELIVERY_DOCUMENT_HEADER,
                new ConfigurableList.Header("SAP Delivery Document ID",
                        "SAP Delivery Document ID",
                        ""));
        mapTypeToHeader.put(QUANTITY_HEADER,
                new ConfigurableList.Header("Billed Quantity",
                        "Billed Quantity",
                        ""));
        mapTypeToHeader.put(PRODUCT_HEADER,
                new ConfigurableList.Header("Billed Product",
                        "Billed Product",
                        ""));
        mapTypeToHeader.put(COMPLETE_DATE_HEADER,
                new ConfigurableList.Header("Completed Date",
                        "Completed Date",
                        ""));
        mapTypeToHeader.put(COMPLETED_HEADER,
                new ConfigurableList.Header("Work Completed",
                        "Work Completed",
                        ""));
        mapTypeToHeader.put(BILLED_DATE_HEADER,
                new ConfigurableList.Header("Date Billed",
                        "Date Billed", ""));
    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {

        throw new UnsupportedOperationException("Method getData not implemented in ProductOrderBillingPlugin");
    }

    /**
     * Defines the logic for extracting the billing session and ledger information and preparing them for display as
     * a nested table
     * @param entity  The entity, a product order in this case, for which to return any nested table data
     * @param columnTabulation Column definition for the nested table
     * @param context Any required helper objects passed in from callers (e.g. ConfigurableListFactory)
     * @return
     */
    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        ProductOrder productOrder = (ProductOrder) entity;

        List<ConfigurableList.ResultRow> billingRows = new ArrayList<>();

        List<ConfigurableList.Header> headers = new ArrayList<>();
        final Format dateFormatter = FastDateFormat.getInstance(CoreActionBean.DATE_PATTERN);

        headers.add(mapTypeToHeader.get(BILLING_SESSION_HEADER_KEY));
        headers.add(mapTypeToHeader.get(BILLED_DATE_HEADER));
        headers.add(mapTypeToHeader.get(BILLED_QUOTE));
        headers.add(mapTypeToHeader.get(QUOTE_WORK_IDENTIFIER_HEADER));
        headers.add(mapTypeToHeader.get(SAP_DELIVERY_DOCUMENT_HEADER));
        headers.add(mapTypeToHeader.get(PRODUCT_HEADER));
        headers.add(mapTypeToHeader.get(QUANTITY_HEADER));
        headers.add(mapTypeToHeader.get(COMPLETED_HEADER));
        headers.add(mapTypeToHeader.get(COMPLETE_DATE_HEADER));


        Map<BillingSession, QuoteImportInfo> billingAggregator = new HashMap<>();

        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                Optional<BillingSession> billingSession = Optional.of(ledgerEntry.getBillingSession());

                billingSession.ifPresent(billingSession1 ->
                {
                    if(!billingAggregator.containsKey(billingSession1)) {

                        billingAggregator.put(billingSession1, new QuoteImportInfo());
                    }
                    billingAggregator.get(billingSession1).addQuantity(ledgerEntry);

                });
            }
        }

        int count = 0;

        for (Map.Entry<BillingSession, QuoteImportInfo> stringQuoteImportInfoEntry : billingAggregator.entrySet()) {
            BillingSession billingKey = stringQuoteImportInfoEntry.getKey();
            QuoteImportInfo sessionItems = stringQuoteImportInfoEntry.getValue();

            try {

                final List<QuoteImportItem> quoteImportItems =
                        sessionItems.getQuoteImportItems(context.getPriceListCache());

                for (QuoteImportItem quoteImportItem : quoteImportItems) {
                    final Optional<Date> billedDate = Optional.ofNullable(billingKey.getBilledDate());
                    final Optional<Date> workCompleteDate = Optional.ofNullable(quoteImportItem.getWorkCompleteDate());
                    final Optional<String> sapItems = Optional.ofNullable(quoteImportItem.getSapItems());
                    final Optional<String> singleWorkItem = Optional.ofNullable(quoteImportItem.getSingleWorkItem());
                    final List<String> cellList =
                            new ArrayList(Arrays.asList(getBillingSessionLink(billingKey.getBusinessKey(), singleWorkItem.isPresent()?singleWorkItem.get():""),
                                    billedDate.isPresent()?dateFormatter.format(billedDate.get()):"",
                                    getQuoteLink(quoteImportItem.getQuoteId(), context),
                                    getWorkItemLink(singleWorkItem.isPresent()?singleWorkItem.get():"", quoteImportItem.getQuoteId(), context),
                                    sapItems.isPresent()?sapItems.get():"",
                                    quoteImportItem.getProduct().getDisplayName(),
                                    quoteImportItem.getRoundedQuantity(), quoteImportItem.getNumSamples(),
                                    workCompleteDate.isPresent()?dateFormatter.format(workCompleteDate.get()):""));
                    ConfigurableList.ResultRow row =
                            new ConfigurableList.ResultRow(null, cellList, String.valueOf(count));
                    billingRows.add(row);
                    count++;
                }
            } catch (QuoteServerException e) {
                e.printStackTrace();
            }
        }

        ConfigurableList.ResultList resultList = null;
        if(CollectionUtils.isNotEmpty(billingRows)) {
            resultList = new ConfigurableList.ResultList(billingRows, headers, 0, "ASC");
        }

        return resultList;
    }

    private String getQuoteLink(String quoteId, SearchContext context) {
        StringBuffer quoteLink = new StringBuffer("<a class=\"external\" target=\"QUOTE\" href=\"");
        quoteLink.append(context.getQuoteLink().quoteUrl(quoteId));
        quoteLink.append("\">").append(quoteId).append("</a>");
        return quoteLink.toString();
    }

    @NotNull
    public String getWorkItemLink(String workItemId, String quoteId, SearchContext context) {
        StringBuffer workLink = new StringBuffer("<a class=\"external\" target=\"QUOTE\" href=\"");
        workLink.append(context.getQuoteLink().workUrl(quoteId, workItemId));
        workLink.append("\">").append(workItemId).append("</a>");

        return workLink.toString();
    }

    public String getBillingSessionLink(String billingSession, String workItem) {
        final StringBuffer billingSessionFormat = new StringBuffer("<a class=\"external\" target=\"new\" href=\"/Mercury/billing/session.action?billingSession=%s");
        billingSessionFormat.append("&workId=%s");
        billingSessionFormat.append("\">%s</a>");

        return String.format(billingSessionFormat.toString(), billingSession, workItem, billingSession);
    }
}
