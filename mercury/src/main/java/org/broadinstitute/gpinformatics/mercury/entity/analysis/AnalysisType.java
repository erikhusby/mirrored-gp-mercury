package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This gives the pipeline the type of analysis that will be desired. This will be one item from a set list that
 * is determined (and managed) by the pipeline team.
 */
@Entity
@Audited
@Table(name = "ANALYSIS_TYPE", schema = "mercury")
public class AnalysisType implements BusinessObject {
    // NO_ANALYSIS puts a null value in the pipeline query.
    public static final String NO_ANALYSIS = "No_Analysis";

    @Id
    @SequenceGenerator(name = "SEQ_ANALYSIS_TYPE", schema = "mercury", sequenceName = "SEQ_ANALYSIS_TYPE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANALYSIS_TYPE")
    private Long analysisTypeId;

    @Column(name = "NAME", nullable = false)
    private String name;

    protected AnalysisType() {
    }

    public AnalysisType(@Nonnull String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessKey() {
        return name;
    }
}
