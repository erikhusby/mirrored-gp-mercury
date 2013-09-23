package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Date;
import java.util.UUID;

public class ProductTestFactory {
    public static Product createTestProduct() {
        String uuid = UUID.randomUUID().toString();
        return createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber " + uuid);
    }

    public static Product createDummyProduct(Workflow workflow, String partNumber) {
        return new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                                  new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements",
                                  "deliverables", true, workflow, false, "an aggregation data type");
    }
}
