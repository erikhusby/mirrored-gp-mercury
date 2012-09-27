package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.common.Id;

/**
 * This holds either the Grant or the PO name
 */
public abstract class FundingId extends Id {
    public FundingId(String value) {
        super(value);
    }
}
