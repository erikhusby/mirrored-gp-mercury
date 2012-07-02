package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.infrastructure.quote.PriceItem;

public class BillingAnnotation implements WorkflowAnnotation {

    public PriceItem getPriceItem() {
        throw new RuntimeException("I haven't been written yet.");
    }

}
