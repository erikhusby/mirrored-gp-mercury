package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowConfigEtlDbFreeTest {
    private String datafileDir = System.getProperty("java.io.tmpdir");

    // Reuses the setup and values from WorkflowConfigLookupDbFreeTest
    private WorkflowConfig config = WorkflowConfigLookupDbFreeTest.buildWorkflowConfig();
    private Collection<WorkflowConfigDenorm> configs = new ArrayList<>();
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private final int EXPECTED_RECORD_COUNT = 15;
    private WorkflowConfigEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private WorkflowConfigLookup loader = createMock(WorkflowConfigLookup.class);
    private Object[] mocks = new Object[]{auditReader, loader};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);

        configs.clear();
        configs.addAll(WorkflowConfigDenorm.parse(config));

        tst = new WorkflowConfigEtl(loader);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {

        replay(mocks);

        assertEquals(tst.entityClass, WorkflowConfig.class);
        assertEquals(tst.baseFilename, WorkflowConfigEtl.WORKFLOW_BASE_FILENAME);

        boolean gotIt = false;
        try {
            tst.entityId(config);
        } catch (RuntimeException e) {
            gotIt = true;
        }
        assertTrue(gotIt);

        verify(mocks);
    }

    public void testEntityRecord() throws Exception {
        replay(mocks);
        boolean gotIt = false;
        try {
            tst.dataRecords(etlDateString, false, 1L);
        } catch (RuntimeException e) {
            gotIt = true;
        }
        assertTrue(gotIt);

        gotIt = false;
        try {
            tst.entitiesInRange(0L, 9999999999999999L);
        } catch (RuntimeException e) {
            gotIt = true;
        }
        assertTrue(gotIt);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(loader.getDenormConfigs()).andReturn(configs);
        replay(mocks);

        File workflowDatafile = new File(datafileDir, etlDateString + "_" + WorkflowConfigEtl.WORKFLOW_BASE_FILENAME + ".dat");
        File processDatafile = new File(datafileDir, etlDateString + "_" + WorkflowConfigEtl.PROCESS_BASE_FILENAME + ".dat");
        assertFalse(workflowDatafile.exists());
        assertFalse(processDatafile.exists());

        assertEquals(tst.doIncrementalEtl(Collections.<Long>emptySet(), etlDateString), EXPECTED_RECORD_COUNT);

        verifyWorkflowFile(workflowDatafile);
        verifyProcessFile(processDatafile);
        verify(mocks);
    }

    private void verifyWorkflowFile(File datafile) throws Exception {
        assertTrue(datafile.exists());
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        IOUtils.closeQuietly(reader);

        boolean[] found = new boolean[]{false, false, false};
        for (String line : lines) {
            if (verifyWorkflowRecord(line, -8190592328589523234L, "Workflow 1", "1.0")) {
                assertFalse(found[0]);
                found[0] = true;
            } else if (verifyWorkflowRecord(line, -8190592328589522273L, "Workflow 1", "2.0")) {
                assertFalse(found[1]);
                found[1] = true;
            } else if (verifyWorkflowRecord(line, -8190592328588599713L, "Workflow 2", "1.0")) {
                assertFalse(found[2]);
                found[2] = true;
            } else {
                fail("Unexpected workflow record: " + line);
            }
        }
        assertTrue(found[0]);
        assertTrue(found[1]);
        assertTrue(found[2]);
    }

    private boolean verifyWorkflowRecord(String record, long id, String name, String version) {
        String[] parts = record.split(",");
        assertEquals(parts[1], etlDateString);
        assertEquals(parts[2], "F");
        return parts[3].equals(String.valueOf(id))
                && parts[4].equals(name)
                && parts[5].equals(version);
    }

    private void verifyProcessFile(File datafile) throws Exception {
        assertTrue(datafile.exists());
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        IOUtils.closeQuietly(reader);

        boolean[] found = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false};
        for (String line : lines) {
            if (verifyProcessRecord(line, -8467717764871572157L, "Process 1", "1.0", "Step 1", "GSWash1")) {
                assertFalse(found[0]);
                found[0] = true;
            } else if (verifyProcessRecord(line, -8467716911980534715L, "Process 1", "1.0", "Step 2", "GSWash2")) {
                assertFalse(found[1]);
                found[1] = true;
            } else if (verifyProcessRecord(line, -8467716059089497273L, "Process 1", "1.0", "Step 3", "GSWash3")) {
                assertFalse(found[2]);
                found[2] = true;
            } else if (verifyProcessRecord(line, -3305996820808558747L, "Process 1", "2.0", "Step 1", "GSWash4")) {
                assertFalse(found[3]);
                found[3] = true;
            } else if (verifyProcessRecord(line, -3305995967917521305L, "Process 1", "2.0", "Step 2", "GSWash5")) {
                assertFalse(found[4]);
                found[4] = true;
            } else if (verifyProcessRecord(line, -3305995115026483863L, "Process 1", "2.0", "Step 3", "GSWash6")) {
                assertFalse(found[5]);
                found[5] = true;
            } else if (verifyProcessRecord(line, 1855724123254454657L, "Process 1", "3.0", "Step 1", "GSWash1")) {
                assertFalse(found[6]);
                found[6] = true;
            } else if (verifyProcessRecord(line, 1855724976145492099L, "Process 1", "3.0", "Step 2", "GSWash2")) {
                assertFalse(found[7]);
                found[7] = true;
            } else if (verifyProcessRecord(line, 5742336828830808460L, "Process 2", "1.0", "Step 1", "SageLoading")) {
                assertFalse(found[8]);
                found[8] = true;
            } else if (verifyProcessRecord(line, -384411111648070892L, "Process 2", "1.0", "Step 2", "SageLoaded")) {
                assertFalse(found[9]);
                found[9] = true;
            } else if (verifyProcessRecord(line, 4064078523236908373L, "Process 2", "1.0", "Step 3", "SageUnloading")) {
                assertFalse(found[10]);
                found[10] = true;
            } else if (verifyProcessRecord(line, 8105325172126646903L, "Process 2", "1.0", "Step 4", "SageCleanup")) {
                assertFalse(found[11]);
                found[11] = true;
            } else {
                fail("Unexpected process record: " + line);
            }
        }

        for (int i = 0; i < found.length; ++i) {
            assertTrue(found[0], "Did not find [" + i + "]");
        }
    }

    private boolean verifyProcessRecord(String record, long id, String name, String version, String step, String event) {
        String[] parts = record.split(",");
        assertEquals(parts.length, 8);
        assertEquals(parts[1], etlDateString);
        assertEquals(parts[2], "F");
        return parts[3].equals(String.valueOf(id))
                && parts[4].equals(name)
                && parts[5].equals(version)
                && parts[6].equals(step)
                && parts[7].equals(event);
    }

}

