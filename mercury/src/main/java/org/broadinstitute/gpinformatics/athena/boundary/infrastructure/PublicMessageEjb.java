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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.admin.PublicMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;

import javax.annotation.Nonnull;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

/**
 * Manipulate the public message displayed at the top of mercury pages.
 * A PublicMessage is normally stored in the database, but allow the message object be set even when the database is unreachable.
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class PublicMessageEjb {
    private static final Log log = LogFactory.getLog(PublicMessageEjb.class);

    private PublicMessageDao publicMessageDao;

    public PublicMessageEjb() {
    }

    @Inject
    public PublicMessageEjb(PublicMessageDao publicMessageDao) {
        this.publicMessageDao = publicMessageDao;
    }

    private PublicMessage message;

    public PublicMessage getPublicMessage() {
        try {
            message = publicMessageDao.getMessage();
        } catch (Exception e) {
            log.error("Could not fetch public message from the database", e);
        }
        return message;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setPublicMessage(@Nonnull PublicMessage publicMessage) {
        clearPublicMessage();
        message = publicMessage;
        try {
            publicMessageDao.persist(message);
        } catch (Exception e) {
            log.error("Could not persist public message", e);
        }

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setPublicMessage(@Nonnull String publicMessageText) {
        setPublicMessage(new PublicMessage(publicMessageText));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clearPublicMessage() {
        message = null;
        try {
            publicMessageDao.clearMessage();
        } catch (Exception e) {
            log.error("Could not clear public message", e);
        }

    }
}
