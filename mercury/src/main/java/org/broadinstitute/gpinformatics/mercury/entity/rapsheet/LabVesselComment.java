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

package org.broadinstitute.gpinformatics.mercury.entity.rapsheet;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury", name = "lv_comment")
public class LabVesselComment<T extends RapSheetEntry> {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LV_COMMENT", schema = "mercury", sequenceName = "SEQ_LV_COMMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LV_COMMENT")
    private Long labVesselCommentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    protected LabEvent labEvent;

    @Column(name = "LAB_VESSEL_COMMENT")
    private String comment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH,optional = false)
    private LabVessel labVessel;

    @NotNull
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReworkEntry> reworkEntries;

    @NotNull
    @Temporal(TemporalType.DATE)
    private Date logDate;

    public LabVesselComment() {
    }

    public LabVesselComment(LabEvent labEvent, LabVessel labVessel, String comment) {
        this.labEvent = labEvent;
        this.comment = comment;
        this.labVessel = labVessel;
    }

    @PrePersist
    private void prePersist() {
        logDate = new Date();
    }

    public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

    public Date getLogDate() {
        return logDate;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    public void setLabEvent(LabEvent labEvent) {
        this.labEvent = labEvent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public List<ReworkEntry> getReworkEntries() {
        return reworkEntries;
    }

    public void setReworkEntries(List<ReworkEntry> reworkEntries) {
        this.reworkEntries = reworkEntries;
    }

    /**
     * This class is here primarily so RapSheet can return the most recent RapSheetEntry/ReworkEntry.
     * Normally we implement comparators as static methods, but Hibernates @Sort expects a class.
     */
    public static final class byLogDate implements Comparator<LabVesselComment> {
        @Override
        public int compare(LabVesselComment first, LabVesselComment second) {
            int result;
            result = first.getLogDate().compareTo(second.getLogDate());
            return result;
        }
    };
}
