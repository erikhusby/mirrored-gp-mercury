package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.EnversAudit;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.RevisionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;

/**
 * dbfree unit test of GenericEntityEtl as implemented by LabBatch etl.
  */

@Test(groups = TestGroups.DATABASE_FREE)
public class GenericEntityEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private final long entityId = 1122334455L;
    private final String label = "012345678";
    private final LabVessel.ContainerType type = LabVessel.ContainerType.TUBE;
    private String vesselName = "vessel_name";
    private Date vesselCreated = new Date();
    private BigDecimal vesselVolume = new BigDecimal("101.999");

    private final String datafileDir = System.getProperty("java.io.tmpdir");

    private final AuditReaderDaoStub auditReader = createMock(AuditReaderDaoStub.class);
    private final LabVessel obj = createMock(LabVessel.class);
    private final LabVesselDao dao = createMock(LabVesselDao.class);
    private final Object[] mocks = new Object[]{auditReader, dao, obj};
    
    private final LabVesselEtl tst = new LabVesselEtl(dao);
    private final RevInfo[] revInfo = new RevInfo[] {new RevInfo(), new RevInfo(), new RevInfo()};

    private class AuditReaderDaoStub extends AuditReaderDao {
        @Override
        public void clear(){
            // Do nothing in DBFree test
        }
    }

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void beforeClass() {
        for (int i = 0; i < revInfo.length; ++i) {
            revInfo[i].setRevInfoId(i + 1L);
            revInfo[i].setRevDate(new Date(1355000000000L + i * 1000000000L));
        }
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }


    public void testEtl() throws Exception {
        Set<Long> revIds = new HashSet<>();
        revIds.add(entityId);

        List<EnversAudit> enversAudits = new ArrayList<>();
        enversAudits.add(new EnversAudit(obj, revInfo[0], RevisionType.ADD));

        expect(auditReader.fetchEnversAudits(revIds, tst.entityClass)).andReturn(enversAudits);
        expect(dao.findById(LabVessel.class, entityId)).andReturn(obj);

        expect(obj.getLabVesselId()).andReturn(entityId).times(2);
        expect(obj.getLabel()).andReturn(label);
        expect(obj.getType()).andReturn(type);
        expect(obj.getName()).andReturn(vesselName);
        expect(obj.getCreatedOn()).andReturn(vesselCreated);
        expect(obj.getVolume()).andReturn(vesselVolume);

        auditReader.clear();

        replay(mocks);

        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.doIncrementalEtl(revIds, etlDateStr);

        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.baseFilename + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(mocks);
    }

    public void testDeletionEtl() throws Exception {
        Set<Long> revIds = new HashSet<>();
        revIds.add(entityId);

        // Three changes to one entity result in one deletion record.
        List<EnversAudit> dataChanges = new ArrayList<>();
        dataChanges.add(new EnversAudit(obj, revInfo[0], RevisionType.ADD));
        dataChanges.add(new EnversAudit(obj, revInfo[1], RevisionType.MOD));
        dataChanges.add(new EnversAudit(obj, revInfo[2], RevisionType.DEL));

        expect(auditReader.fetchEnversAudits(eq(revIds), (Class) anyObject())).andReturn(dataChanges);
        expect(obj.getLabVesselId()).andReturn(entityId).times(3);

        auditReader.clear();

        replay(mocks);

        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.doIncrementalEtl(revIds, etlDateStr);
        assertEquals(recordCount, 1);

        String dataFilename = etlDateStr + "_" + tst.baseFilename + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        verify(mocks);
    }

    public void testWriteDatFile() throws Exception {
        Collection<Long> deletes = new ArrayList<>();
        Collection<Long> mods = new ArrayList<>();
        Collection<Long> adds = new ArrayList<>();
        Collection<LabVesselEtl.RevInfoPair<LabVessel>> revInfoPairs = new ArrayList<>();

        deletes.add(entityId);
        adds.add(entityId);
        mods.add(entityId);

        expect(dao.findById(LabVessel.class, entityId)).andReturn(obj).times(2);
        expect(obj.getLabVesselId()).andReturn(entityId).times(2);
        expect(obj.getLabel()).andReturn(label).times(2);
        expect(obj.getType()).andReturn(type).times(2);
        expect(obj.getName()).andReturn(vesselName).times(2);
        expect(obj.getCreatedOn()).andReturn(vesselCreated).times(2);
        expect(obj.getVolume()).andReturn(vesselVolume).times(2);
        replay(mocks);

        tst.setAuditReaderDao(auditReader);

        int recordCount = tst.writeEtlDataFile(deletes, mods, adds, revInfoPairs, etlDateStr);
        assertEquals(recordCount, 3);

        String datFileEnding = tst.baseFilename + ".dat";
        String dataFilename = etlDateStr + "_" + datFileEnding;
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "T", entityId));
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", entityId));
        verify(mocks);
    }

}
