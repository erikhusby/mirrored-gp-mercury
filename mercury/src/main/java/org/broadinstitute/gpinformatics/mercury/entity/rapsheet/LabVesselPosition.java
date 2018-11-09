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

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rapsheets and Rework log at the sample level. To know where the
 * "thing" we are logging occurred, we keep track of VesselPosition as well.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "lv_pos")
public class LabVesselPosition {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LV_POS", schema = "mercury", sequenceName = "SEQ_LV_POS")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LV_POS")
    private Long labVesselPositionId;

    // this should not cause n+1 select performance issue if it is LAZY and mandatory
    @OneToOne(mappedBy = "labVesselPosition", optional = false, fetch = FetchType.LAZY)
    private RapSheetEntry rapSheetEntry;

    @Column(nullable = false, name = "MERCURY_SAMPLES")
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable( schema = "MERCURY", name = "LV_POS_MERCURY_SAMPLES"
            , joinColumns = {@JoinColumn(name = "LV_POS")}
            ,inverseJoinColumns = {@JoinColumn(name = "MERCURY_SAMPLES")})
    private List<MercurySample> mercurySamples = new ArrayList<>();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    VesselPosition vesselPosition;

    public LabVesselPosition() {
    }

    public List<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public LabVesselPosition(VesselPosition vesselPosition, MercurySample... mercurySamples) {
        this.vesselPosition = vesselPosition;
        Collections.addAll(getMercurySamples(), mercurySamples);
    }

    public void setMercurySamples(List<MercurySample> mercurySamples) {
        this.mercurySamples.clear();
        this.mercurySamples.addAll(mercurySamples);
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }

    public void setVesselPosition(VesselPosition vesselPosition) {
        this.vesselPosition = vesselPosition;
    }

    public RapSheetEntry getRapSheetEntry() {
        return rapSheetEntry;
    }

    public void setRapSheetEntry(RapSheetEntry rapSheetEntry) {
        this.rapSheetEntry = rapSheetEntry;
    }
}
