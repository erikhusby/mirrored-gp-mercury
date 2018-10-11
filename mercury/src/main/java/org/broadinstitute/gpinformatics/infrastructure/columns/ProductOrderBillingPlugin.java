package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
    private static final String BILLING_MESSAGE_HEADER = "billingMsg";

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
        mapTypeToHeader.put(BILLING_MESSAGE_HEADER,
                new ConfigurableList.Header("Billing Message",
                        "Billing Message",
                        ""));
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
        headers.add(mapTypeToHeader.get(BILLING_MESSAGE_HEADER));

        Map<Optional<BillingSession>, QuoteImportInfo> billingAggregator = new HashMap<>();

        // Using the same method of aggregating Billing ledgers as when creating a billing session, this following
        // loops through all ledger entries of all samples and aggregates them by the billing session with which
        // they are associated
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                Optional<BillingSession> billingSession = Optional.ofNullable(ledgerEntry.getBillingSession());

                    if(!billingAggregator.containsKey(billingSession)) {

                        billingAggregator.put(billingSession, new QuoteImportInfo());
                    }
                    billingAggregator.get(billingSession).addQuantity(ledgerEntry);

            }
        }

        int count = 0;

        // Takes the aggregated billing ledger info and displays them in a similar manner to the billing session  
        for (Map.Entry<Optional<BillingSession>, QuoteImportInfo> stringQuoteImportInfoEntry : billingAggregator.entrySet()) {
            Optional<BillingSession> billingKey = stringQuoteImportInfoEntry.getKey();
            QuoteImportInfo sessionItems = stringQuoteImportInfoEntry.getValue();

            try {

                final List<QuoteImportItem> quoteImportItems =
                        sessionItems.getQuoteImportItems(context.getPriceListCache());

                for (QuoteImportItem quoteImportItem : quoteImportItems) {
                    Optional<Date> billedDate = Optional.empty();
                    if(billingKey.isPresent()) {
                        billedDate = Optional.ofNullable(billingKey.get().getBilledDate());
                    }
                    final Optional<Date> workCompleteDate = Optional.ofNullable(quoteImportItem.getWorkCompleteDate());
                    final Optional<String> sapItems = Optional.ofNullable(quoteImportItem.getSapItems());
                    final Optional<String> singleWorkItem = Optional.ofNullable(quoteImportItem.getSingleWorkItem());
                    String businessKey = "";
                    if(billingKey.isPresent()) {
                        businessKey = billingKey.get().getBusinessKey();
                    }

                    final List<String> cellList =
                            new ArrayList(Arrays.asList(getBillingSessionLink(
                                    businessKey, singleWorkItem.isPresent()?singleWorkItem.get():"", context),
                                    billedDate.isPresent()?dateFormatter.format(billedDate.get()):"",
                                    getQuoteLink(quoteImportItem.getQuoteId(), context),
                                    getWorkItemLink(singleWorkItem.isPresent()?singleWorkItem.get():"", quoteImportItem.getQuoteId(), context),
                                    sapItems.isPresent()?sapItems.get():"",
                                    quoteImportItem.getProduct().getDisplayName(),
                                    quoteImportItem.getRoundedQuantity(), quoteImportItem.getNumSamples(),
                                    workCompleteDate.isPresent()?dateFormatter.format(workCompleteDate.get()):"",
                                    quoteImportItem.getBillingMessage()));
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

    /**
     * Creates a link to the definition of the quote on the quote server
     * @param quoteId Unique identifier of the quote as it is found in the Quote server
     * @param context Search context object which contains injectable services that typically cannot be injected
     *                through the search process
     * @return Anchor link to the quote definition on the quote server
     */
    private String getQuoteLink(String quoteId, SearchContext context) {

        StringBuffer quoteLink = new StringBuffer();
        if(StringUtils.isNotBlank(quoteId)) {
            if(context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB) {
                quoteLink.append("<a class=\"external\" target=\"QUOTE\" href=\"");
                quoteLink.append(context.getQuoteLink().quoteUrl(quoteId));
                quoteLink.append("\">").append(quoteId).append("</a>");
            } else {
                quoteLink.append(quoteId);
            }
        }
        return quoteLink.toString();
    }

    /**
     * Helper method to create a link to the specific quote server work item detail
     * @param workItemId ID of the work item in question under the given quotes work tab
     * @param quoteId Unique identifier of the quote as it is found in the Quote server
     * @param context Search context object which contains injectable services that typically cannot be injected
     *                through the search process
     * @return
     */
    private String getWorkItemLink(String workItemId, String quoteId, SearchContext context) {
        StringBuffer workLink = new StringBuffer();
        final boolean webDisplay = StringUtils.isNotBlank(workItemId) &&
                          context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB;
        if(webDisplay) {
            workLink.append("<a class=\"external\" target=\"QUOTE\" href=\"");
            workLink.append(context.getQuoteLink().workUrl(quoteId, workItemId));
            workLink.append("\">");
        }
        workLink.append(workItemId);
        if(webDisplay) {
            if(context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB) {
                workLink.append("</a>");
            }
        }

        return workLink.toString();
    }

    /**
     * Helper method to create a link to the definition of the billing session in Mercury
     * @param billingSession Unique business key for the billing session for which the generated link will be
     *                     associated
     * @param workItem Quote service work item with which the billing session aggregation is associated
     * @param context
     * @return
     */
    private String getBillingSessionLink(String billingSession, String workItem,
                                         SearchContext context) {
        String billingSessionFormat =
                "<a class=\"external\" target=\"new\" href=\"/Mercury/billing/session.action?billingSession=%s"
                + "&workId=%s" +
                "\">%s</a>";

        String formattedOutput = "";
        if(StringUtils.isNotBlank(billingSession)) {
            if(context.getResultCellTargetPlatform() == SearchContext.ResultCellTargetPlatform.WEB) {
                formattedOutput = String.format(billingSessionFormat, billingSession, workItem, billingSession);
            } else {
                formattedOutput = billingSession;
            }
        }
        return formattedOutput;
    }
}
