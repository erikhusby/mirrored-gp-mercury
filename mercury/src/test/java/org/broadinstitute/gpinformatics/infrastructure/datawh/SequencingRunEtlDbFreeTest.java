package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class SequencingRunEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private String runName = "adfASDF0890821lkmxzcv-09zuvb";
    private Date runDate = new Date(1354000000000L);
    private String barcode = "012348909";
    private String machineName = "The \"terminator\"";
    private long operator = 1234L;

    private SequencingRun run;
    private SequencingRunEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private IlluminaSequencingRunDao dao = createMock(IlluminaSequencingRunDao.class);

    private Object[] mocks = new Object[]{auditReader, dao};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
        run = new SequencingRun(runName, barcode, machineName, operator, false, runDate, new IlluminaFlowcell(),
                "/some/dirname");
        run.setSequencingRunId(entityId);
        tst = new SequencingRunEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        replay(mocks);

        assertEquals(tst.entityClass, SequencingRun.class);
        assertEquals(tst.baseFilename, "sequencing_run");
        assertEquals(tst.entityId(run), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(SequencingRun.class, -1L)).andReturn(null);
        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(SequencingRun.class, entityId)).andReturn(run);
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
        assertEquals(parts[i++], runName);
        assertEquals(parts[i++], barcode);
        assertEquals(parts[i++], ExtractTransform.secTimestampFormat.format(runDate));
        assertEquals(parts[i++], machineName);
        assertEquals(parts.length, i);
    }
}

