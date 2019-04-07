package org.broadinstitute.gpinformatics.infrastructure.search;

import com.google.common.collect.ImmutableMap;
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
import org.owasp.encoder.Encode;

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

    public static final String BILLING_RELATED_INFO_COLUMN = "Billing Related Info";
    public static final String PRODUCTS_COLUMN_HEADER = "Product(s)";
    public static final String QUOTE_IDENTIFIER_COLUMN_HEADER = "Quote Identifier";
    public static final String SUBMITTER_NAME_COLUMN_HEADER = "Product Order Submitter Name";
    public static final String ORDER_STATUS_COLUMN_HEADER = "Order Status";
    public static final String RESEARCH_PROJECT_COLUMN_HEADER = "Research Project";
    public static final String LCSETS_COLUMN_HEADER = "LCSET";
    public static final String PDO_TICKET_COLUMN_HEADER = "PDO Ticket";
    public static final String PRODUCT_ORDER_SAMPLES_COLUMN_HEADER = "Product Order Sample(s)";
    public static final String SAP_ORDER_ID_COLUMN_HEADER = "SAP Order Id";

    public ConfigurableSearchDefinition buildSearchDefinition() {
        ProductOrderSearchDefinition searchDefinition = new ProductOrderSearchDefinition();
        Map<String, List<SearchTerm>>
                mapGroupSearchTerms = new LinkedHashMap<>();
        
        List<SearchTerm> idSearchTerms = buildIDTerms();
        mapGroupSearchTerms.put("IDs", idSearchTerms);

        List<SearchTerm> billingSearchTerms = buildBillingSearchTerms();
        mapGroupSearchTerms.put("Billing IDs", billingSearchTerms);

        List<SearchTerm> pdoSearchTerms = buildPDOSearchTerms();
        mapGroupSearchTerms.put("PDO Terms", pdoSearchTerms);

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

        final ConfigurableSearchDefinition configurableSearchDefinition =
                new ConfigurableSearchDefinition(ColumnEntity.PRODUCT_ORDER, criteriaProjections, mapGroupSearchTerms);
        configurableSearchDefinition.addColumnGroupHelpText(ImmutableMap.of("Billing Related Info",
                "Selecting this \"column\" will actually result in a tabular display listed under its associated "
                + "product order.  The content of the billing results table will include: Billing session, the quote "
                + "that was billed, the billed product, the quote work item that tracks the charge, the SAP delivery "
                + "document id (if any) that tracks the change, the quantity charged and the number of samples charged "
                + "for the transaction"));
        return configurableSearchDefinition;
    }

    /**
     * Defines the search terms related to finding and displaying Billing related information on the Product Orders
     *
     * @return
     */
    @NotNull
    private ArrayList<SearchTerm> buildBillingSearchTerms() {
        final ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        // Define the path to Search for PDOs associated with one or more given billing session ids.  Search only.
        SearchTerm billingSessionTerm = new SearchTerm();
        billingSessionTerm.setName("Billing Session Id");
        billingSessionTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getBillingSessionConverter());
        SearchTerm.CriteriaPath billingSessionPath = new SearchTerm.CriteriaPath();
        billingSessionPath.setPropertyName("billingSessionId");
        billingSessionPath.setCriteria(Arrays.asList("BillingSessions", "samples", "ledgerItems", "billingSession"));
        billingSessionTerm.setCriteriaPaths(Collections.singletonList(billingSessionPath));
        billingSessionTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(billingSessionTerm);

        // Define how the billing related information will be displayed.  All logic is defined in the
        // ProductOrderBillingPlugin which will collect Billing sessions and display the associated information in
        // tabular form
        SearchTerm billingDisplayTerm = new SearchTerm();

        billingDisplayTerm.setName(BILLING_RELATED_INFO_COLUMN);
        billingDisplayTerm.setIsNestedParent(Boolean.TRUE);
        billingDisplayTerm.setPluginClass(ProductOrderBillingPlugin.class);
        billingDisplayTerm.setHelpText("Selecting this \"column\" will actually result in a tabular display listed "
                                       + "under its associated product order.  <BR/>The content of the billing results "
                                       + "table will include: <ul><li>Billing session</li><li>The quote that was billed</li><li>The billed "
                                       + "product</li><li>The quote work item that tracks the charge</li><li>The SAP delivery "
                                       + "document id (if any) that tracks the change</li><li>The quantity charged and the "
                                       + "number of samples charged for the transaction</li></ul>");
        searchTerms.add(billingDisplayTerm);
        
        // Defines the path to search for billed PDOs by the ID(s) of the Quote work items that are associated with
        // billing ledgers
        SearchTerm quoteWorkTerm = new SearchTerm();
        quoteWorkTerm.setName("Quote Work Item");
        SearchTerm.CriteriaPath quoteWorkPath = new SearchTerm.CriteriaPath();
        quoteWorkPath.setPropertyName("workItem");
        quoteWorkPath.setCriteria(Arrays.asList("QuoteWork", "samples", "ledgerItems"));
        quoteWorkTerm.setCriteriaPaths(Collections.singletonList(quoteWorkPath));
        quoteWorkTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(quoteWorkTerm);

        // Defines the path to search for PDOs that are billed to SAP by the delivery document id(s)
        SearchTerm sapDeliveryDocTerm = new SearchTerm();
        sapDeliveryDocTerm.setName("SAP Delivery Document Id");
        sapDeliveryDocTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        SearchTerm.CriteriaPath deliveryDocPath = new SearchTerm.CriteriaPath();
        deliveryDocPath.setPropertyName("sapDeliveryDocumentId");
        deliveryDocPath.setCriteria(Arrays.asList("DeliveryDocs", "samples", "ledgerItems"));
        sapDeliveryDocTerm.setCriteriaPaths(Collections.singletonList(deliveryDocPath));
        searchTerms.add(sapDeliveryDocTerm);

        // Defines the path to search for PDOs by that have been billed by the quote(s) with which they were billed
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

    /**
     * Defines the Search terms that are associated with elements defined on a product order
     * @return
     */
    private ArrayList<SearchTerm> buildPDOSearchTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        // Defines the search term used to find PDOs by the products (both primary and add on) with which they are
        // associated
        SearchTerm productTerm = new SearchTerm();
        productTerm.setName("Product Part Number");

        // Due to the fact that PDOs have a ManyToOne relationship with Products, the definition of how to search
        // for products are a little different. The path to the Product field is a nested criteria search and while
        // the top level criteria is defining how to relate the product order id to the nested search.  Similar to
        //      Select * from Product Order pdo
        //      where pdo.product_order_id in (
        //          select product.product_order_id from Product product
        //          where product.part_number = ::Passed_in_part_number
        //      )
        SearchTerm.CriteriaPath productCriteriaPath = new SearchTerm.CriteriaPath();
        SearchTerm.CriteriaPath nestedProductPath = new SearchTerm.CriteriaPath();
        nestedProductPath.setCriteria(Collections.singletonList("PDOProduct"));
        productCriteriaPath.setPropertyName("partNumber");
        productCriteriaPath.setCriteria(Arrays.asList("productOrderId"));
        productCriteriaPath.setNestedCriteriaPath(nestedProductPath);
        // defines the criteria path to find add ons
        SearchTerm.CriteriaPath addonPath = new SearchTerm.CriteriaPath();
        addonPath.setPropertyName("partNumber");
        // Additional search path for the criteria to find add on products with which product orders are associated.
        addonPath.setCriteria(Arrays.asList("AddOns", "addOns", "addOn"));

        productTerm.setCriteriaPaths(Arrays.asList(productCriteriaPath, addonPath));
        // Makes this search term only for search criteria
        productTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(productTerm);

        // Defines the search term that will control the product result column for any found PDOs
        SearchTerm productDisplayTerm = new SearchTerm();
        productDisplayTerm.setName(PRODUCTS_COLUMN_HEADER);
        // Customize how the product info is displayed and return a list of the display info.  This method is used for
        // both UI output and download
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
        // Takes the output of setDisplayValueExpression and adds any UI specific embelishments to it.  In the case
        // of this method, it will bold the lead in
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


        // For searching by quotes, and displaying quotes
        SearchTerm quoteTerm = new SearchTerm();
        quoteTerm.setName(QUOTE_IDENTIFIER_COLUMN_HEADER);
        SearchTerm.CriteriaPath quoteCriteraPath = new SearchTerm.CriteriaPath();
        quoteCriteraPath.setPropertyName("quoteId");
        quoteCriteraPath.setCriteria(Arrays.asList("OrderQuote"));
        quoteTerm.setCriteriaPaths(Collections.singletonList(quoteCriteraPath));
        quoteTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder orderData = (ProductOrder) entity;

                return Encode.forHtml(orderData.getQuoteId());
            }
        });
        // Takes the output of setDisplayValueExpression and wraps the quote ID in an anchor tag to allow the user
        // to go directly to the quote
        quoteTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String quoteId = (String) entity;

                StringBuffer quoteLink = new StringBuffer();
                if(StringUtils.isNotBlank(quoteId)) {
                    quoteLink .append("<a class=\"external\" target=\"QUOTE\" href=\"");
                    //todo this needs to be sap quote link when it is an SAP Order.  HOw do we get that context in here.
                    quoteLink.append(context.getQuoteLink().quoteUrl(quoteId));
                    quoteLink.append("\">").append(Encode.forHtml(quoteId)).append("</a>");
                }
                return quoteLink.toString();
            }
        });
        quoteTerm.setMustEscape(false);
        searchTerms.add(quoteTerm);

        // Defines the search term for finding PDOs by the Broad user id of the PDOs owner
        SearchTerm userIDTerm = new SearchTerm();
        userIDTerm.setName("Broad User ID of Order Submitter");
        userIDTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getUserIdConverter());
        SearchTerm.CriteriaPath userIDCriteriaPath = new SearchTerm.CriteriaPath();
        userIDCriteriaPath.setPropertyName("createdBy");
        userIDTerm.setCriteriaPaths(Collections.singletonList(userIDCriteriaPath));
        userIDTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(userIDTerm);

        // Similar to the above search term, this one deals with the owner of the PDO.  This search term however
        // is defined to just display the column for the user
        SearchTerm userIdDisplayTerm = new SearchTerm();
        userIdDisplayTerm.setName(SUBMITTER_NAME_COLUMN_HEADER);
        userIdDisplayTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                Optional<BspUser> bspDisplayUser = Optional.ofNullable(context.getBspUserList().getById(order.getCreatedBy()));
                StringBuilder userDisplayName = new StringBuilder();

                bspDisplayUser.ifPresent(bspUser -> userDisplayName.append(bspUser.getFullName()));

                return userDisplayName.toString();
            }
        });
        searchTerms.add(userIdDisplayTerm);

        // Defines the search terms to find PDOs by the order status with which they are associated
        SearchTerm pdoStatusTerm = new SearchTerm();
        pdoStatusTerm.setName(ORDER_STATUS_COLUMN_HEADER);
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

        pdoStatusTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                List<ConstrainedValue> constrainedStatusValues = new ArrayList<>();
                for (ProductOrder.OrderStatus status: ProductOrder.OrderStatus.values()) {
                    constrainedStatusValues.add(new ConstrainedValue(status.toString(), status.getDisplayName()));
                }
                return constrainedStatusValues;
            }
        });
        SearchTerm.CriteriaPath orderStatusPath = new SearchTerm.CriteriaPath();
        orderStatusPath.setPropertyName("orderStatus");
        pdoStatusTerm.setCriteriaPaths(Collections.singletonList(orderStatusPath));
        searchTerms.add(pdoStatusTerm);

        // Search only definition
        //
        // In order for Research project search to work, the inclusion of a nested query was ncessary.
        // This is due to the fact that Product Orders are technically Child elements of a Research Projects and
        // Products.  The derived query would be similar to:
        //      Select * from Product Order pdo
        //      where pdo.product_order_id in (
        //          select rp.product_order_id from Research_Project rp
        //          where rp.jiraTicketKey = ::Passed_in_jira_ticket
        //      )
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

        // Display only search term for showing the research project with which the found product order is associated
        SearchTerm researchProjectDisplayTerm = new SearchTerm();
        researchProjectDisplayTerm.setName(RESEARCH_PROJECT_COLUMN_HEADER);
        researchProjectDisplayTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                String result = "";
                final Optional<ResearchProject> researchProject = Optional.ofNullable(order.getResearchProject());
                if(researchProject.isPresent()) {
                    result = Encode.forHtml(researchProject.get().getBusinessKey());
                }
                return result;
            }
        });
        // Defines the UI enhanced display for the research projects to display.  Enhanced with a link to allow the
        // user to be able to easily navigate to the research project definition
        researchProjectDisplayTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String output = (String) entity;
                String result = "";
                if(StringUtils.isNotBlank(output)) {
                    output = Encode.forHtml(output);
                    result = "<a class=\"external\" target=\"new\" href=\"/Mercury/projects/project.action?view=&researchProject="
                    + output + "\">" + output + "</a>";
                }
                return result;
            }
        });
        researchProjectDisplayTerm.setMustEscape(false);
        searchTerms.add(researchProjectDisplayTerm);

        // Defines the search term to find product orders by a given set of LCSETs which were created with samples
        // of which the product order is created
        SearchTerm lcsetTerm = new SearchTerm();
        lcsetTerm.setName(LCSETS_COLUMN_HEADER);
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
        // navigate through
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
                                    results.add(Encode.forHtml(labBatch.getBatchName()));
                                }
                            }
                        }
                    });
                }
                return results;
            }
        });
        // Defines the UI enhanced display for the LCSET.  Will provide the user with a link to the JIRA ticket for
        // easier review
        lcsetTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {

                Set<String> batchNames = (HashSet<String>) entity;
                StringBuffer uiOutput = new StringBuffer();
                Pattern batchPattern = Pattern.compile("([LCSET]|[FCT])[-\\w]*");
                final String jiraBatchLinkFormat = "<a class=\"external\" target=\"JIRA\" href=\"" +
                        context.getJiraConfig().getUrlBase() + JiraLink.BROWSE + "%s\">%s</a>";

                for (String batchName : batchNames) {

                    batchName = Encode.forHtml(batchName);
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
        lcsetTerm.setMustEscape(false);
        searchTerms.add(lcsetTerm);
        return searchTerms;
    }

    /**
     * Defines all the terms that directly relate to the definition of a product order
     * @return
     */
    @NotNull
    private ArrayList<SearchTerm> buildIDTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        // Defines the Search term to find and display the product order by a given set of product order JIRA tickets
        SearchTerm pdoJiraTicketTerm = new SearchTerm();
        pdoJiraTicketTerm.setName(PDO_TICKET_COLUMN_HEADER);
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
                productOrderDisplay.append(Encode.forHtml(order.getBusinessKey())).append(" -- ");
                productOrderDisplay.append(Encode.forHtml(order.getName()));

                return productOrderDisplay.toString();
            }
        });
        // Defines the UI enhanced display of the found product order.  Using regular Expression, it will wrap the
        // display value for the product order with an anchor tag pointing to the product order definition.
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
                    pdoMatch.appendReplacement(pdoLinkOutput, String.format(format, Encode.forHtml(matchGroup),
                            Encode.forHtml(linkText)));
                    pdoMatch.appendTail(pdoLinkOutput);

                    pdoLinkOutput.append("</a>");
                }

                return pdoLinkOutput.toString();
            }
        });
        pdoJiraTicketTerm.setMustEscape(false);
        searchTerms.add(pdoJiraTicketTerm);

        // Defines the search term to find product orders by the sample name of samples with which they are defined
        SearchTerm sampleTerm = new SearchTerm();
        sampleTerm.setName(PRODUCT_ORDER_SAMPLES_COLUMN_HEADER);
        SearchTerm.CriteriaPath sampleCriteriaPath = new SearchTerm.CriteriaPath();
        sampleCriteriaPath.setPropertyName("sampleName");
        sampleCriteriaPath.setCriteria(Arrays.asList("PDOSamples", "samples"));
        sampleTerm.setCriteriaPaths(Collections.singletonList(sampleCriteriaPath));
        sampleTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerms.add(sampleTerm);

        // Defines the search term for finding product orders by the SAP order with which they are associated.
        SearchTerm sapOrderTerm = new SearchTerm();
        sapOrderTerm.setName(SAP_ORDER_ID_COLUMN_HEADER);
        SearchTerm.CriteriaPath sapCriteriaPath = new SearchTerm.CriteriaPath();
        sapCriteriaPath.setPropertyName("sapOrderNumber");
        sapCriteriaPath.setCriteria(Arrays.asList("SAPOrders", "sapReferenceOrders"));
        sapOrderTerm.setCriteriaPaths(Collections.singletonList(sapCriteriaPath));
        // Since product orders can have multiple historic SAP orders associated with them, this following definition
        // will assist in distinguishing which one is currently the active SAP order
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
