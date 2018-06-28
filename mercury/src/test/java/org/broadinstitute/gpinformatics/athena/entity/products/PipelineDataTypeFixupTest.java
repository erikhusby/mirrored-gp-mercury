/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PipelineDataTypeDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation.DATA_TYPE_10X_WGS;
import static org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation.DATA_TYPE_EXOME;
import static org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation.DATA_TYPE_RNA;
import static org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation.DATA_TYPE_WGS;

@Test(groups = TestGroups.FIXUP)
public class PipelineDataTypeFixupTest extends Arquillian {
    @Inject
    private PipelineDataTypeDao pipelineDataTypeDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    /*
     * When applying this to Production, change the input to PROD, "prod"
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    private static boolean isNullPipelineDataType(Product product) {
        return product.getPipelineDataType() == null;
    }

    @Test(enabled = false)
    public void gplim5221initialDataLoad() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> validDataTypes = Arrays.asList(DATA_TYPE_10X_WGS, DATA_TYPE_EXOME, DATA_TYPE_RNA, DATA_TYPE_WGS);

        validDataTypes.stream()
            .map(dataType -> new PipelineDataType(dataType, true))
            .forEach(pipelineDataTypeDao::persist);

        List<Product> allNotInList =
            productDao.findAllNotInList(Product.class, Product_.aggregationDataType, validDataTypes);

        allNotInList.stream()
            .map(Product::getAggregationDataType).distinct()
            .map(dataType -> new PipelineDataType(dataType, false))
            .forEach(pipelineDataTypeDao::persist);

        pipelineDataTypeDao.persist(new FixupCommentary("GPLIM-5221 set initial values after adding new columns."));
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim5221backfillProducts() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        Map<String, PipelineDataType> dataTypeMap = pipelineDataTypeDao.findAll(PipelineDataType.class).stream()
            .collect(Collectors.toMap(PipelineDataType::getName, pipelineDataType -> pipelineDataType));

        //noinspection unchecked
        List<Product> productsWithDataType =
            productDao.findListWithWildcard(Product.class, "%", false, Product_.aggregationDataType);

        productsWithDataType.stream().filter(PipelineDataTypeFixupTest::isNullPipelineDataType).forEach(product -> {

            String aggregationDataType = product.getAggregationDataType();
            PipelineDataType pipelineDataType = dataTypeMap.get(aggregationDataType);
            if (pipelineDataType != null) {
                product.setPipelineDataType(pipelineDataType);
            }
        });
        productDao.persist(new FixupCommentary("GPLIM-5221 back fill pipeline data type."));
        utx.commit();
    }
}
