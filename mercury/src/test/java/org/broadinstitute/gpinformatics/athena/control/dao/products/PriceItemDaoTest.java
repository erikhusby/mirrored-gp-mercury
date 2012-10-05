package org.broadinstitute.gpinformatics.athena.control.dao.products;


import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Category.EXOME_SEQUENCING_ANALYSIS;
import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Name.DNA_EXTRACTION;
import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Name.EXOME_EXPRESS;
import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Platform.GP;


@Test(enabled = false)
public class PriceItemDaoTest extends ContainerTest {

    @Inject
    private PriceItemDao dao;

    @Inject
    private Log log;


    public void testFindAll() {

        final List<PriceItem> priceItems = dao.findAll();
        Assert.assertNotNull(priceItems);

        for (PriceItem priceItem : priceItems) {

            log.warn("Price Item name: " + priceItem.getName());
            if ( EXOME_EXPRESS.getQuoteServerName().equals(priceItem.getName()) ) {
                return;
            }
        }

        Assert.fail("EXEX price item not found!");

    }


    public void testFind() {

        final PriceItem priceItem = dao.find(GP, EXOME_SEQUENCING_ANALYSIS, EXOME_EXPRESS);
        Assert.assertNotNull(priceItem);


        try {
            // deliberately mismatching the name to the other parameters
            dao.find(GP, EXOME_SEQUENCING_ANALYSIS, DNA_EXTRACTION);
        }
        catch (EJBException ejbx) {
            Assert.assertTrue(NoResultException.class.isAssignableFrom(ejbx.getCause().getClass()));
        }

    }

}
