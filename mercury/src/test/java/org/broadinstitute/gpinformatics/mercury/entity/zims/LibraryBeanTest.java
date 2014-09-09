package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class LibraryBeanTest {

    /**
     * Verifies that when a {@link BSPSampleDTO} is passed into a {@link LibraryBean}, the bsp fields take priority
     * over their gssr counterparts.
     */
    @Test(groups = DATABASE_FREE)
    public void testBspDtoOverride() {
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

        BSPSampleDTO bspDto = new BSPSampleDTO(dataMap);

        // send in some GSSR sample attributes in addition to bsp DTO to verify GSSR override.
        LibraryBean libraryBean = new LibraryBean(gssrLsid, gssrMaterialType, gssrCollabSampleId, gssrOrganism,
                gssrSpecies, gssrStrain, gssrParticipant, bspDto, Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(),
                LibraryBean.NO_PDO_SAMPLE, libraryCreationDate);

        assertEquals(libraryBean.getPrimaryDisease(),bspDto.getPrimaryDisease());
        assertEquals(libraryBean.getLsid(),bspDto.getSampleLsid());
        assertFalse(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),bspDto.getMaterialType());
        assertEquals(libraryBean.getCollaboratorSampleId(),bspCollabSampleId);
        assertEquals(libraryBean.getSpecies(),bspDto.getOrganism());
        assertEquals(libraryBean.getParticipantId(),bspDto.getPatientId());

        // new up sans bsp DTO to confirm gssr fields work.
        libraryBean = new LibraryBean(gssrLsid, gssrMaterialType, gssrCollabSampleId, gssrOrganism, gssrSpecies,
                gssrStrain, gssrParticipant, null, Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(),
                LibraryBean.NO_PDO_SAMPLE, libraryCreationDate);
        assertEquals(libraryBean.getLsid(),gssrLsid);
        assertTrue(libraryBean.getIsGssrSample());
        assertEquals(libraryBean.getMaterialType(),gssrMaterialType);
        assertEquals(libraryBean.getCollaboratorSampleId(),gssrCollabSampleId);
        assertEquals(libraryBean.getSpecies(),gssrOrganism + ":" + gssrSpecies + ":" + gssrStrain);
        assertEquals(libraryBean.getCollaboratorParticipantId(),gssrParticipant);
    }

    /**
     * Verifies fields fetched from bsp are accessible as {@link LibraryBean} fields.
     */
    @Test(groups = DATABASE_FREE)
    public void testBspFields() {
        BSPSampleSearchServiceStub bspSampleSearchServiceStub = new BSPSampleSearchServiceStub();
        BSPSampleDTO sampleDTO =
            new SampleDataFetcher(
                bspSampleSearchServiceStub).fetchSingleSampleFromBSP(BSPSampleSearchServiceStub.SM_12CO4);

        LibraryBean libraryBean = new LibraryBean(null, null, null, null, null, null, null, sampleDTO, null,
                LibraryBean.NO_PDO_SAMPLE, null);

        assertEquals(libraryBean.getGender(), StringUtils.trimToNull(sampleDTO.getGender()));
        assertEquals(libraryBean.getLsid(), StringUtils.trimToNull(sampleDTO.getSampleLsid()));
        assertEquals(libraryBean.getCollection(), StringUtils.trimToNull(sampleDTO.getCollection()));
        assertEquals(libraryBean.getRootSample(), StringUtils.trimToNull(sampleDTO.getRootSample()));
        assertEquals(libraryBean.getSpecies(), StringUtils.trimToNull(sampleDTO.getOrganism()));
        assertEquals(libraryBean.getSampleId(), StringUtils.trimToNull(sampleDTO.getSampleId()));
    }
}
