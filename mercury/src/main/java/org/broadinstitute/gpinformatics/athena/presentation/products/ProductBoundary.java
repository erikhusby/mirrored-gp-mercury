package org.broadinstitute.gpinformatics.athena.presentation.products;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * This is a hack to retrofit an existing JSF page to begin transactions for some methods. Rather than moving the
 * method body from the JSF form along with all of its dependencies, this EJB simply establishes the connection and
 * calls back to the JFS form. It's cheap, but it works for now.
 *
 * @author breilly
 */
@Stateful
@RequestScoped
public class ProductBoundary {

    @Inject
    private ProductForm productForm;

    public String create() {
        return productForm.create();
    }

    public String edit() {
        return productForm.edit();
    }
}
