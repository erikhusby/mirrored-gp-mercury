package org.broadinstitute.gpinformatics.infrastructure.columns;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
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

        List<ProductOrder> billedOrderList = (List<ProductOrder>) entityList;
        List<ConfigurableList.Row> billingRows = new ArrayList<>();

        SetMultimap<String, BillingSession> billingSessionsByProductOrder = HashMultimap.create();

        for (ProductOrder productOrder : billedOrderList) {
            ConfigurableList.Row row = new ConfigurableList.Row(productOrder.getProductOrderId().toString());
            SetMultimap<String, String> workItemsByBillingSession = HashMultimap.create();
            SetMultimap<String, String> sapOrdersByBillingSession = HashMultimap.create();
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                    Optional<BillingSession> billingSession = Optional.of(ledgerEntry.getBillingSession());
                    billingSession.ifPresent(billingSession1 ->
                    {
                        billingSessionsByProductOrder.put(productOrder.getBusinessKey(), billingSession1);
                        Optional<String> workItem = Optional.ofNullable(ledgerEntry.getWorkItem());
                        Optional<String> sapDeliveryId = Optional.ofNullable(ledgerEntry.getSapDeliveryDocumentId());
                        workItem.ifPresent(workItemParameter -> workItemsByBillingSession.put(billingSession1.getBusinessKey(),
                                workItemParameter));
                        sapDeliveryId.ifPresent(deliveryDocumentParameter -> sapOrdersByBillingSession.put(billingSession1.getBusinessKey(),
                                deliveryDocumentParameter));
                    });
                }
            }

            if(!billingSessionsByProductOrder.containsKey(productOrder.getBusinessKey())) {
                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(billingSessionHeaderKey),
                        "", ""));
                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(quoteWorkIdentifierHeader),
                        "", ""));
                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(sapDeliveryDocumentHeader),
                        "", ""));
            }
            for (BillingSession billingSession : billingSessionsByProductOrder.get(productOrder.getBusinessKey())) {
                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(billingSessionHeaderKey),
                        billingSession.getBusinessKey(), billingSession.getBusinessKey()));

                Optional<Set<String>> workItemIds = Optional.ofNullable(workItemsByBillingSession.get(billingSession.getBusinessKey()));
                Optional<Set<String>> sapDocumentIds = Optional.ofNullable(sapOrdersByBillingSession.get(billingSession.getBusinessKey()));

                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(quoteWorkIdentifierHeader),
                        StringUtils.join(workItemIds.orElse(Collections.singleton("")), ","),
                        StringUtils.join(Collections.singleton(""), ",")));
                row.addCell(new ConfigurableList.Cell(mapTypeToHeader.get(sapDeliveryDocumentHeader),
                        StringUtils.join(sapDocumentIds.orElse(Collections.singleton("")),","),
                        StringUtils.join(Collections.singleton(""),",")));

                billingRows.add(row);
            }
        }

        return billingRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in ProductOrderBillingPlugin");
    }
}
