package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of ProductOrder etl.
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long productId = 332891L;
    long researchProjectId = 98789798L;
    String jiraTicketKey = "PD0-9123488";
    Date createdDate = new Date(1350000000000L);
    Date modifiedDate = new Date(1354000000000L);
    String title = "Some title";
    String quoteId = "QT-134123";
    ProductOrder.OrderStatus orderStatus = ProductOrder.OrderStatus.Submitted;

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ProductOrderDao dao = createMock(ProductOrderDao.class);
    ProductOrder pdo = createMock(ProductOrder.class);
    ResearchProject researchProject = createMock(ResearchProject.class);
    Product product = createMock(Product.class);

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(auditReader, dao, pdo, researchProject, product);
    }

    public void testEtlFlags() throws Exception {
        expect(pdo.getProductOrderId()).andReturn(entityId);
        replay(dao, auditReader, product, researchProject, pdo);

        ProductOrderEtl tst = new ProductOrderEtl();
        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), ProductOrder.class);

        assertEquals(tst.getBaseFilename(), "product_order");

        assertEquals(tst.entityId(pdo), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(dao, auditReader, product, researchProject, pdo);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrder.class, -1L)).andReturn(null);

        replay(dao, auditReader, product, researchProject, pdo);

        ProductOrderEtl tst = new ProductOrderEtl();
        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(dao, auditReader, product, researchProject, pdo);
    }

    public void testMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrder.class, entityId)).andReturn(pdo);

        List<ProductOrder> list = new ArrayList<ProductOrder>();
        list.add(pdo);
        expect(pdo.getProductOrderId()).andReturn(entityId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(pdo.getProduct()).andReturn(product).times(2);
        expect(pdo.getOrderStatus()).andReturn(orderStatus);
        expect(pdo.getCreatedDate()).andReturn(createdDate);
        expect(pdo.getModifiedDate()).andReturn(modifiedDate);
        expect(pdo.getTitle()).andReturn(title);
        expect(pdo.getQuoteId()).andReturn(quoteId);
        expect(pdo.getJiraTicketKey()).andReturn(jiraTicketKey);

        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);
        expect(product.getProductId()).andReturn(productId);

        replay(dao, auditReader, product, researchProject, pdo);

        ProductOrderEtl tst = new ProductOrderEtl();
        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));
        assertEquals(parts[3], String.valueOf(researchProjectId));
        assertEquals(parts[4], String.valueOf(productId));
        assertEquals(parts[5], orderStatus.name());
        assertEquals(parts[6], ExtractTransform.secTimestampFormat.format(createdDate));
        assertEquals(parts[7], ExtractTransform.secTimestampFormat.format(modifiedDate));
        assertEquals(parts[8], title);
        assertEquals(parts[9], quoteId);
        assertEquals(parts[10], jiraTicketKey);

        verify(dao, auditReader, product, researchProject, pdo);
    }

    public void testMakeEtlRecord2() throws Exception {
        List<ProductOrder> list = new ArrayList<ProductOrder>();
        list.add(pdo);
        expect(dao.findAll(eq(ProductOrder.class), (GenericDao.GenericDaoCallback<ProductOrder>)anyObject())).andReturn(list);

        expect(pdo.getProductOrderId()).andReturn(entityId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(pdo.getProduct()).andReturn(product).times(2);
        expect(pdo.getOrderStatus()).andReturn(orderStatus);
        expect(pdo.getCreatedDate()).andReturn(createdDate);
        expect(pdo.getModifiedDate()).andReturn(modifiedDate);
        expect(pdo.getTitle()).andReturn(title);
        expect(pdo.getQuoteId()).andReturn(quoteId);
        expect(pdo.getJiraTicketKey()).andReturn(jiraTicketKey);

        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);
        expect(product.getProductId()).andReturn(productId);

        replay(dao, auditReader, product, researchProject, pdo);

        ProductOrderEtl tst = new ProductOrderEtl();
        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        String[] parts = records.iterator().next().split(",");
        assertEquals(parts[0], etlDateStr);
        assertEquals(parts[1], "F");
        assertEquals(parts[2], String.valueOf(entityId));

        verify(dao, auditReader, product, researchProject, pdo);
    }

}

