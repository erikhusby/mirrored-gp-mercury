package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterTest {

    @Test
    void testExport() throws IOException {

        SampleLedgerExporter exporter =
                new SampleLedgerExporter(null,
                        Collections.singletonList(ProductOrderFactory.createDummyProductOrder()));

        File test = File.createTempFile("SampleLedgerExporterTest", ".xls");
        OutputStream outputStream = new FileOutputStream(test);
        try {
            exporter.writeToStream(outputStream);

            // TODO: ensure that output file is correct!
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
