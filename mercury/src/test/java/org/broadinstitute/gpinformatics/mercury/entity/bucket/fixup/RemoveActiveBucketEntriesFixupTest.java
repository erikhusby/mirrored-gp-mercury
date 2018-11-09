package org.broadinstitute.gpinformatics.mercury.entity.bucket.fixup;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * @author breilly
 */
@Test(groups = TestGroups.FIXUP)
public class RemoveActiveBucketEntriesFixupTest extends Arquillian {

    @Inject
    private RemoveActiveBucketEntriesFixupEjb ejb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test(enabled = false)
    public void removeActiveBucketEntries() {
        ejb.removeActiveBucketEntries();
    }

}
