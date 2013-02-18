package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
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
public class ProcessEtlDbFreeTest {
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

        ProcessEtl tst = new ProcessEtl();
        tst.setAuditReaderDao(auditReader);
        tst.setWorkflowLoader(loader);

        assertEquals(tst.getEntityClass(), WorkflowConfig.class);

        assertEquals(tst.getBaseFilename(), "process");

        assertNull(tst.entityId(config));

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testEntityRecord() throws Exception {
        replay(mocks);

        ProcessEtl tst = new ProcessEtl();
        tst.setWorkflowLoader(loader);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, 1L).size(), 0);
        assertEquals(tst.entityRecordsInRange(0L, 9999999999999999L, etlDateStr, false).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(loader.load()).andReturn(config);
        replay(mocks);

        ProcessEtl tst = new ProcessEtl();
        tst.setAuditReaderDao(auditReader);
        tst.setWorkflowLoader(loader);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        assertFalse(datafile.exists());

        int count = tst.doEtl(null, etlDateStr);

        assertEquals(count, 12);
        verifyFile(datafile);
        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        expect(loader.load()).andReturn(config);
        replay(mocks);

        ProcessEtl tst = new ProcessEtl();
        tst.setWorkflowLoader(loader);
        tst.setAuditReaderDao(auditReader);

        String dataFilename = etlDateStr + "_" + tst.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        assertFalse(datafile.exists());

        int count = tst.doBackfillEtl(tst.getEntityClass(), 0, 9999999999999L, etlDateStr);

        assertEquals(count, 12);
        verifyFile(datafile);
        verify(mocks);
    }

    private void verifyFile(File datafile) throws Exception {
        assertTrue(datafile.exists());
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        IOUtils.closeQuietly(reader);

        boolean[] found = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false};
        for (String line : lines) {
            if (verifyRecord(line, -8467717764871572157L, "Process 1", "1.0", "Step 1", "GSWash1")) {
                found[0] = true;
            }
            if (verifyRecord(line, -8467716911980534715L, "Process 1", "1.0", "Step 2", "GSWash2")) {
                found[1] = true;
            }
            if (verifyRecord(line, -8467716059089497273L, "Process 1", "1.0", "Step 3", "GSWash3")) {
                found[2] = true;
            }
            if (verifyRecord(line, -3305996820808558747L, "Process 1", "2.0", "Step 1", "GSWash4")) {
                found[3] = true;
            }
            if (verifyRecord(line, -3305995967917521305L, "Process 1", "2.0", "Step 2", "GSWash5")) {
                found[4] = true;
            }
            if (verifyRecord(line, -3305995115026483863L, "Process 1", "2.0", "Step 3", "GSWash6")) {
                found[5] = true;
            }
            if (verifyRecord(line, 1855724123254454657L, "Process 1", "3.0", "Step 1", "GSWash1")) {
                found[6] = true;
            }
            if (verifyRecord(line, 1855724976145492099L, "Process 1", "3.0", "Step 2", "GSWash2")) {
                found[7] = true;
            }
            if (verifyRecord(line, 5742336828830808460L, "Process 2", "1.0", "Step 1", "SageLoading")) {
                found[8] = true;
            }
            if (verifyRecord(line, -384411111648070892L, "Process 2", "1.0", "Step 2", "SageLoaded")) {
                found[9] = true;
            }
            if (verifyRecord(line, 4064078523236908373L, "Process 2", "1.0", "Step 3", "SageUnloading")) {
                found[10] = true;
            }
            if (verifyRecord(line, 8105325172126646903L, "Process 2", "1.0", "Step 4", "SageCleanup")) {
                found[11] = true;
            }
        }

        for (int i = 0; i < found.length; ++i) {
            assertTrue(found[0], "Did not find [" + i + "]");
        }
    }

    private boolean verifyRecord(String record, long id, String name, String version, String step, String event) {
        String[] parts = record.split(",");
        assertEquals(parts[1], etlDateStr);
        assertEquals(parts[2], "F");
        return parts[3].equals(String.valueOf(id))
                && parts[4].equals(name)
                && parts[5].equals(version)
                && parts[6].equals(step)
                && parts[7].equals(event);
    }
}

