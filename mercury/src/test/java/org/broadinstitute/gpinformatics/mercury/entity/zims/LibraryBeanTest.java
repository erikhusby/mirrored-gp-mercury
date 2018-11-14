package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class LibraryBeanTest {

    private String testType;

    /**
     * Verifies that when a {@link BspSampleData} is passed into a {@link LibraryBean}, the bsp fields take priority
     * over their gssr counterparts.
     */
    @Test(groups = DATABASE_FREE)
    public void testBspSampleOverride() {
        final String gssrLsid = "gssr:lsid";
        final String bspLsid = "bsp:lsid";
        final String disease = "cancer";
        final String gssrMaterialType = "Genomic DNA";
        final String bspMaterialType = "DNA: Genomic";
        final String bspCollabSampleId = "bsp:collabId";
        final String gssrCollabSampleId = "gssr:collabId";
        final String gssrOrganism = "gssrOrg";
        final String gssrSpecies = "gssrSpecies";
        final String gssrStrain = "gssrStrain";
        final String bspSpecies = "bsp species";
        final String gssrParticipant = "gssrPatient";
        final String bspParticipant = "bsp participant";
        final String libraryCreationDate = "01/15/2014 12:34:56";

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, disease);
            put(BSPSampleSearchColumn.LSID, bspLsid);
            put(BSPSampleSearchColumn.MATERIAL_TYPE, bspMaterialType);
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, bspCollabSampleId);
            put(BSPSampleSearchColumn.SPECIES, bspSpecies);
            put(BSPSampleSearchColumn.PARTICIPANT_ID, bspParticipant);
        }};

        SampleData sampleData = new BspSampleData(dataMap);

        // send in some GSSR sample attributes in addition to bsp sample data to verify GSSR override.
        LibraryBean libraryBean = new LibraryBean(gssrLsid, gssrMaterialType, gssrCollabSampleId, gssrOrganism,
                gssrSpecies, gssrStrain, gssrParticipant, sampleData, Workflow.AGILENT_EXOME_EXPRESS,
                LibraryBean.NO_PDO_SAMPLE, libraryCreationDate);

        assertEquals(libraryBean.getPrimaryDisease(), sampleData.getPrimaryDisease());
        assertEquals(libraryBean.getLsid(),sampleData.getSampleLsid());
        assertFalse(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),sampleData.getMaterialType());
        assertEquals(libraryBean.getCollaboratorSampleId(), bspCollabSampleId);
        assertEquals(libraryBean.getSpecies(), sampleData.getOrganism());
        assertEquals(libraryBean.getParticipantId(),sampleData.getPatientId());
        assertNull(libraryBean.getTestType());

        // new up sans bspSampleData to confirm gssr fields work.
        libraryBean = new LibraryBean(gssrLsid, gssrMaterialType, gssrCollabSampleId, gssrOrganism, gssrSpecies,
                gssrStrain, gssrParticipant, null, Workflow.AGILENT_EXOME_EXPRESS,
                LibraryBean.NO_PDO_SAMPLE, libraryCreationDate);
        assertEquals(libraryBean.getLsid(),gssrLsid);
        assertTrue(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),gssrMaterialType);
        assertEquals(libraryBean.getCollaboratorSampleId(),gssrCollabSampleId);
        assertEquals(libraryBean.getSpecies(),gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain);
        assertEquals(libraryBean.getCollaboratorParticipantId(),gssrParticipant);
        assertNull(libraryBean.getTestType());
    }

    @Test(groups = DATABASE_FREE)
    public void testTestTypeIsNullForEmptyConstructor() {
        Assert.assertEquals(new LibraryBean().getTestType(), null);
    }

    /**
     * Verifies fields fetched from bsp are accessible as {@link LibraryBean} fields.
     */
    @Test(groups = DATABASE_FREE)
    public void testBspFields() {
        Map<BSPSampleSearchColumn, String> sampleAttributes = new HashMap<>();
        sampleAttributes.put(BSPSampleSearchColumn.GENDER, BspSampleData.FEMALE_IND);
        sampleAttributes.put(BSPSampleSearchColumn.LSID, "broadinstitute.org:bsp.prod.sample:1234");
        sampleAttributes.put(BSPSampleSearchColumn.COLLECTION, "LibraryBeanTest Collection");
        sampleAttributes.put(BSPSampleSearchColumn.ROOT_SAMPLE, "ABC");
        sampleAttributes.put(BSPSampleSearchColumn.SPECIES, "Test Species");
        sampleAttributes.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        SampleData sampleData = new BspSampleData(sampleAttributes);

        LibraryBean libraryBean = new LibraryBean(null, null, null, null, null, null, null, sampleData, null,
                LibraryBean.NO_PDO_SAMPLE, null);

        assertEquals(libraryBean.getGender(), StringUtils.trimToNull(sampleData.getGender()));
        assertEquals(libraryBean.getLsid(), StringUtils.trimToNull(sampleData.getSampleLsid()));
        assertEquals(libraryBean.getCollection(), StringUtils.trimToNull(sampleData.getCollection()));
        assertEquals(libraryBean.getRootSample(), StringUtils.trimToNull(sampleData.getRootSample()));
        assertEquals(libraryBean.getSpecies(), StringUtils.trimToNull(sampleData.getOrganism()));
        assertEquals(libraryBean.getSampleId(), StringUtils.trimToNull(sampleData.getSampleId()));
        assertNull(libraryBean.getTestType());
    }

    public String getTestType() {
        return testType;
    }
}
