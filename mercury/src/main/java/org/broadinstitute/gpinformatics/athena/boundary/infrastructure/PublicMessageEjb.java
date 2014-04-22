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

import javax.annotation.Nonnull;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class PublicMessageEjb {
    @Inject
    private PublicMessageDao publicMessageDao;

    public PublicMessage getPublicMessage() {
        return publicMessageDao.getMessage();
    }

    public void setPublicMessage(@Nonnull PublicMessage publicMessage) {
        clearPublicMessage();
        publicMessageDao.persist(publicMessage);
    }

    public void setPublicMessage(@Nonnull String publicMessageText) {
        clearPublicMessage();
        PublicMessage publicMessage = new PublicMessage(publicMessageText);
        publicMessageDao.persist(publicMessage);
    }


    public void clearPublicMessage() {
        PublicMessage publicMessage = getPublicMessage();
        if (publicMessage != null) {
            publicMessageDao.remove(publicMessage);
        }
    }
}
