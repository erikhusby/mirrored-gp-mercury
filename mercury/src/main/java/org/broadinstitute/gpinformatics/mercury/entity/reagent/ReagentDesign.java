package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.hibernate.envers.Audited;

/**
 * A ReagentDesign is an abstraction or categorization of DesignedReagent, which is an instance of Reagent.
 * ReagentDesign is used when the reagent is a bait or CAT. CAT stands for Custom Amplicon Tube.
 * The design name is associated with a target list (or "manifest") registered in the pipeline.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = {@UniqueConstraint(columnNames = {"reagent_design"})})
public class ReagentDesign implements BusinessObject {
    @Id
    @SequenceGenerator(name = "SEQ_REAGENT_DESIGN", schema = "mercury", sequenceName = "SEQ_REAGENT_DESIGN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT_DESIGN")
    private Long reagentDesignId;

    @Column(name = "reagent_design", nullable = false)
    private String designName;

    private String targetSetName;

    private String manufacturersName;

    @OneToMany(mappedBy = "reagentDesign")
    private Set<DesignedReagent> designedReagents = new HashSet<>();

    /**
     * For JPA
     */
    public ReagentDesign() {
    }

    public static enum ReagentType {
        BAIT, CAT
    }

    @Enumerated(EnumType.STRING)
    private ReagentType reagentType;

    /**
     * @param designName  Example: cancer_2000gene_shift170_undercovered
     * @param reagentType The reagent type
     */
    public ReagentDesign(@Nonnull String designName, @Nonnull ReagentType reagentType) {
        this.designName = designName;
        this.reagentType = reagentType;
    }

    public Long getReagentDesignId() {
        return reagentDesignId;
    }

    public ReagentType getReagentType() {
        return reagentType;
    }

    public void setReagentType(ReagentType reagentType) {
        this.reagentType = reagentType;
    }

    public String getDesignName() {
        return designName;
    }

    @Override
    public String getName() {
        return designName;
    }

    @Override
    public String getBusinessKey() {
        return designName;
    }

    public void setDesignName(String designName) {
        this.designName = designName;
    }

    public String getTargetSetName() {
        return targetSetName;
    }

    /**
     * @param targetSetName Example: Cancer_2K
     */
    public void setTargetSetName(String targetSetName) {
        this.targetSetName = targetSetName;
    }

    public String getManufacturersName() {
        return manufacturersName;
    }

    public void setManufacturersName(String manufacturersName) {
        this.manufacturersName = manufacturersName;
    }

    public Set<DesignedReagent> getDesignedReagents() {
        return designedReagents;
    }

    public void addDesignedReagent(DesignedReagent designedReagent) {
        this.designedReagents.add(designedReagent);
    }

}
