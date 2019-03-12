/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductTest {

    private UserBean mockUserBean=null;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        mockUserBean = Mockito.mock(UserBean.class);
    }

    @SuppressWarnings("ConstantConditions")
    @DataProvider(name = "productsRolesDataProvider")
    public static Object[][] productsRolesDataProvider() {
        boolean PDM_ORDERABLE_ONLY = true;
        Product pdmOnlyAddOn = buildTestProduct("pdmOnlyAddOn", PDM_ORDERABLE_ONLY);
        Product anyoneAddOn =buildTestProduct("anyoneAddOn", !PDM_ORDERABLE_ONLY);

        return new Object[][] {
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.PDM, pdmOnlyAddOn},
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.PDM, anyoneAddOn},
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.Developer, pdmOnlyAddOn},
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.Developer, anyoneAddOn},
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.LabUser, null},
                new Object[]{"pdmOnlyProduct", PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.LabUser, anyoneAddOn},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.Developer, pdmOnlyAddOn},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.PDM, anyoneAddOn},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.Developer, anyoneAddOn},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.PDM, pdmOnlyAddOn},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, pdmOnlyAddOn,  Role.LabUser, null},
                new Object[]{"anyoneProduct", !PDM_ORDERABLE_ONLY, anyoneAddOn,   Role.LabUser, anyoneAddOn},
        };
    }

    @Test(dataProvider = "productsRolesDataProvider")
    public void testAddOns(String productName, boolean isPdmOrderableOnly, Product addOn, Role role, Product expectedAddOn){
        Product product = buildTestProduct(productName, isPdmOrderableOnly);
        product.addAddOn(addOn);
        Mockito.when(mockUserBean.getRoles()).thenReturn(Collections.singleton(role));
        Set<Product> expectedAddOns;
        String expectedAddOnName = null;

        if (expectedAddOn != null) {
            expectedAddOns = Collections.singleton(expectedAddOn);
            expectedAddOnName = expectedAddOn.getProductName();
        } else {
            expectedAddOns = Collections.emptySet();
        }
        Set<Product> addOns = product.getAddOns(mockUserBean);
        assertThat(String.format("%s:%s:%s=%s", product.getProductName(), addOn.getProductName(), role, expectedAddOnName),
                addOns, containsInAnyOrder(expectedAddOns.toArray(new Product[expectedAddOns.size()])));

    }

    private static Product buildTestProduct(String productName, boolean pdmOnly) {
        Product product =
                ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, productName, false, pdmOnly);
        product.setProductName(productName);
        return product;
    }
    @DataProvider(name = "aggregationParticles")
      public Iterator<Object[]> aggregationParticles() {
          List<Object[]> testCases = new ArrayList<>();
          testCases.add(new Object[]{null, Product.AggregationParticle.DEFAULT_LABEL});
          testCases.add(new Object[]{Product.AggregationParticle.PDO, Product.AggregationParticle.PDO.getDisplayName()});
          testCases.add(new Object[]{Product.AggregationParticle.PDO_ALIQUOT,
              Product.AggregationParticle.PDO_ALIQUOT.getDisplayName()});

          return testCases.iterator();
      }

      @Test(dataProvider = "aggregationParticles")
      public void testDefaultAggregationParticleDefaultValueNeverNull(Product.AggregationParticle aggregationParticle, String displayValue) {
          Product product = new Product();
          product.setDefaultAggregationParticle(aggregationParticle);
          assertThat(displayValue, notNullValue());
          assertThat(product.getAggregationParticleDisplayName(), equalTo(displayValue));
      }
}
