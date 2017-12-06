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

import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * A ReworkEntry is marks that a sample needs to be reworked (activeRework = true) or that it has been reworked
 * (activeRework = false). Other information on the event are why the rework is being requested, and to what level
 * it is being reworked; see (@link(ReworkReasonEnum))
 */
@Entity
@Audited
public class ReworkEntry extends RapSheetEntry {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkReasonEnum reworkReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkLevel reworkLevel;

    @Column(nullable = false)
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
    @Column(nullable = false)
    private boolean activeRework = true;

    public ReworkEntry() {

    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> labVesselComment,
                       ReworkReasonEnum reason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        super(labVesselPosition, labVesselComment);
        this.reworkReason = reason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> rapSheetComment) {
        super(labVesselPosition, rapSheetComment);
    }

    public ReworkReasonEnum getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(ReworkReasonEnum reason) {
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

    public boolean isActiveRework() {
        return activeRework;
    }

    public void setActiveRework(boolean activeRework) {
        this.activeRework = activeRework;
    }

    /**
     * Why the rework is happening. This list needs to be added to.
     * TODO To be removed after the deployment of the v1.44 release
     */
    @Deprecated
    public static enum ReworkReasonEnum {
        MACHINE_ERROR("Machine Error"),
        UNKNOWN_ERROR("Unknown Error");

        private final String value;

        ReworkReasonEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
