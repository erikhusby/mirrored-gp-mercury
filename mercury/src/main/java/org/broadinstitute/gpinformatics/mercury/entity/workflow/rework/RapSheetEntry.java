/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow.rework;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Audited
@Table(schema = "mercury")
public class RapSheetEntry {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_SAMPLE_RAP_SHEET", schema = "mercury", sequenceName = "SEQ_SAMPLE_RAP_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMPLE_RAP_SHEET")
    private Long rapSheetEntryId;

    private String comment;

    @Temporal(TemporalType.DATE)
    private Date logDate;


    @PrePersist
    private void prePersist() {
        logDate = new Date();
    }

    @ManyToOne
    private RapSheet rapSheet;


    public RapSheetEntry() {
    }

    public RapSheetEntry(EntryType entryType, String comment, RapSheet rapSheet) {
        this.comment = comment;
        this.rapSheet = rapSheet;
        this.type = entryType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public RapSheet getRapSheet() {
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    @Enumerated
    private EntryType type;

    public static enum EntryType {
        MACHINE_ERROR("Machine Error");

        private String value;

        private EntryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
