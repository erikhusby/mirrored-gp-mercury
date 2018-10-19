package org.broadinstitute.gpinformatics.mercury.entity.analysis.fixup;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.CoverageTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.CoverageType;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to Coverage entities
 */
@Test(groups = TestGroups.FIXUP)
public class CoverageFixupTest extends Arquillian {

    @Inject
    private CoverageTypeDao coverageTypeDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private UserBean userBean;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    @Test(enabled = true)
    public void fixupGplim5831AddCoverageTypes() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Map<String, String> mapProductToCoverage = new HashMap<String, String>() {
            {
                put("P-CLA-0005", "HS 100X Mean Target Coverage");
                put("P-CLA-0003", "HS 100X Mean Target Coverage");
                put("P-CLA-0006", "HS 150X Mean Target Coverage");
                put("P-CLA-0004", "HS 150X Mean Target Coverage");
                put("P-EX-0044", "HS 150X Mean Target Coverage");
                put("P-EX-0045", "HS 150X Mean Target Coverage");
                put("P-EX-0040", "HS 80% Target Bases @ 20X");
                put("P-EX-0042", "HS 80% Target Bases @ 20X");
                put("P-EX-0049", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010268", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010269", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010270", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010274", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010275", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010276", "HS 80% Target Bases @ 20X");
                put("XTNL-WES-010290", "HS 80% Target Bases @ 20X");
                put("P-EX-0018", "HS 85% Target Bases @ 20X");
                put("P-EX-0038", "HS 85% Target Bases @ 20X");
                put("XTNL-WES-010228", "HS 85% Target Bases @ 20X");
                put("XTNL-WES-010229", "HS 85% Target Bases @ 20X");
                put("XTNL-WES-010230", "HS 85% Target Bases @ 20X");
                put("P-EX-0048", "HS 85% Target Bases @ 50X");
                put("P-EX-0050", "HS 85% Target Bases @ 50X");
                put("P-EX-0039", "HS 85% Target Bases @ 50X");
                put("P-EX-0041", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010271", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010272", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010273", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010277", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010278", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010279", "HS 85% Target Bases @ 50X");
                put("XTNL-WES-010289", "HS 85% Target Bases @ 50X");
                put("XTNL-RNA-010407", "100M Reads Aligned in Pairs");
                put("XTNL-RNA-010408", "100M Reads Aligned in Pairs");
                put("XTNL-RNA-010409", "100M Reads Aligned in Pairs");
                put("P-RNA-0022", "100M Reads Aligned in Pairs");
                put("P-RNA-0006", "50M Reads Aligned in Pairs");
                put("P-RNA-0017", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010400.2", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010401.2", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010402.2", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010406", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010411", "50M Reads Aligned in Pairs");
                put("XTNL-RNA-010412", "50M Reads Aligned in Pairs");
                put("P-RNA-0016", "50M Reads Aligned in Pairs");
                put("P-RNA-0020", "50M Reads Aligned in Pairs");
                put("P-RNA-0019", "50M Reads Aligned in Pairs");
                put("P-RNA-0024", "75M Reads Aligned in Pairs");
                put("P-WG-0090", "WGS Raw 0.1X Mean Coverage");
                put("P-WG-0096", "WGS Raw 0.1X Mean Coverage");
                put("XTNL-WGS-010346", "WGS Raw 0.1X Mean Coverage");
                put("P-WG-0082", "WGS Raw 15X Mean Coverage");
                put("P-WG-0083", "WGS Raw 15X Mean Coverage");
                put("XTNL-WGS-010347", "WGS Raw 15X Mean Coverage");
                put("P-WG-0071", "WGS Raw 20X Mean Coverage");
                put("P-WG-0086", "WGS Raw 20X Mean Coverage");
                put("XTNL-WGS-010343", "WGS Raw 20X Mean Coverage");
                put("XTNL-WGS-010344", "WGS Raw 20X Mean Coverage");
                put("P-WG-0080", "WGS Raw 30X Mean Coverage");
                put("P-WG-0069", "WGS Raw 30X Mean Coverage");
                put("P-WG-0087", "WGS Raw 30X Mean Coverage");
                put("XTNL-WGS-010325", "WGS Raw 30X Mean Coverage");
                put("XTNL-WGS-010339", "WGS Raw 30X Mean Coverage");
                put("XTNL-WGS-010341", "WGS Raw 30X Mean Coverage");
                put("XTNL-WGS-010345", "WGS Raw 30X Mean Coverage");
                put("P-WG-0081", "WGS Raw 60X Mean Coverage");
                put("P-WG-0079", "WGS Raw 60X Mean Coverage");
                put("P-WG-0088", "WGS Raw 60X Mean Coverage");
                put("XTNL-WGS-010340", "WGS Raw 60X Mean Coverage");
                put("XTNL-WGS-010342", "WGS Raw 60X Mean Coverage");
                put("P-WG-0084", "WGS Raw 80X Mean Coverage");
                put("P-WG-0089", "WGS Raw 80X Mean Coverage");
                put("XTNL-WGS-010348", "WGS Raw 80X Mean Coverage");
                put("P-CLA-0008", "WGS Raw 95% @ 20X");
            }
        };

        // Note this list contains more than the Mappings above, some won't initially have a product.
        List<String> deliverables = Arrays.asList("50M Reads Aligned in Pairs", "75M Reads Aligned in Pairs",
                "100M Reads Aligned in Pairs", "HS 100X Mean Target Coverage",
                "HS 150X Mean Target Coverage", "HS 250X Mean Target Coverage",
                "HS 400X Mean Target Coverage", "HS 500X Mean Target Coverage",
                "HS 80% Target Bases @ 20X", "HS 85% Target Bases @ 50X", "HS 85% Target Bases @ 20X",
                "WGS Raw 0.1X Mean Coverage", "WGS Raw 15X Mean Coverage", "WGS Raw 20X Mean Coverage",
                "WGS Raw 30X Mean Coverage", "WGS Raw 60X Mean Coverage", "WGS Raw 80X Mean Coverage", "WGS Raw 95% @ 20X");
        Set<CoverageType> coverageTypes = new HashSet<>();
        for (String deliverable: deliverables) {
            CoverageType coverageType = new CoverageType(deliverable);
            coverageTypes.add(coverageType);
        }

        Set<Product> updatedProducts = new HashSet<>();
        for (Map.Entry<String, String> entry: mapProductToCoverage.entrySet()) {
            String partNumber = entry.getKey();
            String coverage = entry.getValue();
            Product product = productDao.findByPartNumber(partNumber);
            if (product == null) {
                throw new RuntimeException("Failed to find product for part number: " + partNumber);
            }
            if (!deliverables.contains(coverage)) {
                throw new RuntimeException("Unexpected deliverable found: " + coverage);
            }
            product.setCoverageTypeKey(mapProductToCoverage.get(partNumber));
            updatedProducts.add(product);
        }

        coverageTypeDao.persistAll(coverageTypes);
        productDao.persistAll(updatedProducts);
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/AddCoverageTypes.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-4205 add new coverage types for selection
     * 10M Aligned In Pairs
     * 15M Aligned In Pairs
     */
    @Test(enabled = false)
    public void fixupGplim4798() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("AddCoverageTypes.txt"));
        String fixupCommentary = lines.get(0);
        Set<CoverageType> coverageTypes = new HashSet<>();
        for (String deliverable : lines.subList(1, lines.size())) {
            if (coverageTypeDao.findByBusinessKey(deliverable) != null) {
                throw new RuntimeException("Coverage type already exists: " + deliverable);
            }
            CoverageType coverageType = new CoverageType(deliverable);
            coverageTypes.add(coverageType);
            System.out.println("Adding " + deliverable);
        }
        coverageTypeDao.persist(new FixupCommentary(fixupCommentary));
        coverageTypeDao.persistAll(coverageTypes);
        coverageTypeDao.flush();
        utx.commit();
    }

}
