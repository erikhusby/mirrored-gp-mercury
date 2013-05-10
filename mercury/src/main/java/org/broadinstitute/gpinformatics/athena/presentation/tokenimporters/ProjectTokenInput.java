package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Token Input support for Research Projects.
 *
 * @author hrafal
 */
public class ProjectTokenInput extends TokenInput<ResearchProject> {

    @Inject
    private ResearchProjectDao researchProjectDao;
    private String tokenObject;

    public ProjectTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected ResearchProject getById(String key) {
        return researchProjectDao.findByBusinessKey(key);
    }

    public String getJsonString(String query) throws JSONException {
        return getJsonString(query, null);
    }

    /**
     * Get the list of all research projects minus the list of projects to omit.
     *
     * @param query the search text string
     * @param omitProjects list of projects to remove from the returned project list
     * @return list of research projects
     * @throws JSONException
     */
    public String getJsonString(String query, Collection<ResearchProject> omitProjects) throws JSONException {
        Collection<ResearchProject> projects = researchProjectDao.searchProjects(query);
        if (omitProjects != null) {
            projects.removeAll(omitProjects);
        }

        return createItemListString(new ArrayList<ResearchProject>(projects));
    }

    @Override
    protected String getTokenId(ResearchProject project) {
        return project.getBusinessKey();
    }

    @Override
    protected String getTokenName(ResearchProject project) {
        return project.getTitle();
    }

    @Override
    protected String formatMessage(String messageString, ResearchProject project) {
        return MessageFormat.format(messageString, project.getTitle());
    }

    public String getTokenObject() {
        List<ResearchProject> projects = getTokenObjects();

        if ((projects == null) || projects.isEmpty()) {
            return "";
        }

        return projects.get(0).getBusinessKey();
    }
}
