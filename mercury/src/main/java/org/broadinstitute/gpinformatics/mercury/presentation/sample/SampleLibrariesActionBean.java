package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.List;

@UrlBinding(SampleLibrariesActionBean.ACTIONBEAN_URL_BINDING)
public class SampleLibrariesActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/sample/sampleLibraries.action";
    private static final String SHOW_LIBRARIES = "showLibraries";
    private static final String SAMPLE_LIBRARIES_PAGE = "/sample/sample_libraries.jsp";

    private List<String> selectedSamples;

    public List<String> getSelectedSamples() {
        return selectedSamples;
    }

    public void setSelectedSamples(List<String> selectedSamples) {
        this.selectedSamples = selectedSamples;
    }

    @DefaultHandler
    @HandlesEvent(SHOW_LIBRARIES)
    public Resolution showLibraries() {
        return new ForwardResolution(SAMPLE_LIBRARIES_PAGE);
    }
}
