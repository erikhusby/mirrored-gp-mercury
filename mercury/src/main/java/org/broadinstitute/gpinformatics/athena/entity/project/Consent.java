/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Collection;

@Entity
@Audited

@Table(name = "CONSENT", schema = "athena",
        uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "type"}))
public class Consent implements Serializable, BusinessObject {
    public enum Type {
        IRB("IRB Protocol"),
        ORSP_NOT_ENGAGED("ORSP Not Engaged"),
        ORSP_NOT_HUMAN_SUBJECTS_RESEARCH("ORSP Not Human Subjects Research");

        private String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    @Id
    @SequenceGenerator(name="seq_consent", schema = "athena", sequenceName="seq_consent")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_consent")
    private Long consentId;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name="identifier", nullable = false)
    private String identifier;

    public Consent() {
    }

    public Consent(String name, Type type, String identifier) {
        this.name = name;
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public String getBusinessKey() {
        return identifier;
    }

    @Override
    public String getName() {
        return name;
    }


    public Type getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Long getConsentId() {
        return consentId;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, Consent.class)) {
            return false;
        }

        Consent castOther = OrmUtil.proxySafeCast(other, Consent.class);

        return new EqualsBuilder().append(getIdentifier(), castOther.getIdentifier())
                .append(getType(), castOther.getType()).append(getName(), castOther.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getIdentifier()).append(getType()).append(getName()).toHashCode();
    }
}
