package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEtlDbFreeTest {
    private String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private long productId = 332891L;
    private long researchProjectId = 98789798L;
    private String jiraTicketKey = "PD0-9123488";
    private Date createdDate = new Date(1350000000000L);
    private Date modifiedDate = new Date(1354000000000L);
    private String title = "Some title";
    private String quoteId = "QT-134123";
    private ProductOrder.OrderStatus orderStatus = ProductOrder.OrderStatus.Submitted;
    private long createdBy = 678L;
    private String ownerName = "testname";
    private ProductOrderEtl tst;
    private Collection<RegulatoryInfo> regulatoryInfos = Collections.EMPTY_LIST;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ProductOrderDao dao = createMock(ProductOrderDao.class);
    private ProductOrder pdo = createMock(ProductOrder.class);
    private ResearchProject researchProject = createMock(ResearchProject.class);
    private Product product = createMock(Product.class);
    private BSPUserList userList = createMock(BSPUserList.class);
    private BspUser owner = createMock(BspUser.class);
    private RegulatoryInfo regulatoryInfo = createMock(RegulatoryInfo.class);

    private Object[] mocks = new Object[]{auditReader, dao, pdo, researchProject, product, userList, owner, regulatoryInfo};
    private String sapMockOrderNumber = "1000084774";

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        tst = new ProductOrderEtl(dao, userList);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(pdo.getProductOrderId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ProductOrder.class);
        assertEquals(tst.baseFilename, "product_order");
        assertEquals(tst.entityId(pdo), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrder.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ProductOrder.class, entityId)).andReturn(pdo);

        expect(pdo.getProductOrderId()).andReturn(entityId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(pdo.getProduct()).andReturn(product).times(2);
        expect(pdo.getOrderStatus()).andReturn(orderStatus);
        expect(pdo.getCreatedDate()).andReturn(createdDate);
        expect(pdo.getModifiedDate()).andReturn(modifiedDate);
        expect(pdo.getTitle()).andReturn(title);
        expect(pdo.getQuoteId()).andReturn(quoteId);
        expect(pdo.getJiraTicketKey()).andReturn(jiraTicketKey);
        expect(pdo.getCreatedBy()).andReturn(createdBy);
        expect(userList.getById(createdBy)).andReturn(owner);
        expect(owner.getUsername()).andReturn(ownerName);
        expect(pdo.getPlacedDate()).andReturn(modifiedDate);
        expect(pdo.getSkipRegulatoryReason()).andReturn(null);
        expect(pdo.getRegulatoryInfos()).andReturn(regulatoryInfos);
        expect(pdo.getSapOrderNumber()).andReturn(sapMockOrderNumber);

        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);
        expect(product.getProductId()).andReturn(productId);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",", 16);
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(researchProjectId));
        assertEquals(parts[i++], String.valueOf(productId));
        assertEquals(parts[i++], orderStatus.name());
        assertEquals(parts[i++], ExtractTransform.formatTimestamp(createdDate));
        assertEquals(parts[i++], ExtractTransform.formatTimestamp(modifiedDate));
        assertEquals(parts[i++], title);
        assertEquals(parts[i++], quoteId);
        assertEquals(parts[i++], jiraTicketKey);
        assertEquals(parts[i++], ownerName);
        assertEquals(parts[i++], ExtractTransform.formatTimestamp(modifiedDate));
        assertEquals(parts[i++], "");
        assertEquals(parts[i++], sapMockOrderNumber);
        assertEquals(parts[i++], "");
        assertEquals(parts.length, i);
    }
}

