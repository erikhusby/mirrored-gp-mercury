package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;

import java.util.Date;
import java.util.UUID;

public class ProductTestFactory {
    public static Product createTestProduct() {
        String uuid = UUID.randomUUID().toString();
        return createDummyProduct("workflowName " + uuid, "partNumber " + uuid);
    }

    public static Product createDummyProduct(String workflowName, String partNumber) {
        return new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                                  new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements",
                                  "deliverables", true, workflowName, false, "an aggregation data type");
    }
}
