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
import org.broadinstitute.gpinformatics.infrastructure.columns.ProductOrderBillingPlugin;
import org.broadinstitute.gpinformatics.infrastructure.presentation.JiraLink;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("BillingSessions",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("Products",
                "productOrderId", "product", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDOSamples",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("SAPOrders",
                "productOrderId", "sapReferenceOrders", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("BatchVessels",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("QuoteWOrk",
                "productOrderId", "samples", ProductOrder.class));

        return new ConfigurableSearchDefinition(ColumnEntity.PRODUCT_ORDER, criteriaProjections, mapGroupSearchTerms);
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
        billingSessionTerm.setIsNestedParent(Boolean.TRUE);
        billingSessionTerm.setPluginClass(ProductOrderBillingPlugin.class);
        searchTerms.add(billingSessionTerm);


        SearchTerm quoteWorkTerm = new SearchTerm();
        quoteWorkTerm.setName("Quote Work Item");
        SearchTerm.CriteriaPath quoteWorkPath = new SearchTerm.CriteriaPath();
        quoteWorkPath.setPropertyName("workItem");
        quoteWorkPath.setCriteria(Arrays.asList("QuoteWork", "samples", "ledgerItems"));
        quoteWorkTerm.setCriteriaPaths(Collections.singletonList(quoteWorkPath));
        quoteWorkTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                Set<String> workItems = new HashSet<>();

                for (ProductOrderSample productOrderSample : order.getSamples()) {
                    for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                        Optional<String> workItem = Optional.ofNullable(ledgerEntry.getWorkItem());
                        Optional<BillingSession> billingSession = Optional.ofNullable(ledgerEntry.getBillingSession());

                        workItem.ifPresent(workItemParam -> {

                                    StringBuffer workItemOutput = new StringBuffer();
                                    billingSession.ifPresent(
                                            billingSession1 -> workItemOutput.append(billingSession1.getBusinessKey())
                                                    .append("-->"));
                                    workItemOutput.append(workItemParam);
                                    workItems.add(workItemOutput.toString());
                                }
                        );
                    }

                }
                return workItems;
            }
        });
        return searchTerms;
    }

    /**
     * Encapsulates the logic to collect and aggregate Billing sessions with
     * @param order
     * @return
     */
    @NotNull
    private List<String> getBillingSessionDisplay(ProductOrder order) {
        List<String> billingSessionResults = new ArrayList<>();
        final Multimap<BillingSession, String> samplesByBillingSession = LinkedListMultimap.create();

        for (ProductOrderSample productOrderSample : order.getSamples()) {
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                if(ledgerEntry.getBillingSession() != null) {
                    samplesByBillingSession.put(ledgerEntry.getBillingSession(),
                            productOrderSample.getName());
                }
            }
        }

        for (BillingSession billingSession : samplesByBillingSession.keys()) {
            billingSessionResults.add(billingSession.getBusinessKey() + "-->("
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
        productTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder) entity;
                return orderData.getProduct().getDisplayName();
            }
        });
        searchTerms.add(productTerm);

        //For searching by quotes, and displaying quotes
        SearchTerm quoteTerm = new SearchTerm();
        quoteTerm.setName("Quote Identifier");
        SearchTerm.CriteriaPath quoteCriteraPath = new SearchTerm.CriteriaPath();
        quoteCriteraPath.setPropertyName("quoteId");
        quoteTerm.setCriteriaPaths(Collections.singletonList(quoteCriteraPath));
        quoteTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder) entity;

                return orderData.getQuoteId();
            }
        });
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
        pdoStatusTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder)entity;
                return orderData.getOrderStatus().getDisplayName();
            }
        });
        searchTerms.add(pdoStatusTerm);

        SearchTerm rpSearchTerm = new SearchTerm();
        rpSearchTerm.setName("Research Project ID");
        SearchTerm.CriteriaPath rpCriteriaPath = new SearchTerm.CriteriaPath();
        rpCriteriaPath.setPropertyName("jiraTicketKey");
        rpCriteriaPath.setCriteria(Arrays.asList("ResearchProject", "researchProject"));
        rpSearchTerm.setCriteriaPaths(Collections.singletonList(rpCriteriaPath));
        rpSearchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                return order.getResearchProject().getJiraTicketKey();
            }
        });
        rpSearchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String output = (String) entity;
                return "<a class=\"external\" target=\"new\" href=\"/Mercury/projects/project.action?view=&researchProject="
                       + output +"\">"+ output +"</a>";
            }
        });
        searchTerms.add(rpSearchTerm);

        SearchTerm lcsetTerm = new SearchTerm();
        lcsetTerm.setName("LCSET");
        lcsetTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());
        List<SearchTerm.CriteriaPath> lcsetPathList = new ArrayList<>();

        SearchTerm.CriteriaPath lcsetVesselPath = new SearchTerm.CriteriaPath();
        SearchTerm.CriteriaPath lcsetReworkPath = new SearchTerm.CriteriaPath();

        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.IN, LabBatch.LabBatchType.WORKFLOW,
                LabBatch.LabBatchType.FCT);
        lcsetVesselPath.setPropertyName("batchName");
        lcsetVesselPath.addImmutableTermFilter(workflowOnlyFilter);
        lcsetVesselPath.setCriteria(Arrays.asList("BatchVessels", "samples", "mercurySample", "labVessel", "labBatches", "labBatch"));

        lcsetReworkPath.setPropertyName("batchName");
        lcsetReworkPath.addImmutableTermFilter(workflowOnlyFilter);
        lcsetReworkPath.setCriteria(Arrays.asList("BatchVessels", "samples", "mercurySample", "labVessel", "reworkLabBatches"));
        lcsetPathList.addAll(Arrays.asList(lcsetReworkPath, lcsetVesselPath));
        lcsetTerm.setCriteriaPaths(lcsetPathList);
        lcsetTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                Set<String> results = new HashSet<>();
                for (ProductOrderSample productOrderSample : order.getSamples()) {
                    final Optional<MercurySample> mercurySample = Optional.ofNullable(productOrderSample.getMercurySample());
                    mercurySample.ifPresent(mercurySample1 -> {
                        for (LabVessel labVessel : mercurySample1.getLabVessel()) {
                            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                                for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                                    results.add(labBatch.getBatchName());
                                }
                            }
                        }
                    });
                }
                return results;
            }
        });
        lcsetTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {

                Set<String> batchNames = (HashSet<String>) entity;
                StringBuffer uiOutput = new StringBuffer();
                Pattern batchPattern = Pattern.compile("([LCSET]|[FCT])[-\\\\w]*");
                final String jiraBatchLinkFormat = "<a class=\"external\" target=\"JIRA\" href=\"" +
                        context.getJiraConfig().getUrlBase() + JiraLink.BROWSE + "%s\">%s</a>";

                for (String batchName : batchNames) {

                    Matcher batchMatch = batchPattern.matcher(batchName);
                    if(batchMatch.find()) {
                        batchMatch.appendReplacement(uiOutput, String.format(jiraBatchLinkFormat,batchMatch.group()));
                        batchMatch.appendTail(uiOutput);
                        uiOutput.append("<br>");
                    }
                }
                return uiOutput.toString();
            }
        });
        searchTerms.add(lcsetTerm);

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
                return ((ProductOrder) entity).getBusinessKey();// + " -- " +  order.getName();
            }
        });
        pdoJiraTicketTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {

                String pdoOutput = (String) entity;
                return "<a class=\"external\" target=\"new\" href=\"/Mercury/orders/order.action?view=&productOrder="
                       + pdoOutput +"\">"+ pdoOutput +"</a>";
            }
        });
        searchTerms.add(pdoJiraTicketTerm);


        SearchTerm sampleTerm = new SearchTerm();
        sampleTerm.setName("Product Order Sample");
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
                if(StringUtils.isNotBlank(currentSapOrderNumber)) {
                    sapIdResults.add("Active order -->" + currentSapOrderNumber);
                }

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
