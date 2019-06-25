package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract definition or template of index plate content.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class IndexPlateDefinition {
    public enum ReagentType {ADAPTER, PRIMER}

    @Id
    @SequenceGenerator(name="seq_index_plate_def", schema = "mercury", sequenceName="seq_index_plate_def")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_index_plate_def")
    @Column(name = "definition_id")
    private Long definitionId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true,
            mappedBy = "indexPlateDefinition")
    private Set<IndexPlateDefinitionWell> definitionWells = new HashSet<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinTable(name = "index_plate_instance",
            joinColumns = @JoinColumn(name = "definition_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_vessel"))
    @AuditJoinTable(name = "index_plate_instance_aud")
    private Set<StaticPlate> plateInstances = new HashSet<>();

    private String definitionName;

    @Enumerated(EnumType.STRING)
    private VesselGeometry vesselGeometry;

    @Enumerated(EnumType.STRING)
    private ReagentType reagentType;

    public IndexPlateDefinition() {
    }

    public IndexPlateDefinition(String definitionName, VesselGeometry vesselGeometry, ReagentType reagentType) {
        this.definitionName = definitionName;
        this.vesselGeometry = vesselGeometry;
        this.reagentType = reagentType;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public Set<IndexPlateDefinitionWell> getDefinitionWells() {
        return definitionWells;
    }

    public void setDefinitionWells(Set<IndexPlateDefinitionWell> definitionWells) {
        this.definitionWells = definitionWells;
    }

    public Set<StaticPlate> getPlateInstances() {
        return plateInstances;
    }

    public void setPlateInstances(Set<StaticPlate> plateInstances) {
        this.plateInstances = plateInstances;
    }

    public String getDefinitionName() {
        return definitionName;
    }

    public void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }

   public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) {
        this.vesselGeometry = vesselGeometry;
    }

    public ReagentType getReagentType() {
        return reagentType;
    }

    public void setReagentType(ReagentType reagentType) {
        this.reagentType = reagentType;
    }
}