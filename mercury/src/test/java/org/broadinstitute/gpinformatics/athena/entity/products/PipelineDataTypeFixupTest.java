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
import java.util.stream.Stream;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class PipelineDataTypeFixupTest extends Arquillian {
    @Inject
    private PipelineDataTypeDao pipelineDataTypeDao;

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

    @Test(enabled = false)
    public void gplim5221initialDataLoad() throws Exception {
        userBean.loginOSUser();
        utx.begin();
        Stream.of("10X_WGS", "Exome", "RNA", "WGS").forEach(dt -> pipelineDataTypeDao.persist(new PipelineDataType(dt)));
        pipelineDataTypeDao.persist(new FixupCommentary("GPLIM-5221 set initial values after adding new columns."));
        utx.commit();

    }
}
