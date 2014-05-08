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

package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This Class is used for storing the mercury public message which is displayed at the top of mercury web pages.
 */
@Entity
@Table(schema = "athena")
public class PublicMessage {
    private PublicMessage() {
    }

    public PublicMessage(String message) {
        this.message=message;
    }

    @Id
    @SequenceGenerator(name = "SEQ_PUBLIC_MESSAGE", schema = "athena", sequenceName = "SEQ_PUBLIC_MESSAGE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PUBLIC_MESSAGE")
    private Long publicMessageId;

    @Column
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
