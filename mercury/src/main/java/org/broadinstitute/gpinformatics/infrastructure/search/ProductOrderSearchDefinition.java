package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDOSamples",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("SAPOrders",
                "productOrderId", "sapReferenceOrders", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("BatchVessels",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("QuoteWork",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("DeliveryDocs",
                "productOrderId", "samples", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("BilledQuote",
                "productOrderId", "samples", ProductOrder.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("productOrderId",
                "productOrderId", "productOrderId", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDORP",
                "pdo.productOrderId", "productOrders","pdo", ResearchProject.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDOProduct",
                "pdoProduct.productOrderId", "productOrders","pdoProduct", Product.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("AddOns",
        "productOrderId", "addOns", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDOKey",
                "productOrderId", "productOrderId", ProductOrder.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("OrderQuote",
                "productOrderId", "productOrderId", ProductOrder.class));

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
        billingSessionTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(billingSessionTerm);

        SearchTerm billingDisplayTerm = new SearchTerm();
        billingDisplayTerm.setName("Billing Related Info");
        billingDisplayTerm.setIsNestedParent(Boolean.TRUE);
        billingDisplayTerm.setPluginClass(ProductOrderBillingPlugin.class);
        searchTerms.add(billingDisplayTerm);
        

        SearchTerm quoteWorkTerm = new SearchTerm();
        quoteWorkTerm.setName("Quote Work Item");
        SearchTerm.CriteriaPath quoteWorkPath = new SearchTerm.CriteriaPath();
        quoteWorkPath.setPropertyName("workItem");
        quoteWorkPath.setCriteria(Arrays.asList("QuoteWork", "samples", "ledgerItems"));
        quoteWorkTerm.setCriteriaPaths(Collections.singletonList(quoteWorkPath));
        quoteWorkTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(quoteWorkTerm);


        SearchTerm sapDeliveryDocTerm = new SearchTerm();
        sapDeliveryDocTerm.setName("SAP Delivery Document Id");
        sapDeliveryDocTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        SearchTerm.CriteriaPath deliveryDocPath = new SearchTerm.CriteriaPath();
        deliveryDocPath.setPropertyName("sapDeliveryDocumentId");
        deliveryDocPath.setCriteria(Arrays.asList("DeliveryDocs", "samples", "ledgerItems"));
        sapDeliveryDocTerm.setCriteriaPaths(Collections.singletonList(deliveryDocPath));
        searchTerms.add(sapDeliveryDocTerm);


        SearchTerm billedQuoteTerm = new SearchTerm();
        billedQuoteTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        billedQuoteTerm.setName("Billed Quote");
        SearchTerm.CriteriaPath billedQuotePath = new SearchTerm.CriteriaPath();
        billedQuotePath.setPropertyName("quoteId");
        billedQuotePath.setCriteria(Arrays.asList("BilledQuote", "samples", "ledgerItems"));
        billedQuoteTerm.setCriteriaPaths(Collections.singletonList(billedQuotePath));
        searchTerms.add(billedQuoteTerm);

        
        return searchTerms;
    }


    private ArrayList<SearchTerm> buildPDOSearchTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm productTerm = new SearchTerm();
        productTerm.setName("Product Part Number");

        SearchTerm.CriteriaPath productCriteriaPath = new SearchTerm.CriteriaPath();

        SearchTerm.CriteriaPath nestedProductPath = new SearchTerm.CriteriaPath();
        nestedProductPath.setCriteria(Collections.singletonList("PDOProduct"));

        productCriteriaPath.setPropertyName("partNumber");
        productCriteriaPath.setCriteria(Arrays.asList("productOrderId"));
        productCriteriaPath.setNestedCriteriaPath(nestedProductPath);

        SearchTerm.CriteriaPath addonPath = new SearchTerm.CriteriaPath();
        addonPath.setPropertyName("partNumber");
        addonPath.setCriteria(Arrays.asList("AddOns", "addOns", "addOn"));

        productTerm.setCriteriaPaths(Arrays.asList(productCriteriaPath, addonPath));
        productTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(productTerm);


        SearchTerm productDisplayTerm = new SearchTerm();
        productDisplayTerm.setName("Product(s)");
        productDisplayTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder) entity;
                List<String> productList = new ArrayList<>();

                Optional<Product> primaryProduct = Optional.ofNullable(((ProductOrder) entity).getProduct());
                primaryProduct.ifPresent(product -> productList.add("Primary Product: " + product.getDisplayName()));

                Optional<List<ProductOrderAddOn>> optionalAddons = Optional.ofNullable(orderData.getAddOns());
                optionalAddons.ifPresent(productOrderAddOns -> {
                    productOrderAddOns.forEach(productOrderAddOn -> {
                        productList.add("Add On: " + productOrderAddOn.getAddOn().getDisplayName());
                    });
                });

                return productList;
            }
        });
        productDisplayTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                List<String> productDisplays = (List<String>) entity;
                StringBuffer displayOutput = new StringBuffer();

                Pattern productPattern = Pattern.compile("((Primary\\sProduct|Add\\sOn):)");

                for (String productDisplay : productDisplays) {
                    Matcher productMatch = productPattern.matcher(productDisplay);
                    if (productMatch.find()) {
                        productMatch.appendReplacement(displayOutput, "<b>" + productMatch.group() + "</b>");
                        productMatch.appendTail(displayOutput);
                        displayOutput.append("<BR>");
                    }
                }
                return displayOutput.toString();
            }
        });
        searchTerms.add(productDisplayTerm);


        //For searching by quotes, and displaying quotes
        SearchTerm quoteTerm = new SearchTerm();
        quoteTerm.setName("Quote Identifier");
        SearchTerm.CriteriaPath quoteCriteraPath = new SearchTerm.CriteriaPath();
        quoteCriteraPath.setPropertyName("quoteId");
        quoteCriteraPath.setCriteria(Arrays.asList("OrderQuote"));
        quoteTerm.setCriteriaPaths(Collections.singletonList(quoteCriteraPath));
        quoteTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder) entity;

                return orderData.getQuoteId();
            }
        });
        quoteTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String quoteId = (String) entity;

                StringBuffer quoteLink = new StringBuffer();
                if(StringUtils.isNotBlank(quoteId)) {
                    quoteLink .append("<a class=\"external\" target=\"QUOTE\" href=\"");
                    quoteLink.append(context.getQuoteLink().quoteUrl(quoteId));
                    quoteLink.append("\">").append(quoteId).append("</a>");
                }
                return quoteLink.toString();
            }
        });
        searchTerms.add(quoteTerm);


        SearchTerm userIDTerm = new SearchTerm();
        userIDTerm.setName("UserID");
        userIDTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getUserIdConverter());
        SearchTerm.CriteriaPath userIDCriteriaPath = new SearchTerm.CriteriaPath();
        userIDCriteriaPath.setPropertyName("createdBy");
        userIDTerm.setCriteriaPaths(Collections.singletonList(userIDCriteriaPath));
        userIDTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(userIDTerm);


        SearchTerm userIdDisplayTerm = new SearchTerm();
        userIdDisplayTerm.setName("Product Order Submitter");
        userIdDisplayTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                Optional<BspUser> bspDisplayUser = Optional.of(context.getBspUserList().getById(order.getCreatedBy()));
                StringBuilder userDisplayName = new StringBuilder();

                bspDisplayUser.ifPresent(bspUser -> userDisplayName.append(bspUser.getFullName()));

                return userDisplayName.toString();
            }
        });
        searchTerms.add(userIdDisplayTerm);


        SearchTerm pdoStatusTerm = new SearchTerm();
        pdoStatusTerm.setName("Order Status");
        pdoStatusTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder)entity;
                return orderData.getOrderStatus();
            }
        });
        pdoStatusTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public ProductOrder.OrderStatus evaluate(Object entity, SearchContext context) {

                String statusSearchValue = context.getSearchValueString();

                return ProductOrder.OrderStatus.valueOf(statusSearchValue);
            }
        });
        SearchTerm.CriteriaPath orderStatusPath = new SearchTerm.CriteriaPath();
        orderStatusPath.setPropertyName("orderStatus");
        pdoStatusTerm.setCriteriaPaths(Collections.singletonList(orderStatusPath));
        searchTerms.add(pdoStatusTerm);



        // In order for Research project search to work, the inclusion of a nested query was ncessary.
        // This is due to the fact that Product Orders are technically Child elements of a Research Projects and
        // Products.  The standard setup for

        SearchTerm rpSearchTerm = new SearchTerm();
        rpSearchTerm.setName("Research Project JIRA ID");
        SearchTerm.CriteriaPath rpCriteriaPath = new SearchTerm.CriteriaPath();

        SearchTerm.CriteriaPath nestedRPCriteriaPath = new SearchTerm.CriteriaPath();
        nestedRPCriteriaPath.setCriteria(Collections.singletonList("PDORP"));

        rpCriteriaPath.setPropertyName("jiraTicketKey");
        rpCriteriaPath.setCriteria(Arrays.asList("productOrderId"));
        rpCriteriaPath.setNestedCriteriaPath(nestedRPCriteriaPath);
        rpSearchTerm.setCriteriaPaths(Collections.singletonList(rpCriteriaPath));
        rpSearchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(rpSearchTerm);


        SearchTerm researchProjectDisplayTerm = new SearchTerm();
        researchProjectDisplayTerm.setName("Research Project");
        researchProjectDisplayTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                return order.getResearchProject().getJiraTicketKey();
            }
        });
        researchProjectDisplayTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String output = (String) entity;
                return "<a class=\"external\" target=\"new\" href=\"/Mercury/projects/project.action?view=&researchProject="
                       + output +"\">"+ output +"</a>";
            }
        });
        searchTerms.add(researchProjectDisplayTerm);

        SearchTerm lcsetTerm = new SearchTerm();
        lcsetTerm.setName("LCSET(s)");
        lcsetTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBatchNameInputConverter());

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
        lcsetTerm.setCriteriaPaths(Arrays.asList(lcsetReworkPath, lcsetVesselPath));
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
                Pattern batchPattern = Pattern.compile("([LCSET]|[FCT])[-\\w]*");
                final String jiraBatchLinkFormat = "<a class=\"external\" target=\"JIRA\" href=\"" +
                        context.getJiraConfig().getUrlBase() + JiraLink.BROWSE + "%s\">%s</a>";

                for (String batchName : batchNames) {

                    Matcher batchMatch = batchPattern.matcher(batchName);
                    if(batchMatch.find()) {
                        batchMatch.appendReplacement(uiOutput, String.format(jiraBatchLinkFormat,batchMatch.group(), batchName));
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
        pdoTicketCriteriaPath.setCriteria(Arrays.asList("PDOKey"));
        pdoJiraTicketTerm.setCriteriaPaths(Collections.singletonList(pdoTicketCriteriaPath));
        pdoJiraTicketTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                StringBuffer productOrderDisplay = new StringBuffer();
                productOrderDisplay.append(order.getBusinessKey()).append(" -- ");
                productOrderDisplay.append(order.getName());

                return productOrderDisplay.toString();
            }
        });
        pdoJiraTicketTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {

                Pattern pdoPattern = Pattern.compile("(PDO|Draft)-\\w*");

                String pdoOutput = (String) entity;
                StringBuffer pdoLinkOutput = new StringBuffer();
                String format =
                        "<a class=\"external\" target=\"new\" href=\"/Mercury/orders/order.action?view=&productOrder=%s\">%s";

                Matcher pdoMatch = pdoPattern.matcher(pdoOutput);
                final boolean matchIsFound = pdoMatch.find();
                String matchGroup = "";
                if(matchIsFound) {
                    String linkText = "";
                    matchGroup = pdoMatch.group();

                    if(StringUtils.contains(matchGroup, "Draft")) {
                        linkText ="(Draft) ";
                    } else {
                        linkText = matchGroup;
                    }
                    pdoMatch.appendReplacement(pdoLinkOutput, String.format(format, matchGroup, linkText));
                    pdoMatch.appendTail(pdoLinkOutput);

                    pdoLinkOutput.append("</a>");
                }

                return pdoLinkOutput.toString();
            }
        });
        searchTerms.add(pdoJiraTicketTerm);


        SearchTerm sampleTerm = new SearchTerm();
        sampleTerm.setName("Product Order Sample(s)");
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
        sampleTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Set<String> sampleSet = (Set<String>) entity;

                return StringUtils.join(sampleSet, ", ");
            }
        });
        searchTerms.add(sampleTerm);

        SearchTerm sapOrderTerm = new SearchTerm();
        sapOrderTerm.setName("SAP Order Id");
        SearchTerm.CriteriaPath sapCriteriaPath = new SearchTerm.CriteriaPath();
        sapCriteriaPath.setPropertyName("sapOrderNumber");
//        sapCriteriaPath.setCriteria(Arrays.asList("SAPOrders", "sapReferenceOrders"));
        sapCriteriaPath.setCriteria(Arrays.asList("SAPOrders"));
        sapOrderTerm.setCriteriaPaths(Collections.singletonList(sapCriteriaPath));
        sapOrderTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> sapIdResults = new HashSet<>();
                ProductOrder order = (ProductOrder) entity;
                boolean first = true;
                String currentSapOrderNumber = order.getSapOrderNumber();
                if(StringUtils.isNotBlank(currentSapOrderNumber)) {
                    sapIdResults.add("Active order --> " + currentSapOrderNumber);
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
