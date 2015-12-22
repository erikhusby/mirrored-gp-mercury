package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
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
    private final String actualReadStructure = "101T8B8B101T";
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private long entityId = 1122334455L;
    private String runName = "adfASDF0890821lkmxzcv-09zuvb";
    private Date runDate = new Date(1354000000000L);
    private String barcode = "012348909";
    private String machineName = "The \"terminator\"";
    private long operator = 1234L;

    private IlluminaSequencingRun run;
    private SequencingRunEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private IlluminaSequencingRunDao dao = createMock(IlluminaSequencingRunDao.class);

    private Object[] mocks = new Object[]{auditReader, dao};
    private String setupReadStructure = "71T8B8B101T";

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
        run = new IlluminaSequencingRun(new IlluminaFlowcell(), runName, barcode, machineName, operator, false, runDate,
                "/some/dirname");
        run.setSetupReadStructure(setupReadStructure);
        run.setActualReadStructure(actualReadStructure);
        run.setSequencingRunId(entityId);
        tst = new SequencingRunEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        replay(mocks);

        assertEquals(tst.entityClass, IlluminaSequencingRun.class);
        assertEquals(tst.baseFilename, "sequencing_run");
        assertEquals(tst.entityId(run), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(IlluminaSequencingRun.class, -1L)).andReturn(null);
        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(IlluminaSequencingRun.class, entityId)).andReturn(run);
        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",", 9);
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], runName);
        assertEquals(parts[i++], barcode);
        assertEquals(parts[i++], ExtractTransform.formatTimestamp(runDate));
        assertEquals(parts[i++], machineName);
        assertEquals(parts[i++], setupReadStructure);
        assertEquals(parts[i++], actualReadStructure);
        assertEquals(parts.length, i);
    }
}

