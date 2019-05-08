/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationError;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;


@Test(groups = TestGroups.DATABASE_FREE)
public class ProductActionBeanTest {
    @Mock
    private ProductDao productDao;
    private ProductActionBean actionBean;
    private Product product;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        actionBean = new ProductActionBean();
        actionBean.setContext(new TestCoreActionBeanContext());
        actionBean.setProductDao(productDao);
        product = Mockito.spy(ProductTestFactory.createTestProduct());
        actionBean.setEditProduct(product);

        Mockito.when(product.getProductId()).thenReturn(1L);
        Mockito.when(productDao.findByPartNumber(Mockito.any())).thenReturn(product);

    }

    public void testValidateForSave() throws Exception {
        actionBean.validateForSave();
        assertThat(actionBean.getValidationErrors().hasFieldErrors(), is(false));
    }

    public void testValidateForSavePartNumberTooLong() throws Exception {
        product.setPartNumber("PN-TOOLONG_123456789123456789");
        actionBean.validateForSave();
        assertThat(actionBean.getValidationErrors().hasFieldErrors(), is(true));
        assertThat(actionBean.getValidationErrors().get("partNumber"), not(Matchers.emptyIterable()));

        // reset the context to clear errors;
        actionBean.setContext(new TestCoreActionBeanContext());
        product.setPartNumber("PN-OK_1234");

        actionBean.validateForSave();
        assertThat(actionBean.getValidationErrors().hasFieldErrors(), is(false));
        assertThat(actionBean.getValidationErrors().get("partNumber"), is(nullValue()));
    }
}
