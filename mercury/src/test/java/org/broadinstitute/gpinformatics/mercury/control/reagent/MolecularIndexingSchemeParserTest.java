package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Test the parser
 */
@Test(groups = TestGroups.STUBBY)
public class MolecularIndexingSchemeParserTest extends ContainerTest {

    @Inject
    private MolecularIndexingSchemeParser molecularIndexingSchemeParser;

    /**
     * Verify that dual indexes generate correct names.
     */
    @Test
    public void testAutoName() {
        String file = "ILLUMINA_P5\tILLUMINA_P7\n" +
                      "TGCGTGAGCTGTACAT\tCAGTAGCGCACTGAGC\n" +
                      "TGCGTGAGCTGTACAT\tCGCGTGCAGAGTGTCA\n";
        List<MolecularIndexingScheme> molecularIndexingSchemes =
                molecularIndexingSchemeParser.parse(new ByteArrayInputStream(file.getBytes()));
        Assert.assertEquals(molecularIndexingSchemes.size(), 2);
        Assert.assertEquals(molecularIndexingSchemes.get(0).getName(), "Illumina_P5-Yetideyocau_P7-Hiwikeconim");
        Assert.assertEquals(molecularIndexingSchemes.get(1).getName(), "Illumina_P5-Yetideyocau_P7-Ketihidoteg");
    }

    /**
     * Verify that grand-fathered names are honored.
     */
    @Test
    public void testSuppliedName() {
        String file = "NAME\tILLUMINA_P7\n" +
                      "tagged_100\tACACGATC\n";
        List<MolecularIndexingScheme> molecularIndexingSchemes = molecularIndexingSchemeParser.parse(
                new ByteArrayInputStream(file.getBytes()));
        Assert.assertEquals(molecularIndexingSchemes.size(), 1);
        Assert.assertEquals(molecularIndexingSchemes.get(0).getName(), "tagged_100");
    }
}
