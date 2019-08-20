package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
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
    private ProductOrderDao pdoDao = createMock(ProductOrderDao.class);
    private AttributeArchetypeDao archetypeDao = createMock(AttributeArchetypeDao.class);
    private GenotypingChip genotypingChip = createMock(GenotypingChip.class);
    private GenotypingProductOrderMapping genotypingProductOrderMapping = createMock(GenotypingProductOrderMapping.class);
    private ProductOrder pdo = createMock(ProductOrder.class);
    private ResearchProject researchProject = createMock(ResearchProject.class);
    private Product product = createMock(Product.class);
    private BSPUserList userList = createMock(BSPUserList.class);
    private BspUser owner = createMock(BspUser.class);
    private RegulatoryInfo regulatoryInfo = createMock(RegulatoryInfo.class);
    private ProductEjb productEjb = createMock(ProductEjb.class);

    private Object[] mocks = new Object[]{auditReader, pdoDao, archetypeDao, genotypingChip, genotypingProductOrderMapping, pdo, researchProject, product, userList, owner,
            regulatoryInfo, productEjb};
    private String sapMockOrderNumber = "1000084774";
    private String chipArchetypeGroup = "Infinium";
    private String arrayChipType = "PsychChipContrived";
    private String callThreshold = "98";
    private String callThresholdOverride = "66";

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        tst = new ProductOrderEtl(pdoDao, userList, productEjb, archetypeDao);
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
        expect(pdoDao.findById(ProductOrder.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(pdoDao.findById(ProductOrder.class, entityId)).andReturn(pdo);

        expect(pdo.getProductOrderId()).andReturn(entityId).anyTimes();
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(pdo.getProduct()).andReturn(product).times(2);
        expect(pdo.getOrderStatus()).andReturn(orderStatus);
        expect(pdo.getCreatedDate()).andReturn(createdDate).times(2);
        expect(pdo.getModifiedDate()).andReturn(modifiedDate);
        expect(pdo.getTitle()).andReturn(title);
        expect(pdo.getQuoteId()).andReturn(quoteId);
        expect(pdo.getJiraTicketKey()).andReturn(jiraTicketKey).anyTimes();
        expect(pdo.getCreatedBy()).andReturn(createdBy);
        expect(userList.getById(createdBy)).andReturn(owner);
        expect(owner.getUsername()).andReturn(ownerName);
        expect(pdo.getPlacedDate()).andReturn(modifiedDate);
        expect(pdo.getSkipRegulatoryReason()).andReturn(null);
        expect(pdo.getRegulatoryInfos()).andReturn(regulatoryInfos);
        expect(pdo.getSapOrderNumber()).andReturn(sapMockOrderNumber);
        expect(pdo.getOrderType()).andReturn(ProductOrder.OrderAccessType.BROAD_PI_ENGAGED_WORK);

        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);
        expect(product.getProductId()).andReturn(productId);

        expect(productEjb.getGenotypingChip(pdo, createdDate)).andReturn(Pair.of(chipArchetypeGroup, arrayChipType));
        expect(archetypeDao.findGenotypingChip(chipArchetypeGroup, arrayChipType)).andReturn(genotypingChip);
        expect(genotypingChip.getAttribute("call_rate_threshold")).andReturn(new ArchetypeAttribute(genotypingChip, "call_rate_threshold", callThreshold));

        expect(archetypeDao.findGenotypingProductOrderMapping(entityId)).andReturn(genotypingProductOrderMapping);
        expect(genotypingProductOrderMapping.getAttributes()).andReturn(Collections.singleton(new ArchetypeAttribute(genotypingChip /* <-- ignored */, "call_rate_threshold", callThresholdOverride)));

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",", 19);
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
        assertEquals(parts[i++], "");// SkipRegulatoryReason
        assertEquals(parts[i++], sapMockOrderNumber);
        assertEquals(parts[i++], arrayChipType);
        assertEquals(parts[i++], callThresholdOverride);
        assertEquals(parts[i++], ""); // regInfoData
        assertEquals(parts[i++], ProductOrder.OrderAccessType.BROAD_PI_ENGAGED_WORK.getCompanyCode());
        assertEquals(parts.length, i);
    }
}

