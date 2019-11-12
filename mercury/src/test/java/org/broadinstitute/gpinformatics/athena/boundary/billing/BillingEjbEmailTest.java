/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import freemarker.core.InvalidReferenceException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingEjbEmailTest {

    private BillingEjb billingEjb;

    @BeforeTest
    public void setUp() {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();
        billingEjb = new BillingEjb(null, null, null, null, null, AppConfig.produce(Deployment.DEV),
            SapConfig.produce(Deployment.DEV), null, templateEngine, null, null);
    }

    public void testProcessTemplate() {
        String expected = "<table>\n"
                          + "    <thead>\n"
                          + "    <tr>\n"
                          + "        <th>Mercury Order</th>\n"
                          + "        <th>Material</th>\n"
                          + "        <th>SAP Sales Order</th>\n"
                          + "        <th>Delivery Documents<br/>Related to this Item</th>\n"
                          + "        <th>Quantity</th>\n"
                          + "    </tr>\n"
                          + "    </thead>\n"
                          + "    <tbody>\n"
                          + "    <tr>\n"
                          + "        <td>pdo-1</td>\n"
                          + "        <td>material</td>\n"
                          + "        <td>sap-on</td>\n"
                          + "        <td>sap-dd</td>\n"
                          + "        <td>1</td>\n"
                          + "    </tr>\n"
                          + "    <tr>\n"
                          + "        <td>pdo-2</td>\n"
                          + "        <td>material2</td>\n"
                          + "        <td>sap-on2</td>\n"
                          + "        <td>sap-dd2</td>\n"
                          + "        <td>2</td>\n"
                          + "    </tr>\n"
                          + "    </tbody>\n"
                          + "</table>";

        Map<String, Object> map = new HashMap<>();
        Map<String, Set<Map<String, Object>>> returnMap = new HashMap<>();

        map.put("mercuryOrder", "pdo-1");
        map.put("material", "material");
        map.put("sapOrderNumber", "sap-on");
        map.put("sapDeliveryDocuments", "sap-dd");
        map.put("quantity", "1");
        returnMap.computeIfAbsent("returnList", k -> new HashSet<>()).add(map);
        map = new HashMap<>();

        map.put("mercuryOrder", "pdo-2");
        map.put("material", "material2");
        map.put("sapOrderNumber", "sap-on2");
        map.put("sapDeliveryDocuments", "sap-dd2");
        map.put("quantity", "2");
        returnMap.get("returnList").add(map);

        String result = billingEjb.processTemplate(SapConfig.BILLING_CREDIT_TEMPLATE, returnMap).trim();

        assertThat(result, containsString(expected));
    }

    public void testProcessTemplateNullKey() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Set<Map<String, Object>>> returnMap = new HashMap<>();
        returnMap.put(null, null);
        returnMap.computeIfAbsent("returnList", k -> new HashSet<>()).add(map);
        try {
            billingEjb.processTemplate(SapConfig.BILLING_CREDIT_TEMPLATE, returnMap);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), instanceOf(InvalidReferenceException.class));
        }
    }

    public void testProcessTemplateNullValue() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, null);
        Map<String, Set<Map<String, Object>>> returnMap = new HashMap<>();
        map.put(null, null);
        returnMap.computeIfAbsent("returnList", k -> new HashSet<>()).add(map);

        try {
            billingEjb.processTemplate(SapConfig.BILLING_CREDIT_TEMPLATE, returnMap);
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(InvalidReferenceException.class));
        }
    }

    public void testProcessTemplateNullMap() {
        Map<String, Object> map = null;
        Map<String, Set<Map<String, Object>>> returnMap = new HashMap<>();
        returnMap.computeIfAbsent("returnList", k -> new HashSet<>()).add(map);

        try {
            billingEjb.processTemplate(SapConfig.BILLING_CREDIT_TEMPLATE, returnMap);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), instanceOf(InvalidReferenceException.class));
        }
    }
}
