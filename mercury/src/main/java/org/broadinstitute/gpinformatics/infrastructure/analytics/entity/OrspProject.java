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

package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An ORSP Project from the ORSP Portal. This entity is read-only because it uses a cache of the data returned from the
 * ORSP Portal web service (http://bussysprod:8080/orsp/api/projects) that is refreshed periodically by the
 * analytics.tiger.OrspProjectAgent TigerETL script.
 */
@Entity
@Table(schema = "ANALYTICS", name = "ORSP_PROJECT")
public class OrspProject {

    /**
     * These are the few ORSP Project statuses that should be allowed to be associated with RPs/PDOs. There are many
     * others, but they do not have meaning to Mercury and are more prone to change over time so are not worth
     * enumerating.
     */
    public static final List<String> USABLE_STATUSES = Arrays.asList("Approved", "Completed");
    public static final String CONSENT_GROUP_PREFIX = "CG-";

    @Id
    @Column(name = "KEY")
    private String projectKey;

    @Column(name = "LABEL")
    private String rawLabel;

    @Column(name = "TYPE")
    private String rawType;

    private String status;

    private String description;

    private String url;

    @OneToMany(mappedBy = "orspProject", fetch = FetchType.EAGER)
    private Collection<OrspProjectConsent> consents = new HashSet<>();
    @Transient
    private Long regulatoryInfoId;

    /**
     * For JPA.
     */
    @SuppressWarnings("unused")
    public OrspProject() {
    }

    /**
     * For tests.
     */
    public OrspProject(String projectKey, String rawLabel, String rawType, String status, String description, String url) {
        this.projectKey = projectKey;
        this.rawLabel = rawLabel;
        this.rawType = rawType;
        this.status = status;
        this.description = description;
        this.url = url;
    }

    /**
     * @return the project key, e.g. ORSP-123
     */
    public String getProjectKey() {
        return projectKey;
    }

    /**
     * Returns the raw label from the ORSP Portal web service. This includes the project key followed by a
     * short name/description in parenthesis.
     *
     * NOTE: This string should typically not be needed by Mercury. Use {@link #getName()} instead.
     *
     * @return the raw label from the ORSP Portal web service
     *
     * @see #getName()
     */
    String getRawLabel() {
        return rawLabel;
    }

    /**
     * @return the short name/description of this ORSP project (extracted from {@link #rawLabel})
     */
    public String getName() {
        Matcher matcher = Pattern.compile("^"+ projectKey +" \\((.*)\\)$").matcher(rawLabel);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        } else {
            throw new RuntimeException("Label from ORSP Portal did not match the expected pattern");
        }
    }

    /**
     * Returns the raw project type name from the ORSP Portal web service.
     *
     * NOTE: This string should typically not be needed by Mercury. Use {@link #getType()} instead.
     *
     * @return the raw project type name from the ORSP Portal web service
     *
     * @see #getType()
     */
    String getRawType() {
        return rawType;
    }

    /**
     * @return the regulatory info type for this ORSP project
     */
    public RegulatoryInfo.Type getType() {
        return RegulatoryInfo.Type.forOrspServiceId(rawType);
    }

    /**
     * @return the status label from the ORSP Portal service
     */
    public String getStatus() {
        return status;
    }

    /**
     * @return true if the ORSP project can be attached to RPs/PDOs; false otherwise
     */
    public boolean isUsable() {
        return USABLE_STATUSES.contains(status);
    }

    public boolean isConsentGroup() {
        return projectKey.startsWith(CONSENT_GROUP_PREFIX);
    }

    /**
     * @return the project description from the ORSP Portal web service, which may include HTML formatting
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the URL for the project in the ORSP Portal
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the project's consents (use restrictions) from the ORSP Portal
     */
    public Collection<OrspProjectConsent> getConsents() {
        return consents;
    }
}
