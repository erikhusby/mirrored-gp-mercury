package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

@Test(groups = {TestGroups.STANDARD})
public class AddProductFamilyTest extends Arquillian {

    private static final Log log = LogFactory.getLog(AddProductFamilyTest.class);

    @Inject
    ProductFamilyDao dao;

    @Inject
    ProductDao productDao;

    @Inject ProductFamilyDao familyDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV,"dev");
    }

    @Test(enabled = false)
    public void createFamilies() {
        ProductFamily wgs = new ProductFamily("Whole Genome Sequencing");
        ProductFamily wga = new ProductFamily("Whole Genome Genotyping");

        System.out.println("Did it");

        dao.persist(wgs);
        dao.persist(wga);
    }

    /**
     * Parses a tab delimited file of product part number
     * and family name and changes the product's family.
     */
    @Test(enabled = false)
    public void changeProductFamilies() throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(new File("/tmp/families.txt")));

        String line = null;
        line = fileReader.readLine(); // skip the header row
        while ((line = fileReader.readLine()) != null) {
            String[] values = line.split("\\t");
            String partNumber = values[0].trim();
            String newFamily = values[1].trim();

            resetProductFamily(partNumber,newFamily);
        }
    }

    /**
     * Sets the family for the given product
     * @param productPartNumber the part number for the product
     * @param family
     */
    private void resetProductFamily(String productPartNumber, String family) {
        Product product = productDao.findByPartNumber(productPartNumber.trim());
        ProductFamily productFamily = familyDao.find(family.trim());
        product.setProductFamily(productFamily);
        productDao.persist(product);
        log.info(productPartNumber + " " + product.getName() + " is now in family " + product.getProductFamily()
                .getName());
    }
}
