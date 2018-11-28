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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.ArrayList;
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, optional = true)
    @JoinColumn(name = "LAB_EVENT")
    protected LabEvent labEvent;

    @Column(name = "LAB_VESSEL_COMMENT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH, optional = false)
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel labVessel;

    @Column(nullable = false)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = RapSheetEntry.class,
            mappedBy = "labVesselComment")
    private List<T> rapSheetEntries = new ArrayList<>();

    @Column(nullable = false)
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

    public void
    setLogDate(Date logDate) {
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

    public List<T> getRapSheetEntries() {
        return rapSheetEntries;
    }

    public void setRapSheetEntries(List<T> rapSheetEntries) {
        this.rapSheetEntries = rapSheetEntries;
    }
}
