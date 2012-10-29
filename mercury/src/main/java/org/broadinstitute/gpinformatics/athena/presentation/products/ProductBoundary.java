package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author breilly
 */
@Stateful
@RequestScoped
public class ProductBoundary extends AbstractJsfBean {

    @Inject
    private ProductForm productForm;

    public String create() {
        return productForm.create();
    }

    public String edit() {
        return productForm.edit();
    }
}
