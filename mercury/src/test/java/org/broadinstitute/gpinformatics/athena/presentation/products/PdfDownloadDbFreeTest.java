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

package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.exception.SourcePageNotFoundException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.IncludePDMOnly;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.TopLevelOnly;

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class PdfDownloadDbFreeTest {
    public static final boolean IS_PDM_USER = true;
    public static final boolean NOT_PDM_USER = false;
    private ProductActionBean productActionBean;
    private Product everyoneProduct;
    private Product pdmOnlyProduct;
    private ProductDao productDao;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        everyoneProduct = ProductTestFactory.createStandardExomeSequencing();
        pdmOnlyProduct = ProductTestFactory.createDummyProduct(Workflow.NONE, "P1-" + System.currentTimeMillis(), false, true);
    }

    public void testGetPdfFilenameOneProduct(){
        String filename = ProductActionBean.getPdfFilename(Arrays.asList(everyoneProduct));
        Assert.assertEquals(filename, everyoneProduct.getName() + ".pdf");
    }

    public void testDownloadProductDescriptionsNullProduct(){
        supUpActionBean(NOT_PDM_USER);
        productActionBean.setEditProduct(null);
        productActionBean.downloadProductDescriptions();
        Assert.assertEquals(productActionBean.getValidationErrors().size(), 0);
    }

    public void testDownloadProductDescriptionsNullPartNumber(){
        supUpActionBean(NOT_PDM_USER);
        productActionBean.setEditProduct(ProductTestFactory.createDummyProduct(Workflow.NONE, null));
        productActionBean.downloadProductDescriptions();

        Assert.assertEquals(productActionBean.getValidationErrors().size(), 0);
    }
    public void testDownloadNoResults(){
        supUpActionBean(NOT_PDM_USER);
        Mockito.when(productDao.findProducts(ProductDao.Availability.CURRENT, TopLevelOnly.NO, IncludePDMOnly.NO))
                .thenReturn(Collections.<Product>emptyList());

        productActionBean.downloadProductDescriptions();

        Assert.assertEquals(productActionBean.getValidationErrors().size(), 1);
    }

    public void testGetPdfFilenameManyProducts(){
        String filename = ProductActionBean.getPdfFilename(Arrays.asList(everyoneProduct, pdmOnlyProduct));
        Assert.assertEquals(filename, "Product Descriptions.pdf");
    }

    public void testGetProductsForPdfDownloadIsPdm() throws Exception {
        supUpActionBean(IS_PDM_USER);

        List<Product> products = productActionBean.getProductsForPdfDownload();
        Assert.assertEquals(products.size(), 2);
    }

    public void testGetProductsForPdfDownloadIsNotPdm() throws Exception {
        supUpActionBean(NOT_PDM_USER);

        List<Product> products = productActionBean.getProductsForPdfDownload();
        Assert.assertEquals(products.size(), 1);
    }

    private void supUpActionBean(boolean isPdmUser) {
        UserBean userBean = Mockito.mock(UserBean.class);
        Mockito.when(userBean.isPDMUser()).thenReturn(isPdmUser);

        productDao = Mockito.mock(ProductDao.class);
        Mockito.when(productDao.findByPartNumber(Mockito.anyString())).thenReturn(this.everyoneProduct);
        Mockito.when(productDao.findProducts(ProductDao.Availability.CURRENT, TopLevelOnly.NO, IncludePDMOnly.YES))
                .thenReturn(Arrays.asList(pdmOnlyProduct, everyoneProduct));
        Mockito.when(productDao.findProducts(ProductDao.Availability.CURRENT, TopLevelOnly.NO, IncludePDMOnly.NO))
                .thenReturn(Arrays.asList( pdmOnlyProduct));

        productActionBean = new ProductActionBean(userBean, productDao);

        productActionBean.setContext(new PdfContext());
    }

    class PdfContext extends TestCoreActionBeanContext {
        @Override
        public Resolution getSourcePageResolution() throws SourcePageNotFoundException {
            return new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    // I don't do anything, no.
                }
            };
        }
    }
}
