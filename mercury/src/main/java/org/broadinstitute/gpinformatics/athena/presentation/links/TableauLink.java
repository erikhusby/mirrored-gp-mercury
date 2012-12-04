package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is a bean to help the UI deal with Jira links
 */
@Named
@RequestScoped
public class TableauLink {

    private static final String PASS_REPORT = "/PMBPASSSampleDashboard/PMBridgeSamplesDashboard?Research%20Project=";

    @Inject
    private TableauConfig tableauConfig;

     public String tableauAuth() {
        return tableauConfig.getTableauAuth();
     }

   public String passReportUrl(String projectTitle) {
        return tableauConfig.getUrlBase() + PASS_REPORT + projectTitle;
    }
}