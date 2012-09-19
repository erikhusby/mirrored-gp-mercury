package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

/**
 * A grouping of samples within the context of a particular product.
 * The class also contains one or more RiskContingency
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:37 AM
 */
@XmlType(namespace = Namespaces.ORDER_NS, propOrder = {
        "product",
        "quoteId",
        "samples",
        "risks"
})
public class OrderItem implements Serializable {

    private Product product;
    private String quoteId;
    private List<BSPSample> samples;
    private List<org.broadinstitute.gpinformatics.athena.entity.orders.RiskContingency> risks;  // lists of risks associated with this orderItem

    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(final String quoteId) {
        this.quoteId = quoteId;
    }

    public List<BSPSample> getSamples() {
        return samples;
    }

    public void setSamples(final List<BSPSample> samples) {
        this.samples = samples;
    }

    public List<RiskContingency> getRisks() {
        return risks;
    }

    public void setRisks(final List<RiskContingency> risks) {
        this.risks = risks;
    }
}
