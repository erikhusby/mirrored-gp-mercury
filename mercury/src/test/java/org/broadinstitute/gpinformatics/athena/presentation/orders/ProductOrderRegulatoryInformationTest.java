/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderRegulatoryInformationTest  {
    private ResearchProject researchProject;
    private ProductOrderActionBean actionBean;


    @BeforeMethod
    private void setUp() {
        ProductOrder pdo = ProductOrderTestFactory.buildSampleInitiationProductOrder(22);
        actionBean = new ProductOrderActionBean();
        actionBean.setEditOrder(pdo);
        actionBean.setContext(new CoreActionBeanContext());

        ResearchProject grannyResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(12, "GrannyResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);

        ResearchProject uncleResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(120, "UncleResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);

        ResearchProject mamaResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(1200, "MamaResearchProject", "To Study Stuff", ResearchProject.IRB_ENGAGED);

        researchProject = ResearchProjectTestFactory
                .createDummyResearchProject(12000, "BabyResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);

        researchProject.setParentResearchProject(mamaResearchProject);
        mamaResearchProject.setParentResearchProject(grannyResearchProject);
        uncleResearchProject.setParentResearchProject(grannyResearchProject);
    }

    public void testGetJson() throws Exception {
        ProductOrderRegulatoryInformation regulatoryInformation =
                new ProductOrderRegulatoryInformation(actionBean.getEditOrder().getResearchProject(),
                        actionBean.getEditOrder());
        String json = regulatoryInformation.getJson();

        ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);
        Assert.assertEquals(jsonNode.get(0).size(), actionBean.getEditOrder().findAvailableRegulatoryInfos().size());
        Assert.assertEquals(jsonNode.get(0).get("group").asText(), "Test synopsis");
        Assert.assertTrue(StringUtils.isNotBlank(json));
    }


    public void testParentHierarchy() {
        ProductOrderRegulatoryInformation productOrderRegulatoryInformation =
                new ProductOrderRegulatoryInformation(researchProject, actionBean.getEditOrder());

        Map<String, Collection<RegulatoryInfo>> regulatoryInfoByProject
                = productOrderRegulatoryInformation.setupRegulatoryInformation(researchProject);

        Assert.assertEquals(regulatoryInfoByProject.size(), 3);
        List<String> titles = Arrays.asList("MamaResearchProject", "GrannyResearchProject", "BabyResearchProject");
        for (Map.Entry<String, Collection<RegulatoryInfo>> regulatoryCollection : regulatoryInfoByProject.entrySet()) {
            Assert.assertTrue(titles.contains(regulatoryCollection.getKey()));
            Assert.assertFalse(regulatoryCollection.getKey().equals("UncleResearchProject"));
            Assert.assertEquals(regulatoryCollection.getValue().size(), 2);
        }
        Assert.assertEquals(regulatoryInfoByProject.size(), 3, "Should have three projects here.");
    }


    @DataProvider(name = "regulatoryOptionsDataProvider")
    public Iterator<Object []> regulatoryOptionsDataProvider() throws ParseException {

        Date grandfatheredInDate = DateUtils.parseDate("01/01/2014");
        Date newDate = DateUtils.parseDate(ProductOrder.IRB_REQUIRED_START_DATE_STRING);
        String skipReviewReason = "not human subjects research";
        RegulatoryInfo regulatoryInfo = new RegulatoryInfo("TEST-1234", RegulatoryInfo.Type.IRB, "12345");
        RegulatoryInfo nullRegulatoryInfo = null;
        List<Object[]> testCases = new ArrayList<>();
        for (String action : Arrays.asList(ProductOrderActionBean.PLACE_ORDER, ProductOrderActionBean.VALIDATE_ORDER)) {
            testCases.add(new Object[]{action, false, "", regulatoryInfo, newDate, true});
            testCases.add(new Object[]{action, false, "", nullRegulatoryInfo, newDate, false});
            testCases.add(new Object[]{action, false, "", regulatoryInfo, grandfatheredInDate, true});
            testCases.add(new Object[]{action, false, "", nullRegulatoryInfo, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, skipReviewReason, regulatoryInfo, newDate, true});
            testCases.add(new Object[]{action, true, skipReviewReason, nullRegulatoryInfo, newDate, true});
            testCases.add(new Object[]{action, true, skipReviewReason, regulatoryInfo, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, skipReviewReason, nullRegulatoryInfo, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, null, regulatoryInfo, newDate, true});
            testCases.add(new Object[]{action, true, null, nullRegulatoryInfo, newDate, false});
            testCases.add(new Object[]{action, true, null, regulatoryInfo, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, null, nullRegulatoryInfo, grandfatheredInDate, true});
        }

        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", regulatoryInfo, newDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", nullRegulatoryInfo, newDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", regulatoryInfo, grandfatheredInDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", nullRegulatoryInfo, grandfatheredInDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, regulatoryInfo, newDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, nullRegulatoryInfo, newDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, regulatoryInfo, grandfatheredInDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, nullRegulatoryInfo, grandfatheredInDate, true});
        // skipValidation is checked but no reason is given. This should fail even if the dates are valid
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, regulatoryInfo, newDate, false});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, nullRegulatoryInfo, newDate, false});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, regulatoryInfo, grandfatheredInDate, false});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, nullRegulatoryInfo, grandfatheredInDate, false});

        return testCases.iterator();
    }


    @Test(dataProvider = "regulatoryOptionsDataProvider" )
    public void testRegulatoryInformation(String action, boolean skipRegulatory, String skipRegulatoryReason,
                                          RegulatoryInfo regulatoryInfo, Date placedDate, boolean expectedToPass)
            throws ParseException {
        // Set up initial state for objects and validate
        actionBean.getEditOrder().getResearchProject().getRegulatoryInfos().clear();
        Assert.assertFalse(actionBean.getEditOrder().hasRegulatoryInfo());
        actionBean.getEditOrder().getRegulatoryInfos().clear();
        Assert.assertTrue(actionBean.getEditOrder().getRegulatoryInfos().isEmpty());
        actionBean.clearValidationErrors();
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty());

        // Now test test validation using passed-in parameters.
        actionBean.setSkipRegulatoryInfo(skipRegulatory);
        actionBean.getEditOrder().setSkipRegulatoryReason(skipRegulatoryReason);
        actionBean.getEditOrder().setPlacedDate(placedDate);
        if (regulatoryInfo != null) {
            actionBean.getEditOrder().getResearchProject().addRegulatoryInfo(regulatoryInfo);
            actionBean.getEditOrder().addRegulatoryInfo(regulatoryInfo);
        }
        actionBean.validateRegulatoryInformation(action);
        Assert.assertEquals(actionBean.getValidationErrors().isEmpty(), expectedToPass);
    }



}
