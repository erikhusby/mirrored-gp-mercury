package org.broadinstitute.gpinformatics.infrastructure.columns;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plugin defined to assist in the display of billing session related data
 */
public class ProductOrderBillingPlugin implements ListPlugin  {
    private static final String billingSessionHeaderKey = "billingSession";
    private static final String quoteWorkIdentifierHeader = "workId";
    private static final String sapDeliveryDocumentHeader = "sapDeliveryDocument";

    private static final Map<String, ConfigurableList.Header> mapTypeToHeader = new HashMap<>();
    static {
        mapTypeToHeader.put(billingSessionHeaderKey,
                new ConfigurableList.Header("Billing Session", "Billing Session", ""));
        mapTypeToHeader.put(quoteWorkIdentifierHeader,
                new ConfigurableList.Header("Quote Work ID(s)", "Quote Work ID(s)", ""));
        mapTypeToHeader.put(sapDeliveryDocumentHeader,
                new ConfigurableList.Header("SAP Delivery Document ID(s)", "SAP Delivery Document ID(s)", ""));
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

//        headers.add(new ConfigurableList.Header("", null, null));
        headers.add(mapTypeToHeader.get(billingSessionHeaderKey));
        headers.add(mapTypeToHeader.get(quoteWorkIdentifierHeader));
        headers.add(mapTypeToHeader.get(sapDeliveryDocumentHeader));


        SetMultimap<String, Pair<String, String>> workAndDeliveryByBIlling = HashMultimap.create();
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                Optional<BillingSession> billingSession = Optional.of(ledgerEntry.getBillingSession());
                Optional<String> workItem = Optional.ofNullable(ledgerEntry.getWorkItem());
                Optional<String> sapDeliveryId = Optional.ofNullable(ledgerEntry.getSapDeliveryDocumentId());

                billingSession.ifPresent(billingSession1 ->
                {
                    Pair<String, String> billingInfo = Pair.of(workItem.orElse(""), sapDeliveryId.orElse(""));
                    workAndDeliveryByBIlling.put(billingSession1.getBusinessKey(), billingInfo);
                });
            }
        }

        int count = 0;

        for (Map.Entry<String, Pair<String, String>> stringPairEntry : workAndDeliveryByBIlling.entries()) {

            Optional<String> workItemId =
                    Optional.ofNullable(stringPairEntry.getValue().getLeft());
            Optional<String> sapDocumentIds =
                    Optional.ofNullable(stringPairEntry.getValue().getRight());

            if(StringUtils.isNotBlank(stringPairEntry.getKey()) ||
                    StringUtils.isNotBlank(workItemId.orElse("")) ||
                    StringUtils.isNotBlank(sapDocumentIds.orElse(""))) {
                final List<String> cellList =
                        new ArrayList(Arrays.asList(getBillingSessionLink(stringPairEntry.getKey(),workItemId.orElse("")),
                                getWorkItemLink(workItemId.orElse("")),
                                sapDocumentIds.orElse("")));
                ConfigurableList.ResultRow row = new ConfigurableList.ResultRow(null,
                        cellList,
                        String.valueOf(count));
                billingRows.add(row);
                count++;
            }
        }
        ConfigurableList.ResultList resultList = null;
        if(CollectionUtils.isNotEmpty(billingRows)) {
            resultList = new ConfigurableList.ResultList(billingRows, headers, 0, "ASC");
        }

        return resultList;
    }

    @NotNull
    public String getWorkItemLink(String workItemId) {
        return workItemId;
    }

    public String getBillingSessionLink(String billingSession, String workItem) {
        final StringBuffer replacementFormat = new StringBuffer("<a class=\"external\" target=\"new\" href=\"/Mercury/billing/session.action?billingSession=%s");
        replacementFormat.append("&workId=%s");
        replacementFormat.append("\">%s</a>");


        return String.format(replacementFormat.toString(), billingSession, workItem, billingSession);
    }
}
