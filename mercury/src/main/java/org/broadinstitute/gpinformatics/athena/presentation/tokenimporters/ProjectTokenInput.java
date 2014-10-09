package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.json.JSONException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Token Input support for Research Projects.
 *
 */
public class ProjectTokenInput extends TokenInput<ResearchProject> {

    /**
     * The attribute by which to search Research Projects.
     */
    public enum SearchBy {
        TITLE,
        BUSINESS_KEY
    }

    @Inject
    private ResearchProjectDao researchProjectDao;

    public ProjectTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected ResearchProject getById(String key) {
        return researchProjectDao.findByBusinessKey(key);
    }

    /**
     * Get a list of all research projects.
     *
     * @param query the search text string
     * @param searchBy The attribute of the Research Project by which to search
     * @return list of research projects
     * @throws JSONException
     */
    public String getJsonString(String query, SearchBy searchBy) throws JSONException {
        return getJsonString(query, searchBy, Collections.<ResearchProject>emptySet());
    }

    /**
     * Get the list of all research projects minus the list of projects to omit.
     *
     * @param query the search text string
     * @param searchBy The attribute of the Research Project by which to search
     * @param omitProjects list of projects to remove from the returned project list
     * @return list of research projects
     * @throws JSONException
     */
    public String getJsonString(String query, @Nonnull SearchBy searchBy,
                                @Nonnull Collection<ResearchProject> omitProjects) throws JSONException {

        Collection<ResearchProject> projects;
        switch (searchBy) {
        case TITLE:
            projects = researchProjectDao.searchProjectsByTitle(query);
            break;
        case BUSINESS_KEY:
            projects = researchProjectDao.searchProjectsByBusinessKey(query);
            break;
        default:
            throw new InformaticsServiceException("Unknown means of searching for Research Projects: " + searchBy);
        }

        projects.removeAll(omitProjects);

        return createItemListString(new ArrayList<>(projects));
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
        return MessageFormat.format(messageString, project.getBusinessKey() + " - " + project.getTitle());
    }
}
