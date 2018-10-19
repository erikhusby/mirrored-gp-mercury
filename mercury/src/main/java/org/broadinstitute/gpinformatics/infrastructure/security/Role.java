package org.broadinstitute.gpinformatics.infrastructure.security;

import javax.annotation.Nonnull;

/**
 * Application roles as provided by HTTP authentication. Roles are defined in Active Directory and exported to
 * our server via Crowd.  These roles are used to determine many application behaviors, including which UI elements
 * are visible to a user, which features are available and what the default view for a user is.
 */
public enum Role {
    Developer("Mercury-Developers"),
    PM("Mercury-ProjectManagers"),
    PDM("Mercury-ProductManagers"),
    LabUser("Mercury-LabUsers"),
    LabManager("Mercury-LabManagers"),
    BillingManager("Mercury-BillingManagers"),
    PipelineManager("Mercury-PipelineAdmins"),
    GPProjectManager("Mercury-GPProjectManagers"),
    Viewer("Mercury-ViewOnly"),
    FinanceViewer("Mercury-FinanceViewer"),
    FingerprintWebService(Constants.FINGERPRINT_WEB_SERVICE),
    All("All");

    public final String name;

    Role(String name) {
        this.name = name;
    }

    /**
     * Return an array of the Role names for the supplied roles.
     *
     * @param roles whose names are to be extracted.
     *
     * @return array of comma delimited role names.
     */
    public static String[] roles(@Nonnull Role... roles) {
        String[] roleNames = new String[roles.length];

        for (int i = 0; i < roles.length; i++) {
            roleNames[i] = roles[i].name;
        }

        return roleNames;
    }

    public String getName() {
        return name;
    }

    /** This is necessary to allow references from annotations. */
    public static class Constants {
        public static final String FINGERPRINT_WEB_SERVICE = "Mercury-FingerprintWebService";
    }
}
