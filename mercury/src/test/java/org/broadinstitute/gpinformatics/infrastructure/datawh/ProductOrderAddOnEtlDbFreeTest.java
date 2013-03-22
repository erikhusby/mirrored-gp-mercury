package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
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
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderAddOnEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private long productId = 332891L;
    private long pdoId = 98789798L;
    private ProductOrderAddOnEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ProductOrderDao dao = createMock(ProductOrderDao.class);
    private ProductOrder pdo = createMock(ProductOrder.class);
    private ProductOrderAddOn obj = createMock(ProductOrderAddOn.class);
    private Product product = createMock(Product.class);
    private Object[] mocks = new Object[]{auditReader, dao, pdo, obj, product};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        tst = new ProductOrderAddOnEtl();
        tst.setProductOrderDao(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductOrderAddOnId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, ProductOrderAddOn.class);
        assertEquals(tst.baseFilename, "product_order_add_on");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderAddOn.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ProductOrderAddOn.class, entityId)).andReturn(obj);
        expect(obj.getProductOrderAddOnId()).andReturn(entityId);
        expect(obj.getProductOrder()).andReturn(pdo).times(2);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getAddOn()).andReturn(product);
        expect(product.getProductId()).andReturn(productId);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(pdoId));
        assertEquals(parts[i++], String.valueOf(productId));
        assertEquals(parts.length, i);
    }

}

