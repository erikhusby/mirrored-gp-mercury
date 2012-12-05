package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
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
}
