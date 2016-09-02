package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

/**
 *
 * The Genomic platform is the first group in the Broad Institute to attempt to replace the Quote Server as a financial
 * management system and thus directly utilize SAP to manage quote and billing information.  To do this, Mercury needs
 * to be provided with a conduit to communicate to SAP.
 *
 * The SapIntegrationService provides Mercury with the ability to translate mercury entities and thus communicate those
 * entities with SAP.
 */
public interface SapIntegrationService {

    /**
     * For a given ProductOrder, this method will communicate to SAP to create a new sales/release order from the
     * information contained
     * @param placedOrder ProductOrder to be created in SAP
     * @return Unique order identifier of the sales/release order created in SAP
     * @throws SAPIntegrationException
     */
    String createOrder(ProductOrder placedOrder) throws SAPIntegrationException;

    /**
     * For a given ProductOrder that is already represented in SAP, this method will communicate to SAP any changes to
     * be made for that order
     * @param placedOrder ProductOrder, that is reflected in SAP, to be updated in SAP
     * @return Unique order identifier of the sales/release order currently in SAP
     * @throws SAPIntegrationException
     */
    void updateOrder(ProductOrder placedOrder) throws SAPIntegrationException;

    /**
     * For Phase 1 of the SAP/GP integration, Orders placed in SAP need to have reference to the customer number found
     * in SAP of the contact person on a purchase order.  This method will give Mercury the ability to search for that
     * number
     * @param foundQuote The Quote associated with the Product order attempting to be replicated in SAP.  This will
     *                   give the method:
     *                   <ul><li>Insight as to whether the Quote allows the order to be eligible to create in SAP</li>
     *                   <li>access to the funding types to determine if there is a purchase order.  If so we will
     *                   then have...</li>
     *                   <li>access to the email address of the customer associated with the purchase order (used
     *                   to find the SAP customer number</li>
     *                   </ul>
     * @param companyCode The code associated with the SAP company structure in which this customer should be found
     * @return If this quote is eligible and backed by a purchase order, the customer number found in SAP is returned
     * @throws SAPIntegrationException
     */
    String findCustomer(Quote foundQuote, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) throws SAPIntegrationException;

    /**
     * This method will allow mercury to record completed work in SAP in order to complete the Billing process
     * @param item A structure previously utilized by logging information to the quote server which aggregates work
     *             by Quote, Product order, PDO and finally amount done.
     * @return A unique identifier associated with the recorded record of work in SAP
     * @throws SAPIntegrationException
     */
    String billOrder(QuoteImportItem item) throws SAPIntegrationException;

    /**
     * With the introduction of a direct communication to SAP from Mercury, we will do away with Price Items for a
     * representation of the price of work.  Products are now directly reflected in the System which manages our
     * financial records (SAP).  This method will allow Mercury to create a representation of a Product within SAP
     * for the purpose of tracking projected and actual work
     * @param product The Product information to be reflected in SAP
     * @throws SAPIntegrationException
     */
    void createProductInSAP(Product product) throws SAPIntegrationException;

    /**
     * For an existing product, this method will allow Mercury to update that product with any changes made within
     * Mercury
     * @param product The Product information to be reflected in SAP
     * @throws SAPIntegrationException
     */
    void changeProductInSAP(Product product) throws SAPIntegrationException;
}
