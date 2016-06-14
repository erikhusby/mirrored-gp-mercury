package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.sapservices.SapIntegrationClientImpl;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@Impl
public class SapIntegrationServiceImpl implements SapIntegrationService {

    @Inject
    private SapConfig sapConfig;

    @Inject
    private QuoteService quoteService;

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImpl.class);

    public SapIntegrationServiceImpl() {
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        initializeClient(sapConfigIn);
    }

    private void initializeClient(SapConfig sapConfigIn) {
        wrappedClient = new SapIntegrationClientImpl(sapConfigIn.getLogin(), sapConfigIn.getPassword(),
                sapConfigIn.getBaseUrl(), sapConfigIn.getWsdlUri(), "/wsdl/sap/dev/sap_test_service.wsdl");
    }

    @Override
    public String submitAge(String age) throws IOException {
        if(wrappedClient == null ) {
            initializeClient(this.sapConfig);
        }
        return wrappedClient.ageSubmission(age);
    }

    @Override
    public String createOrder(ProductOrder placedOrder) {

        return null;
    }

    @Override
    public String updateOrder(ProductOrder placedOrder) {

        return null;
    }

    @Override
    public String findCustomer(@NotNull String quoteId) {

        String customerEmailAddress;
        Quote foundQuote;

        try {
            foundQuote = quoteService.getQuoteByAlphaId(quoteId);
        } catch (QuoteServerException e) {
            e.printStackTrace();
        } catch (QuoteNotFoundException e) {
            e.printStackTrace();
        }


        return null;
    }

    public String billOrder(BillingSession sessionForBilling) {
        return null;
    }
}
