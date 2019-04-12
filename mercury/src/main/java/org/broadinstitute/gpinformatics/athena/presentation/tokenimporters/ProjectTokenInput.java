package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Token Input support for Research Projects.
 *
 */
@Dependent
public class ProjectTokenInput extends TokenInput<ResearchProject> {

    private ResearchProjectDao researchProjectDao;

    public ProjectTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    @Override
    protected ResearchProject getById(String key) {
        return researchProjectDao.findByBusinessKey(key);
    }

    /**
     * Get a list of all research projects.
     *
     * @param query the search text string
     * @return list of research projects
     * @throws JSONException
     */
    public String getJsonString(String query) throws JSONException {
        return getJsonString(query, Collections.<ResearchProject>emptySet());
    }

    /**
     * Get the list of all research projects minus the list of projects to omit.
     *
     * @param query the search text string
     * @param omitProjects list of projects to remove from the returned project list
     * @return list of research projects
     * @throws JSONException
     */
    public String getJsonString(String query, @Nonnull Collection<ResearchProject> omitProjects) throws JSONException {

        Collection<ResearchProject> projects = researchProjectDao.searchProjects(query);
        projects.removeAll(omitProjects);

        return createItemListString(new ArrayList<>(projects));
    }

    @Override
    protected String getTokenId(ResearchProject project) {
        return project.getBusinessKey();
    }

    @Override
    protected String getTokenName(ResearchProject project) {
        return project.getBusinessKey() + " - " + project.getTitle();
    }

    @Override
    protected String formatMessage(String messageString, ResearchProject project) {
        return MessageFormat.format(messageString, project.getBusinessKey(), project.getTitle());
    }

    @Inject
    public void setResearchProjectDao(
            ResearchProjectDao researchProjectDao) {
        this.researchProjectDao = researchProjectDao;
    }
}
