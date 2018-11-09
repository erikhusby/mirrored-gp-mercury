package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Date;

/**
 * Utility to apply some 7/25 production data to dev database
 * for testing the pipeline API.
 */
@Test(groups = TestGroups.FIXUP)
public class Seven25MiSeqFixupTest extends StubbyContainerTest {

    @Inject
    SolexaRunResource runResource;


    @Test(enabled = false)
    public void testRegisterMiseq() {
        SolexaRunBean runBean = new SolexaRunBean("A4834",
                "A4834130712",
                new Date(),
                "SL-MAF",
                "/tmp",
                "MS2004527-50V2");

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(uriInfoMock);
        runResource.createRun(runBean,uriInfoMock);
    }

    @Test(enabled = false)
    public void testRegister2500() {
        SolexaRunBean runBean = new SolexaRunBean("H735BADXX",
                "H735BADXX130715",
                new Date(),
                "SL-HDM",
                "/tmp",
                null);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(uriInfoMock);
        runResource.createRun(runBean,uriInfoMock);
    }

    @Test(enabled = false)
    public void testUpdateReadStructure() {

        ReadStructureRequest req = new ReadStructureRequest();
        req.setRunBarcode("H735BADXX130715");
        req.setImagedArea(999.293923);
        req.setActualReadStructure("76T8B8B76T");
        req.setSetupReadStructure("76T8B8B76T");
        req.setLanesSequenced("5");


        runResource.storeRunReadStructure(req);

    }


}
