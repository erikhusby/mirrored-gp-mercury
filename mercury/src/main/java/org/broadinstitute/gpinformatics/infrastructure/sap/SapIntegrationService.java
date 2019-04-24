package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
     * @param closingOrder
     * @return Unique order identifier of the sales/release order currently in SAP
     * @throws SAPIntegrationException
     */
    void updateOrder(ProductOrder placedOrder, boolean closingOrder) throws SAPIntegrationException;

    /**
     * This method will allow mercury to record completed work in SAP in order to complete the Billing process
     * @param item A structure previously utilized by logging information to the quote server which aggregates work
     *             by Quote, Product order, PDO and finally amount done.
     * @param quantityOverride
     * @param workCompleteDate
     * @return A unique identifier associated with the recorded record of work in SAP
     * @throws SAPIntegrationException
     */
    String billOrder(QuoteImportItem item, BigDecimal quantityOverride, Date workCompleteDate) throws SAPIntegrationException;

    /**
     * With the introduction of a direct communication to SAP from Mercury, we will do away with Price Items for a
     * representation of the price of work.  Products are now directly reflected in the System which manages our
     * financial records (SAP).  This method will allow Mercury to create a representation of a Product within SAP
     * for the purpose of tracking projected and actual work
     * 
     * For an existing product, this method will also allow Mercury to update that product with any changes made within
     * Mercury
     * @param product The Product information to be reflected in SAP
     * @throws SAPIntegrationException
     */
    void publishProductInSAP(Product product) throws SAPIntegrationException;
    
    Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException;

    OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, SapQuote sapQuote, ProductOrder productOrder) throws SAPIntegrationException;

    /**
     * Placeholder method for now.  Future inplementation will return a quote object geared toward the information
     * returned from SAP.
     * @param sapQuoteId  Singular quote identifier for the desired SAP quote information
     * @return
     * @throws SAPIntegrationException
     */
    SapQuote findSapQuote(String sapQuoteId) throws SAPIntegrationException;

    /**
     * This method assists in the need to occasionally "Unbill" samples on an order.  It will create and send the
     * conceptual return order to SAP to credit the work that was previously billed on the given delivery document.
     * @param deliveryDocumentId    Identifier of the delivery document created when the work intended to be reversed
     *                              was originally billed
     * @param quoteItemForBilling   contains all the information for the work that will be reverted
     * @return identifier that is associated with the SAP return order created to process this credit request
     */
    String creditDelivery(String deliveryDocumentId, QuoteImportItem quoteItemForBilling)
            throws SAPIntegrationException;

    class Option {
        public static final Option NONE = Option.create();

        private Set<Type> options = new HashSet<>();

        private Option(Type... options) {
            this.options = new HashSet<>(Arrays.asList(options));
        }

        public static Option create(Type... orderOptions) {
            return new Option(orderOptions);

        }

        public static Type isClosing(boolean booleanFlag) {
            if (booleanFlag) {
                return Type.CLOSING;
            }
            return null;
        }

        public static Type isCreating(boolean booleanFlag) {
            if (booleanFlag) {
                return Type.CREATING;
            }
            return null;
        }

        public static Type isForValueQuery(boolean booleanFlag) {
            if (booleanFlag) {
                return Type.ORDER_VALUE_QUERY;
            }
            return null;
        }

        public boolean hasOption(Type sapOrderOption) {
            return options.stream().anyMatch(option -> option == sapOrderOption);
        }

        public enum Type {
            CREATING,
            CLOSING,
            ORDER_VALUE_QUERY
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || (!OrmUtil.proxySafeIsInstance(o, Option.class))) {
                return false;
            }

            if (!(o instanceof Option)) {
                return false;
            }

            Option option = OrmUtil.proxySafeCast(o, Option.class);

            return new EqualsBuilder()
                .append(options, option.options)
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                .append(options)
                .toHashCode();
        }
    }
}
