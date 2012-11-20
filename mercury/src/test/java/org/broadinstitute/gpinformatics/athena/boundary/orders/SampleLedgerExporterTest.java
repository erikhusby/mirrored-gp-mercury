package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLedgerExporterTest {

    @Test
    void testExport() throws IOException {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = AthenaClientServiceStub.createDummyProductOrder();
        SampleLedgerExporter exporter = new SampleLedgerExporter(orders, null, null);

        File test = File.createTempFile("SampleLedgerExporterTest", ".xls");
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(test);
            exporter.writeToStream(outputStream);

            // TODO: ensure that output file is correct!
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
