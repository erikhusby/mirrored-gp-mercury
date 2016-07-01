package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.sap.services.SAPIntegrationException;

import javax.enterprise.inject.Alternative;

@Stub
@Alternative
public class SapIntegrationClientStub implements SapIntegrationService {
    @Override
    public String submitAge(String age) {
        return "What? Just "+age+" - Great !";
    }

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String findCustomer(Quote foundQuote, String companyCode) throws SAPIntegrationException {
        return null;
    }
}
