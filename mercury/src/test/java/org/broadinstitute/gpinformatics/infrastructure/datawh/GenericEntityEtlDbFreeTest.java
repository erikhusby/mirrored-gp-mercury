package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of GenericEntityEtl as implementd by ProductOrder etl.
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class GenericEntityEtlDbFreeTest {
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

    String datafileDir = System.getProperty("java.io.tmpdir");

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ProductOrderDao dao = createMock(ProductOrderDao.class);
    ProductOrder pdo = createMock(ProductOrder.class);
    ResearchProject researchProject = createMock(ResearchProject.class);
    Product product = createMock(Product.class);
    ProductOrderEtl tst = new ProductOrderEtl();
    RevInfo revInfo = new RevInfo();
    Long revInfoId = 3L;

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void beforeClass() {
        revInfo.setRevDate(new Date(1355000000000L));
        revInfo.setRevInfoId(revInfoId);
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(auditReader, dao, pdo, researchProject, product);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }


    public void testEtl() throws Exception {
        Collection<Long> revIds = new ArrayList<Long>();
        revIds.add(entityId);

        List<Object[]> dataChanges = new ArrayList<Object[]>();
        dataChanges.add(new Object[]{pdo, revInfo, RevisionType.ADD});

        expect(auditReader.fetchDataChanges(revIds, tst.getEntityClass())).andReturn(dataChanges);
        expect(dao.findById(ProductOrder.class, entityId)).andReturn(pdo);

        List<ProductOrder> list = new ArrayList<ProductOrder>();
        list.add(pdo);
        expect(pdo.getProductOrderId()).andReturn(entityId).times(2);
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

        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.doEtl(revIds, etlDateStr);
        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(dao, auditReader, product, researchProject, pdo);
    }

    public void testBackfillEtl() throws Exception {
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

        int recordCount = tst.doBackfillEtl(tst.getEntityClass(), entityId, entityId, etlDateStr);
        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(dao, auditReader, product, researchProject, pdo);
    }

}
