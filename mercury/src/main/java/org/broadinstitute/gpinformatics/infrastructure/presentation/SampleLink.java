package org.broadinstitute.gpinformatics.infrastructure.presentation;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.inject.Inject;

/**
 * This class is used to generate sample links for the UI.
 */
public class SampleLink {

    private final BSPConfig bspConfig;
    private final JiraLink jiraLink;
    private final AbstractSample sample;
    private final Format format;

    private SampleLink(BSPConfig bspConfig, JiraLink jiraLink, AbstractSample sample) {
        this.bspConfig = bspConfig;
        this.jiraLink = jiraLink;
        this.sample = sample;
        format = Format.fromSample(sample);
    }

    public boolean getHasLink() {
        return format != Format.UNKNOWN;
    }

    public String getTarget() {
        return format.target;
    }

    public String getLabel() {
        return format.label;
    }

    public String getUrl() {
        switch (format) {
        case CRSP:
//            return jiraLink.browseUrl(sample.getSampleKey());
        case BSP:
            return bspConfig.getUrl(BSPConfig.SEARCH_PATH + sample.getSampleKey());
        default:
            return sample.getSampleKey();
        }
    }

    private enum Format {
        BSP("BSP_SAMPLE", "BSP Sample"),
        CRSP("CRSP_SAMPLE", "CRSP Sample"),
        UNKNOWN(null, null);
        private final String target;
        private final String label;

        Format(String target, String label) {
            this.target = target;
            this.label = label;
        }

        static Format fromSample(AbstractSample sample) {
            if (sample.isInBspFormat()) {
                if (ApplicationInstance.CRSP.isCurrent()) {
                    return Format.CRSP;
                } else {
                    return Format.BSP;
                }
            }
            return Format.UNKNOWN;
        }
    }

    public static class Factory {
        @Inject
        private BSPConfig bspConfig;
        @Inject
        private JiraLink jiraLink;

        public SampleLink create(AbstractSample sample) {
            return new SampleLink(bspConfig, jiraLink, sample);
        }
    }
}
