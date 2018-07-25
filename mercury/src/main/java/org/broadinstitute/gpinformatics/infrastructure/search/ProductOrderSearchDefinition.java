package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


        return searchTerms;
    }

    @NotNull
    private ArrayList<SearchTerm> buildPDOSearchTerms() {

        ArrayList<SearchTerm> searchTerms = new ArrayList<>();

        List<SearchTerm.CriteriaPath> productCriteriaPaths = new ArrayList<>();
        SearchTerm productTerm = new SearchTerm();
        productTerm.setName("Product Part Number");
        SearchTerm.CriteriaPath productCriteriaPath = new SearchTerm.CriteriaPath();
        productCriteriaPath.setPropertyName("partNumber");
        productCriteriaPath.setCriteria(Arrays.asList("Products", "product"));
        productCriteriaPaths.add(productCriteriaPath);
        productTerm.setCriteriaPaths(productCriteriaPaths);
        productTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                ProductOrder order = (ProductOrder) entity;
                return ((ProductOrder) entity).getProduct().getDisplayName() ;
            }
        });



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
        List<SearchTerm.CriteriaPath> pdoKeyCriteriaPathList = new ArrayList<>();
        SearchTerm.CriteriaPath pdoTicketCriteriaPath = new SearchTerm.CriteriaPath();
        pdoTicketCriteriaPath.setPropertyName("jiraTicketKey");
        pdoKeyCriteriaPathList.add(pdoTicketCriteriaPath);
        pdoJiraTicketTerm.setCriteriaPaths(pdoKeyCriteriaPathList);
        searchTerms.add(pdoJiraTicketTerm);

        List<SearchTerm.CriteriaPath> sampleCriteriaPathList = new ArrayList<>();
        SearchTerm sampleTerm = new SearchTerm();
        sampleTerm.setName("Product Order Sample Id");
        SearchTerm.CriteriaPath sampleCriteriaPath = new SearchTerm.CriteriaPath();
        sampleCriteriaPath.setPropertyName("sampleName");
        sampleCriteriaPath.setCriteria(Arrays.asList("PDOSamples", "samples"));
        sampleCriteriaPathList.add(sampleCriteriaPath);
        sampleTerm.setCriteriaPaths(sampleCriteriaPathList);
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


        List<SearchTerm.CriteriaPath> sapCriteriaPathList = new ArrayList<>();
        SearchTerm sapOrderTerm = new SearchTerm();
        sapOrderTerm.setName("SAP Order Id");
        SearchTerm.CriteriaPath sapCriteriaPath = new SearchTerm.CriteriaPath();
        sapCriteriaPath.setPropertyName("sapOrderNumber");
        sapCriteriaPath.setCriteria(Arrays.asList("SAPOrders", "sapReferenceOrders"));
        sapCriteriaPathList.add(sapCriteriaPath);
        sapOrderTerm.setCriteriaPaths(sapCriteriaPathList);
        sapOrderTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {

                

                return ;
            }
        });


        return searchTerms;
    }

}
