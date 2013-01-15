package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.testng.Assert.*;

public class LibraryBeanTest {

    /**
     * Verifies that when a {@link BSPSampleDTO} is passed into
     * a {@link LibraryBean}, the bsp fields take priority
     * over their gssr counterparts
     */
    @Test(groups = DATABASE_FREE)
    public void test_bsp_dto_override() {
        String gssrLsid = "gssr:lsid";
        String bspLsid = "bsp:lsid";
        String disease = "cancer";
        String gssrMaterialType = "Genomic DNA";
        String bspMaterialType = "DNA: Genomic";
        String bspCollabSampleId = "bsp:collabId";
        String gssrCollabSampleId = "gssr:collabId";
        String gssrOrganism = "gssrOrg";
        String gssrSpecies = "gssrSpecies";
        String gssrStrain = "gssrStrain";
        String bspSpecies = "bsp species";
        String gssrParticipant = "gssrPatient";
        String bspParticipant = "bsp participant";
        BSPSampleDTO bspDto = new BSPSampleDTO(bspLsid,disease,bspMaterialType,bspCollabSampleId,bspSpecies,bspParticipant);

        // send in some GSSR sample attributes in addition to bsp DTO to verify GSSR override
        LibraryBean libraryBean = new LibraryBean(gssrLsid,gssrMaterialType,gssrCollabSampleId,gssrOrganism,gssrSpecies,gssrStrain,gssrParticipant,bspDto);

        assertEquals(libraryBean.getPrimaryDisease(),bspDto.getPrimaryDisease());
        assertEquals(libraryBean.getLsid(),bspDto.getSampleLsid());
        assertFalse(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),bspDto.getMaterialType());
        assertEquals(libraryBean.getCollaboratorSampleId(),bspCollabSampleId);
        assertEquals(libraryBean.getSpecies(),bspDto.getOrganism());
        assertEquals(libraryBean.getParticipantId(),bspDto.getPatientId());

        // new up sans bsp DTO to confirm gssr fields work
        libraryBean = new LibraryBean(gssrLsid,gssrMaterialType,gssrCollabSampleId,gssrOrganism,gssrSpecies,gssrStrain,gssrParticipant,null);
        assertEquals(libraryBean.getLsid(),gssrLsid);
        assertTrue(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),gssrMaterialType);
        assertEquals(libraryBean.getCollaboratorSampleId(),gssrCollabSampleId);
        assertEquals(libraryBean.getSpecies(),gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain);
        assertEquals(libraryBean.getParticipantId(),gssrParticipant);
    }

    /**
     * Verifies fields fetched from bsp
     * are accessible as {@link LibraryBean} fields
     */
    @Test(groups = DATABASE_FREE)
    public void test_bsp_fields() {
        BSPSampleSearchServiceStub bspSampleSearchServiceStub = new BSPSampleSearchServiceStub();
        BSPSampleDTO sampleDTO = new BSPSampleDataFetcher(bspSampleSearchServiceStub).fetchSingleSampleFromBSP(BSPSampleSearchServiceStub.SM_12CO4);

        LibraryBean libraryBean = new LibraryBean(null,null,null,null,null,null,null,sampleDTO);

        assertEquals(libraryBean.getGender(),sampleDTO.getGender());
        assertEquals(libraryBean.getLsid(),sampleDTO.getSampleLsid());
        assertEquals(libraryBean.getCollection(),sampleDTO.getCollection());
        assertEquals(libraryBean.getRootSample(),sampleDTO.getRootSample());
        assertEquals(libraryBean.getSpecies(),sampleDTO.getOrganism());
        assertEquals(libraryBean.getSampleId(),sampleDTO.getSampleId());
    }
}
