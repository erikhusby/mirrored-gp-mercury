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
        BSPSampleDTO bspDto = new BSPSampleDTO(bspLsid,disease);

        // send in some GSSR sample attributes in addition to bsp DTO to verify GSSR override
        LibraryBean libraryBean = new LibraryBean(gssrLsid,bspDto);

        assertEquals(libraryBean.getPrimaryDisease(),bspDto.getPrimaryDisease());
        assertEquals(libraryBean.getLsid(),bspDto.getSampleLsid());

        // new up sans bsp DTO to confirm gssr fields work
        libraryBean = new LibraryBean(gssrLsid,null);
        assertEquals(libraryBean.getLsid(),gssrLsid);
    }

    /**
     * Verifies fields fetched from bsp
     * are accessible as {@link LibraryBean} fields
     */
    @Test(groups = DATABASE_FREE)
    public void test_bsp_fields() {
        BSPSampleSearchServiceStub bspSampleSearchServiceStub = new BSPSampleSearchServiceStub();
        BSPSampleDTO sampleDTO = new BSPSampleDataFetcher(bspSampleSearchServiceStub).fetchSingleSampleFromBSP(BSPSampleSearchServiceStub.SM_12CO4);

        LibraryBean libraryBean = new LibraryBean(null,sampleDTO);

        assertEquals(libraryBean.getBSpGender(),sampleDTO.getGender());
        assertEquals(libraryBean.getLsid(),sampleDTO.getSampleLsid());
        assertEquals(libraryBean.getBspCollection(),sampleDTO.getCollection());
        assertEquals(libraryBean.getBspRootSample(),sampleDTO.getRootSample());
        assertEquals(libraryBean.getBspSpecies(),sampleDTO.getOrganism());
        assertEquals(libraryBean.getBspSampleId(),sampleDTO.getSampleId());
        assertEquals(libraryBean.getBspCollaboratorName(),sampleDTO.getCollaboratorName());
    }
}
