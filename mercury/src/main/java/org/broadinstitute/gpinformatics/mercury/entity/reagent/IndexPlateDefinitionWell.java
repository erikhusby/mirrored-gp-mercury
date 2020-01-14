package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Defines the reagent and well of an index plate definition.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "index_plate_def_well")
public class IndexPlateDefinitionWell {
    @Id
    @SequenceGenerator(name="seq_index_plate_def_well", schema = "mercury", sequenceName="seq_index_plate_def_well")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_index_plate_def_well")
    @Column(name = "definition_well_id")
    private Long definitionWellId;

    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name="index_plate_definition", referencedColumnName = "definition_id")
    private IndexPlateDefinition indexPlateDefinition;

    @ManyToOne
    @JoinColumn(name="molecular_indexing_scheme", referencedColumnName = "molecular_indexing_scheme_id")
    private MolecularIndexingScheme molecularIndexingScheme;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;

    public IndexPlateDefinitionWell() {
    }

    public IndexPlateDefinitionWell(IndexPlateDefinition indexPlateDefinition,
            MolecularIndexingScheme molecularIndexingScheme, VesselPosition vesselPosition) {
        this.indexPlateDefinition = indexPlateDefinition;
        this.molecularIndexingScheme = molecularIndexingScheme;
        this.vesselPosition = vesselPosition;
    }

    public Long getDefinitionWellId() {
        return definitionWellId;
    }

    public IndexPlateDefinition getIndexPlateDefinition() {
        return indexPlateDefinition;
    }

    public void setIndexPlateDefinition(IndexPlateDefinition indexPlateDefinition) {
        this.indexPlateDefinition = indexPlateDefinition;
    }

    @NotNull
    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }

    public void setMolecularIndexingScheme(
            @NotNull MolecularIndexingScheme molecularIndexingScheme) {
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }

    public void setVesselPosition(VesselPosition vesselPosition) {
        this.vesselPosition = vesselPosition;
    }
}