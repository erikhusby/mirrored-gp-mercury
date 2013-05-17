package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * A representation of the aligner that will be used for analysis.
 */
@Entity
@Audited
@Table(name = "ANALYSIS_TYPE", schema = "mercury")
public class AnalysisType {

    @Id
    @SequenceGenerator(name = "SEQ_ANALYSIS_TYPE_TYPE", schema = "mercury", sequenceName = "SEQ_ANALYSIS_TYPE_TYPE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANALYSIS_TYPE_TYPE")
    private Long analysisTypeId;

    @Column(name = "ANALYSIS_TYPE_TYPE_NAME")
    private final String name;

    public AnalysisType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getBusinessKey() {
        return name;
    }
}
