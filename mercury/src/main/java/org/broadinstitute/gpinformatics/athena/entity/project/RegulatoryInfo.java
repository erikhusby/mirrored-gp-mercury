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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Representation of an Office of Research Subject Protection (ORSP) "Project". ORSP Projects can contain IRB approval
 * documents, consent groups, and other information about determinations made by the office.
 *
 * Originally, before an ORSP Portal existed, these were hand-entered records. They are now a mix of old hand-entered
 * records and copies of data from the ORSP Portal.
 */
@Entity
@Audited
@Table(name = "REGULATORY_INFO", schema = "athena",
        uniqueConstraints = @UniqueConstraint(columnNames = {"identifier", "type"}))
public class RegulatoryInfo implements Serializable, BusinessObject {
    public static final int PROTOCOL_TITLE_MAX_LENGTH=255;

    /**
     * Possible types of ORSP Project that can be used for PDOs in Mercury. There may be other types available through
     * the ORSP Portal web service (i.e., "Consent Group"), but this enum should only contain those to be attached to
     * RPs and PDOs.
     *
     * There is currently no need for Mercury to ever look at "Consent Group" type projects, therefore they have been
     * excluded from this list (instead of being included as a disabled or unavailable type). If this ever changes,
     * this documentation and usage of this enum (including calls to {@link #values()}, particularly in the DAO) will
     * need to be updated. DAO methods may need to be added/renamed.
     */
    public enum Type {
        IRB("IRB Protocol", "IRB Protocol", "IRB Project"),
        ORSP_NOT_ENGAGED("ORSP Not Engaged", "'Not Engaged' Project", "'Not Engaged' Project"),
        ORSP_NOT_HUMAN_SUBJECTS_RESEARCH("ORSP Not Human Subjects Research", "Not Human Subjects Research",
                "NHSR Project");

        /*
         * WARNING: These values can NOT be changed without also re-running the related ETL scrips for all records.
         */
        private String name;

        private String label;

        private String orspServiceId;

        Type(String name, String label, String orspServiceId) {
            this.name = name;
            this.label = label;
            this.orspServiceId = orspServiceId;
        }

        /**
         * @return the label exported by Mercury ETL jobs
         */
        public String getName() {
            return name;
        }

        /**
         * @return the label for use in the Mercury UI
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return the identifier matching that returned from the ORSP Portal web service
         */
        public String getOrspServiceId() {
            return orspServiceId;
        }

        /**
         * Returns the type matching the identifier from the ORSP Portal web service.
         *
         * @param id    the type ID from the ORSP Portal web service
         * @return the matching Type value
         * @throws IllegalArgumentException if no matching Type can be found
         */
        public static Type forOrspServiceId(String id) {
            Type result = null;
            for (Type type : values()) {
                if (type.getOrspServiceId().equals(id)) {
                    result = type;
                    break;
                }
            }
            if (result == null) {
                throw new IllegalArgumentException("No RegulatoryInfo.Type for ID: " + id);
            }
            return result;
        }

        /**
         * @return a list of IDs for all types that match those returned from the ORSP Portal web service
         */
        public static List<String> getOrspServiceTypeIds() {
            return Lists.transform(Arrays.asList(values()),
                    new Function<Type, String>() {
                        @Override
                        public String apply(@Nullable RegulatoryInfo.Type type) {
                            return type.getOrspServiceId();
                        }
                    });
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

    /**
     * Setter exists only for careful use from tests and admin management interfaces. The combination of identifier and
     * type must be unique, as enforced by a database constraint. Callers of this setter must validate that this
     * constraint will hold or else be tolerant or accepting of exceptions being thrown during transaction commit.
     *
     * @param type  the new type
     */
    public void setType(Type type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Setter exists only for careful use from tests and admin management interfaces. The combination of identifier and
     * type must be unique, as enforced by a database constraint. Callers of this setter must validate that this
     * constraint will hold or else be tolerant or accepting of exceptions being thrown during transaction commit.
     *
     * @param identifier    the new identifier
     */
    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Long getRegulatoryInfoId() {
        return regulatoryInfoId;
    }

    public Collection<ResearchProject> getResearchProjects() {
        return researchProjects;
    }

    /**
     * Since RegulatoryInfo can be used not only for associated ResearchProjects, but also their children, it is
     * helpful to be able to get a list lf all ResearchProjects and their children.
     *
     * @return all ResearchProjects this RegulatoryInfo is associated with, including child ResearchProjects.
     */
    public Collection<ResearchProject> getResearchProjectsIncludingChildren() {
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

    public String printFriendlyValue() {
        return String.format("OSRP/IRB Identifier: %s Type: %s Title: %s", identifier, type.getName(), name);
    }
}
