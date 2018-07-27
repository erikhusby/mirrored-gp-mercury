package org.broadinstitute.gpinformatics.infrastructure.search;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the configurable search Definition for product order defined search logic
 */
public class ProductOrderSearchDefinition {

    public ConfigurableSearchDefinition buildSearchDefinition() {
        ProductOrderSearchDefinition searchDefinition = new ProductOrderSearchDefinition();
        Map<String, List<SearchTerm>>
                mapGroupSearchTerms = new LinkedHashMap<>();
        
        List<SearchTerm> idSearchTerms = buildIDTerms();
        mapGroupSearchTerms.put("IDs", idSearchTerms);

        List<SearchTerm> pdoSearchTerms = buildPDOSearchTerms();
        mapGroupSearchTerms.put("PDO Terms", pdoSearchTerms);

        List<SearchTerm> billingSearchTerms = buildBillingSearchTerms();
        mapGroupSearchTerms.put("Billing IDs", billingSearchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections =
                new ArrayList<>();
        ConfigurableSearchDefinition productOrderSearchDefinition =
                new ConfigurableSearchDefinition(ColumnEntity.PRODUCT_ORDER, criteriaProjections, mapGroupSearchTerms);

        return productOrderSearchDefinition;
    }

    @NotNull
    private ArrayList<SearchTerm> buildBillingSearchTerms() {
        final ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm billingSessionTerm = new SearchTerm();
        billingSessionTerm.setName("Billing Session Id");
        billingSessionTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBillingSessionConverter());
        SearchTerm.CriteriaPath billingSessionPath = new SearchTerm.CriteriaPath();
        billingSessionPath.setPropertyName("billingSessionId");
        billingSessionPath.setCriteria(Arrays.asList("BillingSessions", "samples", "ledgerItems", "billingSession"));
        billingSessionTerm.setCriteriaPaths(Collections.singletonList(billingSessionPath));
        billingSessionTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                String billingSessionHtmlPostWrap = "";
                String billingSessionHtmlPreWrap = "";

                List<String> billingSessionResults = getBillingSessionDisplay((ProductOrder) entity,
                        billingSessionHtmlPostWrap, billingSessionHtmlPreWrap);

                return billingSessionResults;
            }
        });

        billingSessionTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {

                return StringUtils.join(getBillingSessionDisplay((ProductOrder) entity,
                        "<a class=\"external\" target=\"new\" href=\"/Mercury/billing/session.action?view=&sessionKey=", "\"</a>"), "<br>");
            }
        });
        searchTerms.add(billingSessionTerm);



        return searchTerms;
    }

    @NotNull
    public List<String> getBillingSessionDisplay(ProductOrder entity, String billingSessionHtmlPostWrap,
                                                 String billingSessionHtmlPreWrap) {
        ProductOrder order = entity;
        List<String> billingSessionResults = new ArrayList<>();
        final Multimap<String, String> samplesByBillingSession = LinkedListMultimap.create();

        for (ProductOrderSample productOrderSample : order.getSamples()) {
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                if(ledgerEntry.getBillingSession() != null) {
                    samplesByBillingSession.put(BillingSession.ID_PREFIX + ledgerEntry.getBillingSession(),
                            productOrderSample.getName());
                }
            }
        }

        for (String billingSession : samplesByBillingSession.keys()) {
            billingSessionResults.add(billingSessionHtmlPreWrap + billingSession + billingSessionHtmlPostWrap + "-->("
                                      + StringUtils.join(samplesByBillingSession.get(billingSession), ",") + ")");
        }
        return billingSessionResults;
    }

    @NotNull
    private ArrayList<SearchTerm> buildPDOSearchTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm productTerm = new SearchTerm();
        productTerm.setName("Primary Product Part Number");
        SearchTerm.CriteriaPath productCriteriaPath = new SearchTerm.CriteriaPath();
        productCriteriaPath.setPropertyName("partNumber");
        productCriteriaPath.setCriteria(Arrays.asList("Products", "product"));
        productTerm.setCriteriaPaths(Collections.singletonList(productCriteriaPath));
        productTerm.setDisplayExpression(DisplayExpression.PRIMARY_PDO_PRODUCT);
        searchTerms.add(productTerm);

        //For searching by quotes, and displaying quotes
        SearchTerm quoteTerm = new SearchTerm();
        quoteTerm.setName("Quote Identifier");
        SearchTerm.CriteriaPath quoteCriteraPath = new SearchTerm.CriteriaPath();
        quoteCriteraPath.setPropertyName("quoteId");
        quoteTerm.setCriteriaPaths(Collections.singletonList(quoteCriteraPath));
        quoteTerm.setDisplayExpression(DisplayExpression.PDO_QUOTE);
        searchTerms.add(quoteTerm);


        SearchTerm userIDTerm = new SearchTerm();
        userIDTerm.setName("UserID");
        userIDTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getUserIdConverter());
        SearchTerm.CriteriaPath userIDCriteriaPath = new SearchTerm.CriteriaPath();
        userIDCriteriaPath.setPropertyName("createdBy");
        userIDTerm.setCriteriaPaths(Collections.singletonList(userIDCriteriaPath));
        userIDTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                Optional<BspUser> bspDisplayUser = Optional.of(context.getBspUserList().getById(order.getCreatedBy()));
                StringBuilder userDisplayName = new StringBuilder();
                
                bspDisplayUser.ifPresent(bspUser -> userDisplayName.append(bspUser.getFullName()));

                return userDisplayName.toString();
            }
        });
        searchTerms.add(userIDTerm);

        SearchTerm pdoStatusTerm = new SearchTerm();
        pdoStatusTerm.setName("Order Status");
        pdoStatusTerm.setDisplayExpression(DisplayExpression.ORDER_STATUS);
        searchTerms.add(pdoStatusTerm);

        return searchTerms;
    }

    @NotNull
    private ArrayList<SearchTerm> buildIDTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm pdoJiraTicketTerm = new SearchTerm();
        pdoJiraTicketTerm.setName("PDO Ticket");
        //Necessary for displaying this field in the results
        pdoJiraTicketTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        pdoJiraTicketTerm.setIsDefaultResultColumn(Boolean.TRUE);
        pdoJiraTicketTerm.setDbSortPath("jiraTicketKey");
        SearchTerm.CriteriaPath pdoTicketCriteriaPath = new SearchTerm.CriteriaPath();
        pdoTicketCriteriaPath.setPropertyName("jiraTicketKey");
        pdoJiraTicketTerm.setCriteriaPaths(Collections.singletonList(pdoTicketCriteriaPath));
        pdoJiraTicketTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                return ((ProductOrder) entity).getBusinessKey();
            }
        });
        searchTerms.add(pdoJiraTicketTerm);


        SearchTerm sampleTerm = new SearchTerm();
        sampleTerm.setName("Product Order Sample Id");
        SearchTerm.CriteriaPath sampleCriteriaPath = new SearchTerm.CriteriaPath();
        sampleCriteriaPath.setPropertyName("sampleName");
        sampleCriteriaPath.setCriteria(Arrays.asList("PDOSamples", "samples"));
        sampleTerm.setCriteriaPaths(Collections.singletonList(sampleCriteriaPath));
        sampleTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                ProductOrder order = (ProductOrder) entity;
                Set<String> sampleNames = new HashSet<>();

                for (ProductOrderSample productOrderSample : order.getSamples()) {
                    sampleNames.add(productOrderSample.getName());
                }

                return sampleNames;
            }
        });
        searchTerms.add(sampleTerm);

        SearchTerm sapOrderTerm = new SearchTerm();
        sapOrderTerm.setName("SAP Order Id");
        SearchTerm.CriteriaPath sapCriteriaPath = new SearchTerm.CriteriaPath();
        sapCriteriaPath.setPropertyName("sapOrderNumber");
        sapCriteriaPath.setCriteria(Arrays.asList("SAPOrders", "sapReferenceOrders"));
        sapOrderTerm.setCriteriaPaths(Collections.singletonList(sapCriteriaPath));
        sapOrderTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> sapIdResults = new HashSet<>();
                ProductOrder order = (ProductOrder) entity;
                boolean first = true;
                String currentSapOrderNumber = order.getSapOrderNumber();
                sapIdResults.add("Current Sap order " + currentSapOrderNumber);

                if(order.getSapReferenceOrders().size() >1) {
                    sapIdResults.addAll(order.getSapReferenceOrders().stream().sorted()
                            .filter(sapOrderDetail -> !sapOrderDetail.getSapOrderNumber().equals(currentSapOrderNumber))
                            .map(SapOrderDetail::getSapOrderNumber)
                            .collect(Collectors.toSet()));
                }
                return sapIdResults;
            }
        });
        searchTerms.add(sapOrderTerm);

        return searchTerms;
    }
}
