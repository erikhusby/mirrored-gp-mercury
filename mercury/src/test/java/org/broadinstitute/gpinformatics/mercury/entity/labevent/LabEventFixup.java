/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.bsp.client.users.BspUser;

import java.util.Date;

/**
 * This class is used for updating LabEvent data in such a way as to avoid creating public setters.
 * This class should only be called by fixup tests and never by production code.
 */
public class LabEventFixup {
    public static void fixupLabEvent(LabEvent labEvent, BspUser correctReceiptUser, Date createdDate,
                                     String eventLocation) {
        labEvent.setEventOperator(correctReceiptUser.getUserId());
        labEvent.setEventLocation(eventLocation);
        labEvent.setEventDate(createdDate);
    }
}
