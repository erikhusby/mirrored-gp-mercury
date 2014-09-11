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
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@Entity
@Audited
@Table(name = "REGULATORY_INFO", schema = "athena",
        uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "type"}))
public class RegulatoryInfo implements Serializable, BusinessObject {
    public static final int PROTOCOL_TITLE_MAX_LENGTH=255;
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
    @SequenceGenerator(name="seq_regulatory_info", schema = "athena", sequenceName="seq_regulatory_info")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_regulatory_info")
    private Long regulatoryInfoId;

    @Column(name="name", nullable = false)
    private String name;

    @Column(name="type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name="identifier", nullable = false)
    private String identifier;

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "regulatoryInfos")
    private Collection<ResearchProject> researchProjects = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "regulatoryInfos")
    private Collection<ProductOrder> productOrders = new HashSet<>();

    public RegulatoryInfo() {
    }

    public RegulatoryInfo(String name, Type type, String identifier) {
        this.name = name;
        this.type = type;
        this.identifier = identifier;
    }

    public String getDisplayText() {
        return getIdentifier() + " - " + getName();
    }

    @Override
    public String getBusinessKey() {
        return String.valueOf(getRegulatoryInfoId());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Long getRegulatoryInfoId() {
        return regulatoryInfoId;
    }

    public Collection<ResearchProject> getResearchProjects() {
        return researchProjects;
    }

    /**
     * Returns all ResearchProjects this RegulatoryInfo is associated with, including child ResearchProjects
     */
    public Collection<ResearchProject> getAvailableResearchProjects() {
        Collection<ResearchProject> allProjects = new ArrayList<>(getResearchProjects());
        for (ResearchProject project : getResearchProjects()) {
            allProjects.addAll(project.getAllChildren());
        }
        return allProjects;
    }

    public void addResearchProject(ResearchProject researchProject) {
        researchProjects.add(researchProject);
    }

    public void removeResearchProject(ResearchProject researchProject) {
        researchProjects.remove(researchProject);
    }

    public Collection<ProductOrder> getProductOrders() {
        return productOrders;
    }

    @Override
    public boolean equals(Object other) {
        if(this == other) {
            return true;
        }

        if (other == null || !OrmUtil.proxySafeIsInstance(other, RegulatoryInfo.class)) {
            return false;
        }

        RegulatoryInfo castOther = OrmUtil.proxySafeCast(other, RegulatoryInfo.class);

        return new EqualsBuilder().append(getIdentifier(), castOther.getIdentifier())
                .append(getType(), castOther.getType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getIdentifier()).append(getType()).toHashCode();
    }
}
