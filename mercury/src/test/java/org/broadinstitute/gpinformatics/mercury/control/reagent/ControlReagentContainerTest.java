package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;

/**
 * Test persistence of control reagents.
 */
@Test(groups = TestGroups.STANDARD)
public class ControlReagentContainerTest {

    @Inject
    private ControlReagentFactory controlReagentFactory;

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("ControlReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        controlReagentFactory.make(testSpreadSheetInputStream, new MessageCollection());
    }
}
