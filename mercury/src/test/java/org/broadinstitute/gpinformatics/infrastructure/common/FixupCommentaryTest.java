/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditedRevDto;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


@Test(groups = TestGroups.STANDARD)
public class FixupCommentaryTest extends Arquillian {

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD)
    public void fixupCommentaryTest () throws InterruptedException {
        // Sets the user associated with the fixup.
        userBean.loginOSUser();

        // Timeboxes the changes for efficient AuditReader search.
        long timeboxStart = System.currentTimeMillis() / 1000;

        // Adds a metadata as a "fixup".
        LabEvent labEvent = labEventDao.findById(LabEvent.class, 477915L);
        Assert.assertNotNull(labEvent);
        Assert.assertEquals(labEvent.getLabEventMetadatas().size(), 0);
        labEvent.addMetadata(new LabEventMetadata(LabEventMetadata.LabEventMetadataType.DilutionFactor,
                String.valueOf(timeboxStart)));
        labEventDao.persist(new FixupCommentary("fixupCommentary unit test"));
        labEventDao.flush();

        // Makes the timebox wide since time is truncated to the second.
        long timeboxEnd = System.currentTimeMillis() / 1000 + 2;

        // Removes the metadata.  It's ok if this change is in the timebox.
        labEvent.getLabEventMetadatas().clear();
        labEventDao.flush();

        // Looks for a rev in the timebox that has both LabEvent and FixupCommentary.
        List<AuditedRevDto> auditedRevDtos = auditReaderDao.fetchAuditIds(timeboxStart, timeboxEnd,
                userBean.getLoginUserName(), null);
        Assert.assertTrue(auditedRevDtos.size() > 0, "Could not find modifications by " + userBean.getLoginUserName() +
                                                     " in the range " + timeboxStart + " to " + timeboxEnd);
        for (AuditedRevDto auditedRevDto : auditedRevDtos) {
            boolean foundLabEvent = false;
            boolean foundFixupCommentary = false;
            for (String classname : auditedRevDto.getEntityTypeNames()) {
                foundLabEvent |= classname.equals(LabEvent.class.getCanonicalName());
                foundFixupCommentary |= classname.equals(FixupCommentary.class.getCanonicalName());
            }
            if (foundLabEvent && foundFixupCommentary) {
                return;
            }
        }
        Assert.fail("Could not find a rev with both LabEvent and FixupCommentary.");
    }
}
