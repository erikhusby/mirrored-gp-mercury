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

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

@Entity
@Audited
public class ReworkEntry extends RapSheetEntry {
    @Enumerated(EnumType.STRING)
    @NotNull
    private ReworkReason reworkReason;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ReworkLevel reworkLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LabEventType reworkStep;

    /**
     * Active rework is work that is in a bucket, pdo, lcset, or whatever.
     * IE, It is Actively being worked on, and is not available to put into
     * a bucket.
     *
     * This is a Big B Boolean, because hibernate was complaining about not being able
     * so assign null to a primitive.
     */
    private Boolean activeRework=false;

    public ReworkEntry() {

    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> labVesselComment,
                       ReworkReason reason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        super(labVesselPosition, labVesselComment);
        this.reworkReason = reason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> rapSheetComment) {
        super(labVesselPosition, rapSheetComment);
    }

    public ReworkReason getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(ReworkReason reason) {
        this.reworkReason = reason;
    }

    public ReworkLevel getReworkLevel() {
        return reworkLevel;
    }

    public void setReworkLevel(ReworkLevel reworkLevel) {
        this.reworkLevel = reworkLevel;
    }

    public LabEventType getReworkStep() {
        return reworkStep;
    }

    public void setReworkStep(LabEventType reworkStep) {
        this.reworkStep = reworkStep;
    }

    public Boolean isActiveRework() {
        return activeRework;
    }

    public void setActiveRework(Boolean activeRework) {
        this.activeRework = activeRework;
    }
}
