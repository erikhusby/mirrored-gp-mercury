package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
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
public class WorkflowEtlDbFreeTest {
    private String datafileDir = System.getProperty("java.io.tmpdir");

    // Reuses the setup and values from WorkflowConfigLookupDbFreeTest
    private WorkflowConfig config = WorkflowConfigLookupDbFreeTest.buildWorkflowConfig();
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private WorkflowLoader loader = createMock(WorkflowLoader.class);
    private Object[] mocks = new Object[]{auditReader, loader};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {

        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);
    }

    public void testEtlFlags() throws Exception {

        replay(mocks);

        WorkflowEtl tst = new WorkflowEtl();
        tst.setAuditReaderDao(auditReader);
        tst.setWorkflowLoader(loader);

        assertEquals(tst.getEntityClass(), WorkflowConfig.class);

        assertEquals(tst.getBaseFilename(), "workflow");

        assertNull(tst.entityId(config));

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testEntityRecord() throws Exception {
        replay(mocks);

        WorkflowEtl tst = new WorkflowEtl();
        tst.setWorkflowLoader(loader);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, 1L).size(), 0);
        assertEquals(tst.entityRecordsInRange(0L, 9999999999999999L, etlDateStr, false).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(loader.load()).andReturn(config);
        replay(mocks);

        WorkflowEtl tst = new WorkflowEtl();
        tst.setAuditReaderDao(auditReader);
        tst.setWorkflowLoader(loader);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        assertFalse(datafile.exists());

        int count = tst.doEtl(null, etlDateStr);

        assertEquals(count, 3);
        verifyFile(datafile);
        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        expect(loader.load()).andReturn(config);
        replay(mocks);

        WorkflowEtl tst = new WorkflowEtl();
        tst.setWorkflowLoader(loader);
        tst.setAuditReaderDao(auditReader);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        assertFalse(datafile.exists());

        int count = tst.doBackfillEtl(tst.getEntityClass(), 0, 9999999999999L, etlDateStr);

        assertEquals(count, 3);
        verifyFile(datafile);
        verify(mocks);
    }

    private void verifyFile(File datafile) throws Exception {
        assertTrue(datafile.exists());
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        IOUtils.closeQuietly(reader);

        boolean[] found = new boolean[]{false, false, false};
        for (String line : lines) {
            if (verifyRecord(line, -8190592328589523234L, "Workflow 1", "1.0")) {
                found[0] = true;
            }
            if (verifyRecord(line, -8190592328589522273L, "Workflow 1", "2.0")) {
                found[1] = true;
            }
            if (verifyRecord(line, -8190592328588599713L, "Workflow 2", "1.0")) {
                found[2] = true;
            }
        }
        assertTrue(found[0]);
        assertTrue(found[1]);
        assertTrue(found[2]);
    }

    private boolean verifyRecord(String record, long id, String name, String version) {
        String[] parts = record.split(",");
        assertEquals(parts[1], etlDateStr);
        assertEquals(parts[2], "F");
        return parts[3].equals(String.valueOf(id))
                && parts[4].equals(name)
                && parts[5].equals(version);
    }
}

