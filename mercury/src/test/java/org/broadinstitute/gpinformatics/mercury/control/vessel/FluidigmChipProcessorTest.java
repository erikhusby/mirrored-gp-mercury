package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class FluidigmChipProcessorTest {

    public static final String FLUIDIGM_OUTPUT_CSV = "FluidigmOutput.csv";

    @Test
    public void testParseBasic() throws Exception {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(FLUIDIGM_OUTPUT_CSV);
        FluidigmChipProcessor fluidigmChipProcessor = new FluidigmChipProcessor();
        FluidigmChipProcessor.FluidigmRun run  = fluidigmChipProcessor.parse(testSpreadSheet);
        Assert.assertNotNull(run);
        Assert.assertFalse(fluidigmChipProcessor.getMessageCollection().hasErrors());
    }

}