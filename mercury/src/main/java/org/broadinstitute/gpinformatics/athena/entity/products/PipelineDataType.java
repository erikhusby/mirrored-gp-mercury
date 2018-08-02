/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Audited
@Table(schema = "athena", uniqueConstraints = @UniqueConstraint(columnNames = "NAME"))
public class PipelineDataType  {
    @Id
    @SequenceGenerator(name = "SEQ_PIPELINE_DATA_TYPE", schema = "athena", sequenceName = "SEQ_PIPELINE_DATA_TYPE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PIPELINE_DATA_TYPE")
    private Long pipelineDataTypeId;

    private String name;

    private boolean active;

    public PipelineDataType() {
    }

    public PipelineDataType(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, PipelineDataType.class))) {
            return false;
        }

        if (!(o instanceof PipelineDataType)) {
            return false;
        }

        PipelineDataType that = OrmUtil.proxySafeCast(o, PipelineDataType.class);

        return new EqualsBuilder()
            .append(isActive(), that.isActive())
            .append(getName(), that.getName())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getName())
            .append(isActive())
            .toHashCode();
    }
}
