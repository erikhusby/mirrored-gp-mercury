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

package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.admin.PublicMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;


@Test(groups = TestGroups.DATABASE_FREE)
public class PublicMessageErrorHandlingTest {
    private static final String TEST_MESSAGE = "this is my message";

    private PublicMessageEjb publicMessageEjb;

    @BeforeTest
    public void setUpMessage() {
        PublicMessageDao publicMessageDao = mock(PublicMessageDao.class);
        doThrow(new RuntimeException("exception removing message")).when(publicMessageDao).remove(Mockito.any());
        doThrow(new RuntimeException("exception getting message")).when(publicMessageDao).getMessage();
        publicMessageEjb = new PublicMessageEjb(publicMessageDao);
    }

    public void testGetPublicMessage() {
        PublicMessage publicMessage = publicMessageEjb.getPublicMessage();
        Assert.assertNull(publicMessage);
    }

    private PublicMessage createTestPublicMessage(){
        publicMessageEjb.setPublicMessage(TEST_MESSAGE);
        return publicMessageEjb.getPublicMessage();
    }

    public void testSetPublicMessage() {
        PublicMessage publicMessage = createTestPublicMessage();
        Assert.assertNotNull(publicMessage);
        Assert.assertEquals(publicMessage.getMessage(), TEST_MESSAGE);
    }

    public void testClearPublicMessage() {
        PublicMessage publicMessage = createTestPublicMessage();
        Assert.assertNotNull(publicMessage);

        publicMessageEjb.clearPublicMessage();
        publicMessage = publicMessageEjb.getPublicMessage();
        Assert.assertNull(publicMessage);
    }
}
