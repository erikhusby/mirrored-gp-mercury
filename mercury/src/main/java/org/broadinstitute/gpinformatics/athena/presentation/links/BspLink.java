package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;

import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is a bean to help the UI deal with Jira links
 */
@Named
@RequestScoped
public class BspLink {
    private static final String SEARCH_SAMPLE = "samplesearch/SampleSummary.action?sampleId=";

    @Inject
    private BSPConfig bspConfig;

    public String sampleSearchUrl(String sampleId) {
        // skip the SM- part of the name
        return bspConfig.getUrl(SEARCH_SAMPLE + sampleId.substring(3));
    }
}
